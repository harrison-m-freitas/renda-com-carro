# Arquitetura do Renda com Carro

## Visão geral

O Renda com Carro é uma aplicação pessoal, single-user, para registrar e analisar uma operação profissional com veículo em aplicativos. A solução prioriza operação simples no celular, consistência financeira, execução em hardware próprio e recuperação verificável.

```text
Dispositivo autorizado
        │ HTTPS pela tailnet
        ▼
Tailscale Serve no Raspberry Pi
        │ proxy para 127.0.0.1:8080
        ▼
Spring Boot + Thymeleaf
        │
        ├── PostgreSQL 17
        ├── armazenamento de anexos no SSD
        └── Restic/rclone → Google Drive
```

## Decisões principais

- **Monólito modular:** mantém implantação e transações simples sem misturar regras dos módulos.
- **Renderização server-side:** reduz JavaScript, dependências e consumo de memória.
- **Melhoria progressiva:** JavaScript adiciona etapas móveis, autosave e recuperação, mas o formulário completo continua enviável sem scripts.
- **PostgreSQL + Flyway:** garante integridade relacional e evolução reproduzível do esquema.
- **Rascunhos sincronizados:** o PostgreSQL é a fonte principal dos rascunhos e permite continuar o preenchimento em outro dispositivo.
- **Usuário único:** não há cadastro público, equipes ou autorização por múltiplos papéis.
- **Enums estáveis e apresentação amigável:** os valores persistidos permanecem em inglês, enquanto a interface usa `LabeledEnum#getLabel()` para exibir português.
- **Inferência com auditoria:** o fechamento mensal é calculado a partir dos registros existentes e armazena tanto a prévia quanto os valores confirmados.
- **Tailscale + Spring Security:** o dispositivo precisa pertencer à rede privada e o usuário ainda precisa autenticar-se na aplicação.
- **Raspberry Pi + SSD:** oferece baixo custo recorrente e persistência fora de cartão microSD.
- **Backup Restic:** snapshots criptografados, deduplicados, retidos e restaurados em teste.

## Módulos

### `security`

Autenticação, bootstrap da conta proprietária, sessão e regras de acesso.

### `vehicle`

Cadastro e histórico de veículos. Apenas um veículo ativo pode ser marcado como principal. O odômetro atual possui valor, data, origem e referência ao evento que produziu a leitura.

### `operation`

Dias operacionais, turnos, plataformas, regiões, bairros atendidos, corridas opcionais e receitas. Fechamentos de turno e de dia encaminham as leituras ao serviço central de odômetro.

### `expense`

Gastos profissionais, pessoais e mistos. O rateio padrão de gastos mistos usa a proporção mensal de quilômetros profissionais. O módulo também infere, confirma e audita o fechamento mensal de quilometragem.

### `fuel`

Abastecimentos, preço por litro, tanque cheio/parcial, consumo entre tanques cheios e custo econômico estimado por turno. Abastecimentos funcionam também como leituras rastreáveis de odômetro.

### `goal`

Metas mensais, dias planejados, distribuição sem domingos e projeção do valor necessário por dia restante.

### `finance`

Empréstimos familiares, financiamentos, obrigações flexíveis/estruturadas, parcelas, pagamentos, amortizações e saldo devedor.

### `draft`

Rascunhos de formulários complexos, sincronização entre dispositivos, bloqueio otimista, expiração, recuperação e limpeza diária. O módulo armazena somente campos editáveis e delega a validação do payload a uma definição específica de cada formulário.

### `attachment`

Validação, armazenamento fora do diretório público, checksum SHA-256 e download autenticado de comprovantes.

### `dashboard`

Agrega dados dos módulos em indicadores operacionais e financeiros sem duplicar desembolsos e custos econômicos. Gastos mistos por quilometragem usam o fechamento mensal confirmado.

### `shared`

Políticas decimais, parser decimal localizado, contrato de rótulos amigáveis, exceções de domínio, binding web e tratamento central de erros.

## Dependências entre módulos

```text
vehicle ───────────────┐
                       ├── operation ──┐
vehicle ── expense ────┤               │
vehicle ── fuel ───────┤               ├── dashboard
vehicle ── finance ────┤               │
goal ──────────────────┘               │
attachment ── associa-se por owner ────┘

draft ── integra-se transacionalmente com expense, goal e finance
security e shared são transversais.
```

O domínio não depende da camada web. Controllers recebem formulários, chamam serviços de aplicação e renderizam templates. Repositórios JPA ficam na infraestrutura de cada módulo. Serviços de submissão coordenam a criação do registro definitivo e a exclusão do rascunho na mesma transação.

## Modelo operacional

1. O usuário abre um dia operacional para o veículo principal.
2. Um ou mais turnos podem ser executados no mesmo dia, mas somente um fica aberto por vez.
3. Uber e 99 podem estar ativos simultaneamente no turno.
4. Receitas podem ser consolidadas ou vinculadas a corridas opcionais.
5. Gastos e abastecimentos podem ser registrados durante ou depois da operação.
6. O dia só pode ser fechado após o fechamento de todos os turnos.
7. Ao final do mês, a aplicação calcula uma prévia da quilometragem e o usuário a confirma ou corrige com justificativa.

