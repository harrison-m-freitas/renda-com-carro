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

Cada abertura de formulário novo receberá uma chave de sessão exclusiva, por exemplo:

```text
expense:new:<uuid>
```

Um rascunho anterior continuará com sua própria chave. Dessa forma, o usuário pode começar um novo preenchimento sem sobrescrever o rascunho antigo.

### Rascunho anterior

Ao abrir `/expenses/new`, o servidor ou o cliente identifica o rascunho ativo mais recente do tipo `EXPENSE`, excluindo a chave da sessão atual.

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
2. impedir novos salvamentos durante o descarte;
3. aguardar ou invalidar a operação de salvamento em andamento;
4. excluir o rascunho da sessão atual sem exigir uma versão já obsoleta;
5. remover a cópia de emergência local;
6. marcar o controlador como limpo;
7. redirecionar para um novo formulário vazio ou limpar o formulário atual;
8. impedir que `pagehide` recrie o rascunho.

Não haverá `window.confirm()` adicional quando a confirmação já estiver dentro de um componente próprio da interface.

### Criar gasto com sucesso

A criação bem-sucedida deve remover:

- o rascunho da sessão atual;
- o rascunho anterior apresentado na abertura, se ainda existir;
- as cópias locais de emergência associadas às duas chaves.

A remoção no servidor deve fazer parte da mesma conclusão transacional do cadastro sempre que possível.

Se a submissão falhar por validação ou erro de domínio, nenhum rascunho é removido.

## Autosave e saída da página

### Estado interno

O controlador compartilhado deve distinguir pelo menos:

- `dirty`: existem alterações locais ainda não persistidas;
- `saving`: há uma operação em andamento;
- `discarding`: um descarte foi iniciado;
- `disposed`: o controlador não deve mais salvar;
- `lastPersistedPayload`: último payload confirmado pelo servidor.

### Regras

- `pagehide` salva apenas se `dirty` for verdadeiro e o controlador não estiver em `discarding` ou `disposed`.
- um salvamento bem-sucedido atualiza `lastPersistedPayload` e marca `dirty = false`;
- um novo evento de entrada compara o payload atual com o último persistido, evitando salvar alterações que apenas voltaram ao estado anterior;
- descarte limpa timer, estado local e cópia de emergência antes de qualquer navegação;
- inicialização de valores padrão não agenda autosave.

## Cópia local de emergência

A cópia de emergência continua existindo para falhas de rede, mas precisa incluir:

- chave do rascunho;
- versão conhecida;
- payload normalizado;
- `savedAt` real da cópia;
- identificador estável da sessão ou aba, quando disponível.

Após resposta de salvamento bem-sucedida, a cópia local correspondente é removida.

Se uma requisição `keepalive` puder ter sido concluída sem retorno ao JavaScript, a reconciliação seguinte deve comparar o conteúdo, não apenas a versão.

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

1. o servidor possui alterações não presentes na base conhecida localmente; e
2. o cliente possui alterações diferentes ainda não persistidas; e
3. não é possível combinar as alterações com segurança.

O título será:

> **Existem alterações diferentes neste rascunho**

As opções serão:

- `Usar versão salva`;
- `Manter minhas alterações`;
- `Revisar antes de decidir`.

A interface mostrará o horário real da cópia local e o horário informado pelo servidor. Não usará o horário atual como substituto.

## API de rascunhos

### Listagem de gastos

A API deve permitir listar rascunhos ativos de `EXPENSE`, assim como já ocorre para obrigações, para identificar o rascunho anterior mais recente.

A listagem precisa devolver ao menos:

- `contextKey`;
- `version`;
- `currentStep`;
- `updatedAt`;
- `expiresAt`.

### Descarte

O descarte acionado explicitamente pelo usuário deve ser idempotente.

Para impedir falha por versão obsoleta durante um descarte intencional, a API poderá oferecer modo de descarte forçado da própria chave, limitado ao usuário proprietário. A versão continuará útil para operações normais de concorrência, mas não deve impedir o usuário de eliminar definitivamente o próprio rascunho.

