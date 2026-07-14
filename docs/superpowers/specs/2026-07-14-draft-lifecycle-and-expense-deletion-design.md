# Ciclo de vida de rascunhos e exclusão lógica de gastos

Data: 2026-07-14

## Contexto

O formulário de gastos usa o framework compartilhado de rascunhos guiados. O comportamento atual causa três problemas principais:

1. o usuário descarta um rascunho, mas o evento `pagehide` pode salvá-lo novamente;
2. a cópia de emergência local pode ser interpretada como conflito mesmo quando pertence ao mesmo navegador e já foi sincronizada;
3. o cadastro de gastos oferece apenas “Cancelar”, embora o caso de uso real seja excluir um lançamento digitado por engano e, eventualmente, restaurá-lo.

A solução deve corrigir o ciclo de vida do rascunho no framework compartilhado e introduzir exclusão lógica restaurável para gastos, sem exclusão física pelo fluxo normal.

## Objetivos

- Abrir o formulário de novo gasto sempre com valores padrão, sem restaurar automaticamente um rascunho anterior.
- Preservar rascunhos anteriores até que o usuário os continue, descarte ou conclua com sucesso um novo cadastro.
- Criar um rascunho para a sessão atual somente após uma alteração real do usuário.
- Impedir que um rascunho descartado seja recriado ao sair da página.
- Reduzir falsos conflitos e abrir diálogo apenas quando duas versões do mesmo rascunho possuem alterações materiais diferentes.
- Substituir o conceito de “cancelamento” de gasto por exclusão lógica restaurável.
- Migrar gastos atualmente `CANCELLED` para `DELETED`.

## Fora de escopo

- Exclusão física de gastos pela interface.
- Histórico completo de auditoria por usuário nesta primeira versão.
- Edição de gastos já salvos.
- Reembolso ou estorno financeiro como entidade própria.
- Redesenho integral dos demais formulários guiados.

## Modelo de rascunho

### Chaves independentes

O formulário não usará mais uma única chave estática `current` para todas as tentativas de novo gasto.

Cada abertura de formulário novo receberá uma chave de sessão exclusiva, gerada pelo servidor e renderizada no formulário:

```text
expense:new:<uuid>
```

Um rascunho anterior continuará com sua própria chave. Dessa forma, o usuário pode começar um novo preenchimento sem sobrescrever o rascunho antigo.

O formulário enviará duas chaves ocultas na submissão:

- `draftContextKey`: chave da sessão atual;
- `previousDraftContextKey`: chave do rascunho anterior mostrado no aviso, quando existir.

### Rascunho anterior

Ao abrir `/expenses/new`, o cliente consulta o endpoint de listagem de rascunhos `EXPENSE` e escolhe o ativo mais recentemente atualizado, excluindo a chave da sessão atual.

O formulário permanece vazio e mostra um aviso não bloqueante:

> **Existe um rascunho anterior**  
> Salvo em 14/07/2026 às 19:43.  
> **Continuar rascunho** · **Descartar rascunho**

Não haverá modal automático de recuperação.

### Rascunho da sessão atual

Apenas abrir o formulário não cria nem atualiza rascunho.

O controlador mantém um indicador `dirty`. A sessão passa a ser considerada alterada somente após uma mudança real em um campo editável feita pelo usuário.

Valores padrão definidos na inicialização, como veículo principal, data atual, classificação `PROFESSIONAL` e pagamento `PAID`, não contam como alteração do usuário.

O primeiro autosave cria o rascunho da sessão atual usando sua chave exclusiva.

## Fluxos de usuário

### Abrir formulário sem rascunho anterior

1. O formulário abre com os valores padrão.
2. Nenhum rascunho é criado.
3. Após a primeira alteração real, inicia-se o autosave da sessão atual.

### Abrir formulário com rascunho anterior

1. O formulário abre vazio, com os valores padrão.
2. O rascunho anterior permanece preservado.
3. Um aviso discreto oferece `Continuar rascunho` e `Descartar rascunho`.
4. O usuário pode começar um novo preenchimento; esse preenchimento recebe outra chave.

### Continuar rascunho anterior

Se o formulário atual não possui alterações:

1. o rascunho anterior é restaurado diretamente;
2. o controlador passa a usar a chave e versão daquele rascunho;
3. o aviso é removido.

Se o formulário atual possui alterações:

1. uma única confirmação informa que o preenchimento atual será descartado;
2. timers e salvamentos pendentes da sessão atual são cancelados;
3. o rascunho temporário da sessão atual é removido, se existir;
4. o rascunho anterior é restaurado;
5. o controlador passa a usar a chave e versão reais do rascunho restaurado.

### Descartar rascunho anterior

1. Uma única confirmação é apresentada.
2. O controlador cancela timers relacionados àquele rascunho.
3. O rascunho é excluído do servidor.
4. A cópia de emergência local da mesma chave é removida.
5. O aviso desaparece.
6. O formulário atual permanece aberto e não é alterado.