## Apresentação de enums

Os nomes de constantes, como `PROFESSIONAL`, `MILEAGE_RATIO` e `BANK_FINANCING`, permanecem estáveis no código e no banco. Enums visíveis implementam `LabeledEnum` e expõem rótulos como “Profissional”, “Proporcional à quilometragem” e “Financiamento bancário”.

Templates usam a propriedade `label` em selects, tabelas, badges e detalhes. Um teste contratual exige rótulo amigável para todos os enums registrados como visíveis, e testes web verificam que constantes técnicas não aparecem nas opções principais.

## Formulários guiados

Gastos, fechamentos de quilometragem, metas mensais e obrigações financeiras usam um componente guiado compartilhado.

No desktop, todas as seções são renderizadas na mesma página. Em telas menores que 768 px, o JavaScript mostra uma seção por vez, exibe `Etapa N de M` e só avança depois que os campos da etapa são válidos e o rascunho foi sincronizado. Quando um fluxo está bloqueado e não oferece navegação de etapas, todas as informações permanecem visíveis.

Os inputs seguem um padrão localizado:

- moeda aceita vírgula e usa prefixo `R$`;
- percentuais são informados de `0` a `100` e convertidos para razão no serviço de submissão;
- competências usam `YearMonth` e o controle `type="month"`;
- odômetros e distâncias exibem o sufixo `km`;
- valores calculados começam somente para leitura;
- erros ficam associados ao campo com `aria-describedby`;
- alvos móveis têm altura mínima de 44 px.

A interface é uma melhoria progressiva. Sem JavaScript, todas as seções continuam visíveis, campos condicionais podem ser enviados e os serviços de submissão descartam valores incompatíveis com a classificação ou modo selecionado.

## Rascunhos sincronizados

A tabela `form_draft` possui:

```text
id
owner_id
form_type
context_key
schema_version
current_step
payload_json
lock_version
created_at
updated_at
expires_at
```

A combinação `(owner_id, form_type, context_key)` é única. As chaves são:

- gasto: `current`;
- fechamento: `vehicle:{uuid}:month:{AAAA-MM}`;
- meta: `month:{AAAA-MM}`;
- obrigação: `draft:{uuid}`.

`FormDraftDefinitionRegistry` associa cada tipo à sua definição. As definições limitam campos aceitos, normalizam valores, validam a etapa e rejeitam campos derivados ou desconhecidos. O payload serializado não pode ultrapassar 65.536 bytes e não aceita anexos binários.

`FormDraftService` oferece criação, atualização, consulta, listagem, descarte, conclusão e limpeza. Cada atualização válida renova `expiresAt` por sete dias. Uma tarefa agendada diariamente remove rascunhos expirados.

A coluna `lock_version` usa bloqueio otimista. Uma versão antiga recebe HTTP `409 Conflict` com a versão atual do servidor. A interface oferece:

- usar a versão mais recente;
- revisar as alterações locais;
- substituir explicitamente a versão do servidor.

Não existe mesclagem automática campo a campo. Uma cópia no `localStorage` é usada somente como proteção emergencial quando a rede falha. Ela não é fonte de verdade e é removida após sincronização bem-sucedida.

O envio definitivo nunca converte o JSON diretamente em entidade. O formulário MVC é validado novamente, cálculos derivados são refeitos e um serviço transacional cria o registro real. O rascunho é removido apenas depois da criação bem-sucedida; em qualquer falha, permanece disponível.

## Odômetro rastreável

`VehicleOdometerService` centraliza decisões sobre novas leituras:

```text
leitura com data anterior à atual -> preservar o evento e não alterar o veículo
leitura igual e mais recente       -> atualizar os metadados da origem
leitura maior e mais recente       -> atualizar valor, data e origem
leitura menor e mais recente       -> rejeitar como regressão
```

As origens incluem ajuste manual do veículo, fechamento de turno, fechamento de dia, abastecimento e fechamento mensal. Dias históricos usam a data operacional como instante da leitura, evitando que um lançamento retroativo seja confundido com uma leitura feita hoje.

## Fechamento mensal inferido

`MonthlyMileageInferenceService` recebe veículo e mês e monta uma prévia imutável.

### Odômetro inicial

A prioridade é:

1. fechamento do mês anterior;
2. primeiro dia operacional válido;
3. primeiro turno válido;
4. primeiro abastecimento;
5. odômetro inicial do veículo.

### Odômetro final

É a leitura cronologicamente mais recente no período entre:

- dia operacional fechado;
- turno fechado;
- abastecimento;
- odômetro atual do veículo, quando a leitura pertence ao mês e possui origem rastreável.

### Quilometragem profissional

Somente distâncias de turnos fechados entram na soma profissional. A diferença entre o total do mês e os turnos é classificada como uso pessoal ou não classificado.

