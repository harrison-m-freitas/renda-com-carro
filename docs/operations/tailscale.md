# Acesso privado com Tailscale

A aplicação publica a porta Docker apenas em `127.0.0.1:8080`. Não abra redirecionamento de portas no roteador.

## 1. Instalação

No Raspberry Pi OS 64-bit:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up --ssh
```

Confira o dispositivo e o IP da tailnet:

```bash
tailscale status
tailscale ip -4
```

Autorize o Raspberry no painel do Tailscale e mantenha pelo menos dois dispositivos pessoais aprovados para recuperação.

## 2. Publicar a aplicação somente na tailnet

Com a aplicação saudável localmente:

```bash
curl --fail http://127.0.0.1:8080/actuator/health
sudo tailscale serve --bg http://127.0.0.1:8080
sudo tailscale serve status
```

Use a URL HTTPS exibida pelo comando `tailscale serve status`. O perfil de produção marca o cookie de sessão como `Secure`; portanto, acesse a interface pela URL HTTPS do Tailscale.

## 3. Política de acesso

No painel de administração do Tailscale:

- identifique os dispositivos confiáveis por proprietário;
- remova dispositivos antigos imediatamente;
- use ACLs ou grants para limitar o acesso ao Raspberry;
- mantenha SSH permitido somente aos seus dispositivos administrativos;
- ative autenticação multifator na conta que administra a tailnet;
- evite chaves de autenticação reutilizáveis ou sem expiração.

Exemplo conceitual de grant — adapte os usuários, tags e sintaxe no painel antes de salvar:

```json
{
  "grants": [
    {
      "src": ["autogroup:member"],
      "dst": ["tag:renda-com-carro"],
      "ip": ["tcp:443"]
    }
  ]
}
```

Uma política mais restritiva deve listar apenas seus usuários ou dispositivos confiáveis, em vez de toda a `autogroup:member`.

## 4. Regras de rede

- Não publique `8080` em `0.0.0.0`.
- Não publique a porta `5432` do PostgreSQL.
- Não crie port forwarding no modem/roteador.
- Não habilite Funnel para esta aplicação.
- Mantenha o firewall do host negando conexões de entrada não necessárias.

Exemplo com UFW, quando ele já fizer parte da administração do host:

```bash
sudo apt install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow in on tailscale0
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status verbose
```

Antes de ativar o firewall remotamente, confirme uma segunda sessão SSH e um método de recuperação local.

## 5. Recuperação

Caso a interface deixe de responder:

```bash
ssh usuario@IP_TAILSCALE_DO_PI
cd /opt/renda-com-carro
docker compose ps
docker compose logs --tail=200 app
curl -v http://127.0.0.1:8080/actuator/health
sudo tailscale serve status
```

Para remover e recriar a publicação:

```bash
sudo tailscale serve reset
sudo tailscale serve --bg http://127.0.0.1:8080
```
