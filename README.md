# Renda com Carro

Aplicação pessoal para controlar uma operação de renda com veículo por aplicativos. O MVP registra dias, turnos, Uber/99, receitas, gastos, abastecimentos, metas, empréstimos, anexos e indicadores operacionais/financeiros.

## Estado do projeto

O código do MVP é validado por testes de unidade, integração e aceite com PostgreSQL/Testcontainers. A pipeline também executa testes JavaScript, constrói a imagem ARM64, inicia a stack de produção e executa backup/restauração isolada.

A instalação no Raspberry só deve ser considerada pronta após concluir os itens manuais de SSD, Tailscale, Google Drive, reboot e soak test registrados no [checklist de aceite](docs/mvp-acceptance-checklist.md).

## Stack

- Java 21 e Spring Boot 3.5;
- Spring MVC, Thymeleaf e Bootstrap;
- JavaScript modular sem framework para melhoria progressiva;
- PostgreSQL 17 e Flyway;
- Spring Security para usuário único;
- Docker Compose;
- implantação privada em Raspberry Pi por Tailscale;
- Restic + rclone para backup criptografado.

## Formulários guiados e rascunhos

Gastos, fechamentos de quilometragem, metas mensais e obrigações financeiras usam um padrão guiado:

- no desktop, todas as seções permanecem visíveis na mesma página;
- no celular, uma etapa é exibida por vez, com **Voltar** e **Continuar**;
- avançar aguarda a sincronização do rascunho;
- o rascunho é salvo no PostgreSQL e pode ser retomado em outro dispositivo;
- rascunhos expiram após sete dias sem edição;
- ao reabrir, a aplicação pergunta se deve continuar ou começar novamente;
- alterações concorrentes mostram um conflito e nunca sobrescrevem silenciosamente a versão mais recente;
- quando a conexão falha, uma cópia local emergencial preserva o preenchimento até a reconexão.

A organização dos rascunhos é específica por formulário: um gasto em andamento, um fechamento por veículo e mês, uma meta por mês e várias obrigações. O envio definitivo sempre revalida os dados, recalcula valores derivados e remove o rascunho na mesma transação da criação do registro real.

Sem JavaScript, os campos continuam visíveis e os formulários ainda podem ser enviados normalmente. Nesse modo, autosave, recuperação dinâmica e navegação móvel por etapas ficam indisponíveis.

## Usabilidade e fechamento mensal

Os valores técnicos dos enums continuam persistidos em inglês, preservando estabilidade no Java e no PostgreSQL. A interface apresenta rótulos em português por meio do contrato `LabeledEnum`, inclusive em selects, tabelas, detalhes e badges.

Valores monetários aceitam formato brasileiro e exibem o prefixo `R$`. Percentuais são informados de `0` a `100`, competências usam mês e ano e leituras de distância exibem `km`.

O fechamento mensal de quilometragem não exige mais a digitação normal de início, fim e quilômetros profissionais. O fluxo é:

1. selecionar veículo e mês;
2. revisar a prévia inferida a partir de dias, turnos e abastecimentos;
3. verificar a origem das leituras e os alertas;
4. confirmar os valores automáticos;
5. usar **Corrigir valores** somente quando necessário, com justificativa obrigatória.

O fechamento armazena os valores inferidos e confirmados, as origens das leituras, as datas de cálculo/confirmação e o motivo da correção. Turnos ou dias abertos, duplicidade e regressões de odômetro bloqueiam a confirmação.

O odômetro atual do veículo é atualizado por um serviço central e rastreável. Leituras históricas permanecem registradas, mas não reduzem a leitura atual.

## Desenvolvimento

Pré-requisitos: Java 21, Node.js 22 e Docker.

Para executar apenas o PostgreSQL local, use um override ou um comando direto, porque o Compose principal representa a topologia de produção. Exemplo direto:

```bash
docker run --rm --name renda-postgres-dev \
  -e POSTGRES_DB=renda_com_carro \
  -e POSTGRES_USER=renda \
  -e POSTGRES_PASSWORD=senha-local \
  -p 5438:5432 \
  postgres:17-alpine
```

Em outro terminal:

```bash
export DB_URL=jdbc:postgresql://localhost:5438/renda_com_carro
export DB_USER=renda
export DB_PASSWORD='senha-local'
export APP_ADMIN_USERNAME=harrison
export APP_ADMIN_PASSWORD='senha-local-com-16-ou-mais-caracteres'
./mvnw spring-boot:run
```

Para testes e empacotamento:

```bash
npm run test:js
./mvnw clean test
./mvnw package
```

Os testes de integração usam Testcontainers e precisam de um daemon Docker acessível. Os testes JavaScript usam somente o test runner nativo do Node.js, sem dependências de produção adicionais.

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

## Documentação

- [Arquitetura](docs/architecture.md)
- [Checklist de aceite](docs/mvp-acceptance-checklist.md)
- [Especificação dos formulários guiados](docs/superpowers/specs/2026-07-13-guided-forms-and-synced-drafts-design.md)
- [Plano de implementação dos formulários guiados](docs/superpowers/plans/2026-07-13-guided-forms-and-synced-drafts.md)
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
├── draft
├── attachment
├── dashboard
├── security
└── shared
```

Cada módulo separa domínio, aplicação, infraestrutura e web conforme a necessidade.