### Conclusão múltipla

O serviço de submissão do gasto deve receber as chaves relacionadas à sessão atual e ao rascunho anterior. Após criar o gasto, remove ambas de forma idempotente.

## Exclusão lógica de gastos

### Estados

O estado persistido passa a usar:

- `ACTIVE` — lançamento vigente;
- `DELETED` — lançamento excluído logicamente.

O estado `CANCELLED` será removido do comportamento de negócio e migrado para `DELETED`.

### Migração

Uma migração Flyway deve executar:

```sql
UPDATE expense
SET status = 'DELETED'
WHERE status = 'CANCELLED';
```

A migração pode também adicionar metadados de exclusão, caso aprovados para esta versão:

- `deleted_at timestamp null`;
- `restored_at timestamp null`.

O registro do usuário responsável fica fora do escopo inicial, salvo se a estrutura atual permitir adicioná-lo sem ampliar significativamente o trabalho.

### Exclusão

A ação `Excluir lançamento`:

1. exige uma confirmação clara;
2. altera o estado para `DELETED`;
3. registra `deletedAt`, se a coluna existir;
4. remove o gasto de relatórios, totais e listagens padrão;
5. preserva o registro e seus vínculos.

### Restauração

A ação `Restaurar`:

1. altera o estado de `DELETED` para `ACTIVE`;
2. limpa ou atualiza metadados de exclusão;
3. faz o lançamento voltar a participar dos cálculos.

### Listagem

A página de gastos terá filtros:

- `Ativos` — padrão;
- `Excluídos`;
- opcionalmente `Todos`.

Nos ativos, a ação será `Excluir lançamento`.

Nos excluídos, a ação será `Restaurar`.

O texto `Cancelar` não será usado para gastos.

## Impacto em relatórios e consultas

Toda consulta usada por dashboard, relatórios, rateios e totais deve excluir `DELETED` por padrão.

Consultas administrativas ou de restauração podem incluir excluídos explicitamente.

A verificação deve abranger repositórios e serviços que hoje assumem que qualquer registro encontrado está ativo.

## Acessibilidade e UX

- O aviso de rascunho anterior será uma região não modal com título, data e ações nomeadas.
- As ações devem funcionar por teclado e possuir foco visível.
- Mensagens de sucesso ou erro de descarte usam `role="status"` ou `role="alert"` conforme a gravidade.
- Uma confirmação nunca será duplicada por outro `window.confirm()`.
- A exclusão de gasto usará linguagem explícita: `Excluir lançamento` e `Restaurar`.
- O usuário deve entender que a exclusão é reversível.

## Estratégia de implementação

A implementação será dividida em duas trilhas sequenciais:

1. corrigir o framework de rascunhos e o fluxo específico de novo gasto;
2. introduzir exclusão lógica, migração e restauração de gastos.

O framework compartilhado será alterado somente no que for necessário para o ciclo de vida correto. O comportamento dos demais formulários guiados será preservado e coberto por testes de regressão.

## Testes obrigatórios

### JavaScript

- abrir formulário não cria rascunho;
- primeira alteração real cria rascunho da sessão;
- `pagehide` não salva quando o formulário está limpo;
- `pagehide` não salva durante ou após descarte;
- descartar remove servidor e cópia local;
- continuar rascunho troca corretamente chave e versão;
- rascunho anterior e sessão atual coexistem sem conflito;
- payload igual com versões diferentes não abre conflito;
- payloads materiais diferentes abrem conflito;
- horário local exibido vem de `savedAt`;
- salvamentos concorrentes continuam serializados.

### Backend de rascunhos

- listar rascunhos de gasto por usuário;
- descarte idempotente;
- descarte explícito não falha por versão antiga quando autorizado;
- conclusão remove múltiplas chaves;
- falha de criação preserva todos os rascunhos;
- usuário não pode listar ou excluir rascunho de outro usuário.

### Gastos

- `CANCELLED` é migrado para `DELETED`;
- exclusão muda `ACTIVE` para `DELETED`;
- restauração muda `DELETED` para `ACTIVE`;
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