### Descartar rascunho da sessão atual

A ação visível `Descartar rascunho` deve:

1. cancelar o timer de autosave;
2. marcar o controlador como `discarding`;
3. impedir novos salvamentos;
4. aguardar a operação de salvamento em andamento e então excluir a chave de forma idempotente;
5. remover a cópia de emergência local;
6. marcar o controlador como limpo e `disposed`;
7. navegar para `/expenses/new`, gerando uma nova chave de sessão;
8. impedir que `pagehide` recrie o rascunho descartado.

Haverá uma única confirmação. O fluxo não combinará modal próprio com `window.confirm()` adicional.

### Criar gasto com sucesso

A criação bem-sucedida deve remover, na mesma transação do cadastro:

- o rascunho da sessão atual;
- o rascunho anterior apresentado na abertura, se ainda existir.

As duas remoções são idempotentes. Se alguma chave já não existir, a criação do gasto continua normalmente.

Antes da submissão final, o cliente marca o controlador como `submitting`, cancela o timer de autosave e remove as cópias locais de emergência das duas chaves somente depois que a navegação de sucesso for confirmada. Em caso de retorno do formulário com erro, as cópias permanecem.

Se a submissão falhar por validação ou erro de domínio, nenhum rascunho é removido.

## Autosave e saída da página

### Estado interno

O controlador compartilhado deve distinguir:

- `dirty`: existem alterações locais ainda não persistidas;
- `saving`: há uma operação em andamento;
- `discarding`: um descarte foi iniciado;
- `submitting`: o formulário está sendo enviado definitivamente;
- `disposed`: o controlador não deve mais salvar;
- `lastPersistedPayload`: último payload confirmado pelo servidor.

### Regras

- `pagehide` salva apenas se `dirty` for verdadeiro e o controlador não estiver em `discarding`, `submitting` ou `disposed`.
- Um salvamento bem-sucedido atualiza `lastPersistedPayload` e marca `dirty = false`.
- Um novo evento de entrada compara o payload atual com o último persistido, evitando salvar alterações que apenas voltaram ao estado anterior.
- Descarte limpa timer, estado local e cópia de emergência antes da navegação.
- Inicialização de valores padrão não agenda autosave.
- Eventos programáticos de restauração ou sincronização de data não contam como alterações do usuário.

## Cópia local de emergência

A cópia de emergência continua existindo para falhas de rede e incluirá:

- chave do rascunho;
- versão conhecida;
- payload normalizado;
- `savedAt` real da cópia;
- identificador da aba, gerado uma vez e mantido em `sessionStorage`.

Após resposta de salvamento bem-sucedida, a cópia local correspondente é removida.

Se uma requisição `keepalive` puder ter sido concluída sem retorno ao JavaScript, a reconciliação seguinte compara o conteúdo, não apenas a versão.

## Detecção de conflitos

O diálogo de conflito só aparece para duas versões da mesma chave com conteúdos materiais diferentes.

### Casos sem diálogo

- payload local igual ao payload do servidor;
- diferença apenas em versão ou horário;
- cópia local antiga já representada integralmente no servidor;
- rascunhos com chaves diferentes;
- sessão atual vazia diante de rascunho anterior.

### Casos com diálogo

O diálogo aparece quando:

1. o servidor possui alterações não presentes na base conhecida localmente;
2. o cliente possui alterações diferentes ainda não persistidas;
3. não é possível escolher uma versão silenciosamente sem perda de dados.

O título será:

> **Existem alterações diferentes neste rascunho**

As opções serão:

- `Usar versão salva`;
- `Manter minhas alterações`;
- `Revisar antes de decidir`.

A interface mostrará o `savedAt` real da cópia local e o `updatedAt` informado pelo servidor. Não usará o horário atual como substituto.

## API de rascunhos

### Listagem de gastos

A API permitirá listar rascunhos ativos de `EXPENSE`, assim como já ocorre para obrigações.

A listagem devolverá:

- `contextKey`;
- `version`;
- `currentStep`;
- `updatedAt`;
- `expiresAt`.

A resposta pertence exclusivamente ao usuário autenticado.

### Descarte

O descarte acionado explicitamente pelo proprietário será idempotente e não exigirá versão. A versão continuará sendo usada para salvamentos concorrentes, mas não impedirá o usuário de excluir definitivamente o próprio rascunho.

A API nunca permitirá excluir rascunho pertencente a outro usuário.

### Conclusão múltipla

O serviço de submissão do gasto receberá `draftContextKey` e `previousDraftContextKey`. Após criar o gasto, removerá ambas na mesma transação e de forma idempotente.

## Exclusão lógica de gastos

### Estados

O estado persistido passa a usar:

- `ACTIVE` — lançamento vigente;
- `DELETED` — lançamento excluído logicamente.

O estado `CANCELLED` será removido do comportamento de negócio e migrado para `DELETED`.

### Migração

Uma migração Flyway executará:

```sql
ALTER TABLE expense ADD COLUMN deleted_at timestamp NULL;

UPDATE expense
SET status = 'DELETED',
    deleted_at = COALESCE(deleted_at, created_at)
WHERE status = 'CANCELLED';
```

Não será adicionada coluna `restored_at` nesta versão. Ao restaurar, `deleted_at` volta a `NULL`.

O registro do usuário responsável pela exclusão fica fora do escopo inicial.

### Exclusão

A ação `Excluir lançamento`:

1. exige uma confirmação clara;
2. altera o estado para `DELETED`;
3. registra `deletedAt` com o relógio da aplicação;
4. remove o gasto de relatórios, totais e listagens padrão;
5. preserva o registro e seus vínculos.

A exclusão de um gasto já `DELETED` será idempotente.

### Restauração

A ação `Restaurar`:

1. altera o estado de `DELETED` para `ACTIVE`;
2. define `deletedAt = null`;
3. faz o lançamento voltar a participar dos cálculos.

A restauração de um gasto já `ACTIVE` será idempotente.

### Listagem

A página de gastos terá dois filtros:

- `Ativos` — padrão;
- `Excluídos`.

Nos ativos, a ação será `Excluir lançamento`.

Nos excluídos, a ação será `Restaurar`.

O texto `Cancelar` não será usado para gastos.

## Impacto em relatórios e consultas

Toda consulta usada por dashboard, relatórios, rateios e totais deve excluir `DELETED` por padrão.

A listagem de restauração consultará excluídos explicitamente.

A verificação abrangerá repositórios e serviços que hoje assumem que qualquer registro encontrado está ativo.

## Acessibilidade e UX

- O aviso de rascunho anterior será uma região não modal com título, data e ações nomeadas.
- As ações funcionarão por teclado e terão foco visível.
- Mensagens de sucesso ou erro de descarte usarão `role="status"` ou `role="alert"` conforme a gravidade.
- Uma confirmação nunca será duplicada por outro `window.confirm()`.
- A exclusão de gasto usará linguagem explícita: `Excluir lançamento` e `Restaurar`.
- A confirmação explicará que a exclusão é reversível.

## Estratégia de implementação

A implementação será dividida em duas trilhas sequenciais:

1. corrigir o framework de rascunhos e o fluxo específico de novo gasto;
2. introduzir exclusão lógica, migração e restauração de gastos.

O framework compartilhado será alterado somente no que for necessário para o ciclo de vida correto. O comportamento dos demais formulários guiados será preservado e coberto por testes de regressão.

## Testes obrigatórios

### JavaScript

- abrir formulário não cria rascunho;
- primeira alteração real cria rascunho da sessão;
- eventos programáticos de inicialização não marcam o formulário como sujo;
- `pagehide` não salva quando o formulário está limpo;
- `pagehide` não salva durante descarte ou submissão final;
- descartar remove servidor e cópia local;
- continuar rascunho troca corretamente chave e versão;
- rascunho anterior e sessão atual coexistem sem conflito;
- payload igual com versões diferentes não abre conflito;
- payloads materiais diferentes abrem conflito;
- horário local exibido vem de `savedAt`;
- salvamentos concorrentes continuam serializados.

### Backend de rascunhos

- listar rascunhos de gasto por usuário;
- descarte idempotente sem versão;
- conclusão remove múltiplas chaves;
- falha de criação preserva todos os rascunhos;
- usuário não pode listar ou excluir rascunho de outro usuário.

### Gastos

- `CANCELLED` é migrado para `DELETED`;
- migração preenche `deleted_at` para registros antigos;
- exclusão muda `ACTIVE` para `DELETED`;
- restauração muda `DELETED` para `ACTIVE` e limpa `deleted_at`;
- excluídos não aparecem na listagem padrão;
- filtro de excluídos mostra somente `DELETED`;
- excluídos não entram em dashboard e relatórios;
- restaurados voltam aos cálculos;
- exclusão física não é exposta pela interface.

### Fluxo web

- formulário abre vazio mesmo com rascunho anterior;
- aviso de rascunho anterior é renderizado sem modal automático;
- criar gasto remove rascunho antigo e rascunho da sessão;
- erro de validação mantém os rascunhos;
- listagem usa `Excluir lançamento` e `Restaurar`, nunca `Cancelar`.

## Critérios de aceite

- O usuário consegue descartar um rascunho e ele não reaparece após recarregar ou sair da página.
- O formulário não pergunta repetidamente sobre o mesmo rascunho.
- Um rascunho anterior pode ser preservado enquanto um novo formulário é preenchido.
- A criação bem-sucedida remove todos os rascunhos relacionados ao fluxo.
- Falsos conflitos causados apenas por versão ou `keepalive` deixam de aparecer.
- Gastos podem ser excluídos e restaurados pela interface.
- Gastos excluídos não afetam resultados, relatórios ou totais.
- Registros antigos `CANCELLED` passam a aparecer como excluídos restauráveis.
- A CI completa passa antes de o trabalho ser considerado concluído.
