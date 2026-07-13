# Implantação no Raspberry Pi 4

Este guia considera Raspberry Pi 4 com 4 GB, Raspberry Pi OS Lite 64-bit, SSD externo USB–SATA e acesso remoto via Tailscale.

## 1. Instalar o sistema

1. Grave a versão 64-bit do Raspberry Pi OS Lite no SSD ou cartão de inicialização.
2. Defina usuário, hostname, chave SSH e fuso `America/Sao_Paulo` no Raspberry Pi Imager.
3. Inicie o equipamento, atualize o sistema e reinicie:

```bash
sudo apt update
sudo apt full-upgrade -y
sudo reboot
```

Confirme a arquitetura:

```bash
uname -m
# Esperado: aarch64
```

## 2. Identificar e testar o SSD

> **ATENÇÃO:** `badblocks -w` destrói todos os dados do dispositivo informado. Confirme três vezes o caminho em `lsblk`. Nunca execute no disco que contém dados a preservar ou enquanto estiver montado.

Instale as ferramentas:

```bash
sudo apt install -y smartmontools fio usbutils
lsblk -o NAME,SIZE,MODEL,SERIAL,FSTYPE,MOUNTPOINTS
```

Substitua `/dev/sdX` pelo SSD correto.

```bash
sudo umount /dev/sdX?* 2>/dev/null || true
sudo smartctl -a /dev/sdX
sudo smartctl -t long /dev/sdX
```

Após o tempo indicado pelo SMART:

```bash
sudo smartctl -a /dev/sdX
```

Teste destrutivo de superfície:

```bash
sudo badblocks -wsv -b 4096 /dev/sdX
```

Crie a partição e o sistema de arquivos somente depois dos testes:

```bash
sudo parted /dev/sdX --script mklabel gpt
sudo parted /dev/sdX --script mkpart primary ext4 0% 100%
sudo mkfs.ext4 -L renda-data /dev/sdX1
```

Monte por UUID:

```bash
sudo mkdir -p /srv/renda-com-carro
sudo blkid /dev/sdX1
sudoedit /etc/fstab
```

Linha de exemplo para `/etc/fstab`:

```text
UUID=SUBSTITUA_PELO_UUID /srv/renda-com-carro ext4 defaults,noatime,nofail,x-systemd.device-timeout=30 0 2
```

Valide:

```bash
sudo mount -a
findmnt /srv/renda-com-carro
df -h /srv/renda-com-carro
```

## 3. Validar USB, UASP e energia

```bash
lsusb -t
sudo dmesg --follow
```

Na saída de `lsusb -t`, prefira o driver `uas`. Durante um teste prolongado, não devem surgir mensagens recorrentes como `USB disconnect`, `reset SuperSpeed USB device`, `I/O error` ou falhas de filesystem.

Teste de escrita/leitura no filesystem já criado:

```bash
sudo mkdir -p /srv/renda-com-carro/fio-test
sudo fio \
  --name=ssd-soak \
  --directory=/srv/renda-com-carro/fio-test \
  --size=8G \
  --rw=randrw \
  --rwmixread=70 \
  --bs=4k \
  --iodepth=16 \
  --runtime=6h \
  --time_based \
  --verify=crc32c \
  --verify_fatal=1 \
  --direct=1
sudo rm -rf /srv/renda-com-carro/fio-test
```

Verifique subtensão:

```bash
vcgencmd get_throttled
```

`0x0` indica que não há sinal atual ou histórico de throttling desde a inicialização. Caso apareça outro valor, investigue fonte, cabo e adaptador USB–SATA antes de usar em produção.

## 4. Instalar Docker Engine e Compose

Remova pacotes conflitantes, configure o repositório oficial e instale os pacotes:

```bash
for pkg in docker.io docker-doc docker-compose podman-docker containerd runc; do
  sudo apt remove -y "$pkg" 2>/dev/null || true
done

sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

. /etc/os-release
ARCH="$(dpkg --print-architecture)"
echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian ${VERSION_CODENAME} stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
newgrp docker

docker version
docker compose version
```

## 5. Preparar diretórios e código

```bash
sudo mkdir -p /opt/renda-com-carro
sudo chown "$USER":"$USER" /opt/renda-com-carro
cd /opt/renda-com-carro
git clone SEU_REPOSITORIO_PRIVADO .
git checkout main

cp .env.example .env
cp infra/backup/restic.env.example infra/backup/restic.env
chmod 600 .env infra/backup/restic.env
```

Edite as senhas e caminhos:

```bash
nano .env
nano infra/backup/restic.env
```

Use o SSD como raiz de dados:

```text
DATA_ROOT=/srv/renda-com-carro
RESTORE_ROOT=/srv/renda-com-carro/restore-test
RCLONE_CONFIG_DIR=/srv/renda-com-carro/rclone
RESTIC_LOCAL_ROOT=/srv/renda-com-carro/restic-local
```

Prepare os diretórios. O container da aplicação usa UID/GID `10001`:

```bash
sudo mkdir -p \
  /srv/renda-com-carro/{postgres,storage,logs,backup-work,restore-test,rclone,restic-local}
sudo chown -R 10001:10001 /srv/renda-com-carro/storage /srv/renda-com-carro/logs
sudo chown -R "$USER":"$USER" \
  /srv/renda-com-carro/backup-work \
  /srv/renda-com-carro/restore-test \
  /srv/renda-com-carro/rclone \
  /srv/renda-com-carro/restic-local
```

## 6. Construir e iniciar

```bash
cd /opt/renda-com-carro
docker compose config
docker compose build
docker compose up -d
```

Acompanhe a inicialização:

```bash
docker compose ps
docker compose logs -f --tail=100 app
curl --fail http://127.0.0.1:8080/actuator/health
```

O PostgreSQL não possui porta publicada. A aplicação escuta somente em `127.0.0.1`, devendo ser exposta aos dispositivos aprovados pelo Tailscale conforme o guia específico.

## 7. Teste de estabilidade de 24–48 horas

Mantenha a stack em execução e acompanhe:

```bash
watch -n 5 'docker stats --no-stream; echo; vcgencmd get_throttled'
sudo journalctl -kf
docker compose logs -f postgres app
```

Durante o período:

- cadastre dados de teste;
- faça uploads e downloads;
- execute backups;
- reinicie a stack algumas vezes;
- verifique SMART, erros USB e uso de memória;
- confirme que os dados continuam presentes.

## 8. Teste de reinicialização

```bash
sudo reboot
```

Depois do retorno:

```bash
cd /opt/renda-com-carro
docker compose ps
curl --fail http://127.0.0.1:8080/actuator/health
systemctl status tailscaled --no-pager
```

Somente considere a instalação pronta após um backup e uma restauração completa terem sido testados.
