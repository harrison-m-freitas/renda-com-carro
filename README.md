# Renda com Carro

Aplicação pessoal para controlar uma operação de renda com veículo por aplicativos. O MVP registra dias, turnos, Uber/99, receitas, gastos, abastecimentos, metas, empréstimos, anexos e indicadores operacionais/financeiros.

## Stack

- Java 21 e Spring Boot 3.5;
- Spring MVC, Thymeleaf e Bootstrap;
- PostgreSQL 17 e Flyway;
- Spring Security para usuário único;
- Docker Compose;
- implantação privada em Raspberry Pi por Tailscale;
- Restic + rclone para backup criptografado.

## Desenvolvimento

Pré-requisitos: Java 21 e Docker.

```bash
cp .env.example .env
docker compose up -d postgres
export DB_URL=jdbc:postgresql://localhost:5432/renda_com_carro
export DB_USER=renda
export DB_PASSWORD='senha-local'
export APP_ADMIN_USERNAME=harrison
export APP_ADMIN_PASSWORD='senha-local-com-16-ou-mais-caracteres'
./mvnw spring-boot:run
```

Para testes:

```bash
./mvnw clean test
```

Os testes de integração usam Testcontainers e precisam de um daemon Docker acessível.

## Produção com Docker Compose

```bash
cp .env.example .env
cp infra/backup/restic.env.example infra/backup/restic.env
chmod 600 .env infra/backup/restic.env
# Edite senhas, caminhos e o repositório Restic.
docker compose config
docker compose up -d --build
curl --fail http://127.0.0.1:8080/actuator/health
```

A aplicação é publicada somente em `127.0.0.1`. Use `tailscale serve` para disponibilizá-la por HTTPS aos dispositivos aprovados. O PostgreSQL não publica porta no host.

## Operação

- [Implantação no Raspberry Pi](docs/operations/raspberry-pi.md)
- [Acesso privado com Tailscale](docs/operations/tailscale.md)
- [Backup e restauração](docs/operations/backup-restore.md)

## Segurança

- não versione `.env`, `infra/backup/restic.env`, senhas ou configuração real do rclone;
- mantenha a aplicação restrita à tailnet;
- preserve pelo menos dois dispositivos administrativos confiáveis;
- mantenha cópia externa da senha Restic;
- não exponha PostgreSQL nem redirecione portas no roteador.

## Backup rápido

```bash
docker compose exec -T backup /opt/backup/backup.sh
```

A restauração exige a confirmação textual `RESTORE`, recria um banco de destino e exige diretórios de arquivos vazios. Faça mensalmente uma restauração isolada antes de confiar nos snapshots.

## Estrutura modular

```text
src/main/java/dev/harrison/rendacomcarro/
├── operation
├── vehicle
├── fuel
├── expense
├── finance
├── goal
├── attachment
├── dashboard
├── security
└── shared
```

Cada módulo separa domínio, aplicação, infraestrutura e web conforme a necessidade.
