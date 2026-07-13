# Backup e restauração

O backup combina:

- `pg_dump -Fc` do PostgreSQL;
- anexos armazenados no SSD;
- arquivos de configuração necessários à recuperação;
- criptografia e retenção pelo Restic;
- transporte pelo backend `rclone`, normalmente Google Drive.

Política padrão: **7 diários, 4 semanais e 12 mensais**. Aos domingos, o script também executa `restic check --read-data-subset=5%`.

## 1. Configurar o rclone

Execute a configuração interativa dentro do container de backup, persistindo o arquivo no diretório definido por `RCLONE_CONFIG_DIR`:

```bash
mkdir -p "${RCLONE_CONFIG_DIR:-./var/rclone}"
docker compose run --rm --entrypoint rclone backup config
```

Crie um remote chamado `gdrive`. Quando a autorização precisar de um navegador em outro computador, siga o fluxo exibido pelo próprio rclone.

Teste:

```bash
docker compose run --rm --entrypoint rclone backup lsd gdrive:
```

## 2. Configurar o Restic

```bash
cp infra/backup/restic.env.example infra/backup/restic.env
chmod 600 infra/backup/restic.env
nano infra/backup/restic.env
```

Exemplo:

```text
RESTIC_REPOSITORY=rclone:gdrive:renda-com-carro
RESTIC_PASSWORD=uma-senha-longa-guardada-fora-do-raspberry
RESTIC_AUTO_INIT=true
RESTIC_HOST=renda-com-carro-pi
```

A senha do Restic deve ter uma cópia fora do Raspberry e fora do próprio Google Drive. Sem ela, os snapshots não podem ser restaurados.

Na primeira execução, mantenha `RESTIC_AUTO_INIT=true`. Após o primeiro backup bem-sucedido, altere para `false` para impedir a criação acidental de outro repositório quando houver erro de configuração.

## 3. Executar backup manual

```bash
docker compose up -d postgres app backup
docker compose exec -T backup /opt/backup/backup.sh
```

Ver snapshots:

```bash
docker compose exec -T backup restic snapshots
```

Forçar a verificação completa da rotina semanal:

```bash
docker compose exec -T backup /opt/backup/backup.sh --check
```

## 4. Instalar o timer diário

```bash
sudo cp infra/systemd/renda-com-carro-backup.service /etc/systemd/system/
sudo cp infra/systemd/renda-com-carro-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now renda-com-carro-backup.timer
systemctl list-timers renda-com-carro-backup.timer
```

Executar e inspecionar manualmente:

```bash
sudo systemctl start renda-com-carro-backup.service
sudo journalctl -u renda-com-carro-backup.service -n 200 --no-pager
```

## 5. Teste mensal de restauração isolada

Nunca comece restaurando sobre o banco e os anexos em produção. Primeiro restaure para um banco e diretório isolados:

```bash
rm -rf var/restore-test
mkdir -p var/restore-test/storage var/restore-test/config-review

docker compose exec -T \
  -e RESTORE_CONFIRMATION=RESTORE \
  -e RESTORE_DATABASE=renda_com_carro_restore_test \
  -e RESTORE_STORAGE_DIR=/restore/storage \
  -e RESTORE_CONFIG_DIR=/restore/config-review \
  backup /opt/backup/restore.sh latest
```

Valide o banco:

```bash
docker compose exec -T postgres \
  psql -U "${DB_USER:-renda}" -d renda_com_carro_restore_test -c '\dt'
```

Valide os anexos:

```bash
find var/restore-test/storage -maxdepth 5 -type f -print
```

Depois do teste:

```bash
docker compose exec -T postgres \
  dropdb -U "${DB_USER:-renda}" --if-exists --force renda_com_carro_restore_test
rm -rf var/restore-test
```

## 6. Restauração de produção

1. Confirme que há um snapshot válido e uma cópia da senha Restic.
2. Coloque a aplicação em indisponibilidade:

```bash
docker compose stop app
```

3. Preserve os dados atuais antes de esvaziar o destino:

```bash
mv "${DATA_ROOT:-./var}/storage" "${DATA_ROOT:-./var}/storage.before-restore.$(date +%Y%m%d-%H%M%S)"
mkdir -p "${DATA_ROOT:-./var}/storage"
sudo chown -R 10001:10001 "${DATA_ROOT:-./var}/storage"
```

4. Execute a restauração apontando para o banco real e para `/source/storage`:

```bash
docker compose exec -T \
  -e RESTORE_CONFIRMATION=RESTORE \
  -e RESTORE_DATABASE="${DB_NAME:-renda_com_carro}" \
  -e RESTORE_STORAGE_DIR=/source/storage \
  -e RESTORE_CONFIG_DIR=/restore/config-review \
  backup /opt/backup/restore.sh latest
```

5. Corrija permissões, reinicie e valide:

```bash
sudo chown -R 10001:10001 "${DATA_ROOT:-./var}/storage"
docker compose up -d app
curl --fail http://127.0.0.1:8080/actuator/health
```

6. Compare os arquivos em `config-review` com a configuração corrente. O script não substitui automaticamente `.env` ou configuração do host.

## 7. Falhas e alertas

Uma execução com código diferente de zero aparece como falha no systemd:

```bash
systemctl --failed
journalctl -u renda-com-carro-backup.service --since today
```

Até a integração com Telegram ser implementada, configure no sistema operacional um mecanismo de observação do timer ou revise diariamente o estado do serviço.