### Alertas

Bloqueios impedem confirmação quando há:

- fechamento duplicado;
- dia ou turno aberto;
- ausência de leitura suficiente;
- regressão cronológica;
- quilometragem profissional maior que o total.

Avisos informam lacunas entre distância do dia e turnos ou ausência de distância profissional. Avisos exigem confirmação explícita.

### Confirmação e auditoria

Os campos inferidos iniciam somente para leitura. O botão **Corrigir valores** libera início, fim e quilômetros profissionais. O servidor sempre recalcula a prévia no POST e compara os valores confirmados com os inferidos; qualquer diferença exige justificativa.

O registro armazena:

- valores inferidos;
- valores confirmados;
- origem das leituras inicial e final;
- indicador de ajuste manual;
- justificativa;
- data do cálculo;
- data da confirmação.

Valores derivados — total, pessoal e percentual profissional — são recalculados no servidor. Após a confirmação, a leitura final é enviada ao serviço central de odômetro.

## Conceitos financeiros

### Receita por competência

Valor produzido na data da operação, independentemente da data em que a plataforma transferiu o dinheiro.

### Fluxo de caixa

Entradas e saídas nas datas efetivas de recebimento ou pagamento.

### Margem operacional

```text
receitas operacionais − custos variáveis da operação
```

### Resultado econômico

```text
margem operacional − custos fixos − provisões − depreciação − custo financeiro
```

### Caixa líquido pessoal

```text
caixa da operação − devolução familiar − parcelas − amortizações − compromissos de implantação
```

Pagamentos da aquisição reduzem caixa pessoal e saldo devedor, mas não são classificados como custo operacional do trabalho.

## Consistência e concorrência

- índices únicos impedem mais de um veículo principal ativo;
- índices parciais impedem mais de um turno aberto no dia;
- a combinação proprietário, tipo e contexto impede rascunhos duplicados;
- bloqueio otimista impede sobrescrita silenciosa de rascunho;
- leituras cronologicamente novas não podem reduzir o odômetro;
- regressões internas do mês bloqueiam o fechamento;
- o fechamento do dia é bloqueado quando há turno aberto;
- o fechamento mensal é bloqueado quando há dia ou turno aberto;
- correções manuais exigem justificativa;
- pagamentos não podem exceder o saldo;
- referências externas evitam duplicidade de receitas e pagamentos;
- criação definitiva e exclusão de rascunho usam a mesma transação;
- operações financeiras usam transações de banco;
- exclusões destrutivas são evitadas em favor de arquivamento ou cancelamento.

## Segurança

- credenciais são fornecidas por variáveis de ambiente e nunca ficam no repositório;
- senhas são armazenadas com BCrypt;
- CSRF permanece habilitado, inclusive na API JSON de rascunhos;
- consultas de rascunho são sempre limitadas ao usuário autenticado;
- payloads possuem lista de campos permitidos e limite de tamanho;
- textos restaurados são atribuídos por `.value` ou `.checked`, sem `innerHTML`;
- sessões têm limite e expiração;
- cookies de produção são `HttpOnly`, `Secure` e `SameSite=Strict`;
- PostgreSQL não publica porta no host;
- a aplicação publica somente em loopback;
- Tailscale Serve fornece HTTPS e acesso pela tailnet;
- anexos não são servidos diretamente pelo servidor web;
- erros inesperados mostram apenas um correlation ID ao usuário.

## Implantação

O Compose possui três serviços:

- `app`: aplicação Java não-root, limite de 1 GiB;
- `postgres`: banco com limite de 768 MiB e volume no SSD;
- `backup`: ferramentas PostgreSQL, Restic e rclone, limite de 512 MiB.

A imagem da aplicação é multi-stage e validada para `linux/arm64` pela CI. A rede interna conecta aplicação e banco; o container de backup também recebe uma rede com saída para o backend remoto.

## Backup e recuperação

Cada snapshot contém:

- dump PostgreSQL no formato custom;
- anexos;
- configuração necessária à recuperação;
- metadados do snapshot.

Retenção:

- 7 diários;
- 4 semanais;
- 12 mensais.

A restauração exige confirmação textual, banco de destino seguro e diretórios vazios. A CI executa backup e restauração isolada com repositório Restic local; a instalação real deve repetir o teste com Google Drive.

## Observabilidade

- endpoint `/actuator/health` público apenas para healthcheck;
- estados de autosave mostram salvando, salvo e falha;
- conflitos de rascunho informam as versões local e remota;
- limpeza de rascunhos expirados é executada diariamente;
- logs de produção rotacionados no SSD;
- correlation ID para erros inesperados;
- healthchecks de aplicação e banco no Compose;
- logs do backup disponíveis pelo systemd e Docker.

## Evolução futura

- alertas por Telegram;
- manutenção preventiva avançada;
- coletor Android com fila offline;
- importações e reconciliação de eventos;
- comparação preditiva de horários, regiões e plataformas.
