# Ciclo de vida de rascunhos, obrigações e exclusão lógica de gastos

Data: 2026-07-14

## Contexto

Os formulários guiados compartilham o mesmo framework de autosave e recuperação. O comportamento atual causa problemas diferentes em Gastos e Obrigações:

1. o usuário descarta um rascunho, mas o evento `pagehide` pode salvá-lo novamente;
2. a cópia de emergência local pode ser interpretada como conflito mesmo quando pertence ao mesmo navegador e já foi sincronizada;
3. o formulário de gastos usa uma chave estática e pergunta repetidamente sobre o mesmo rascunho;
4. cada acesso a “Nova obrigação” pode gerar outra chave, e a listagem oferece apenas `Continuar`, acumulando rascunhos sem um encerramento claro;
5. o cadastro de gastos oferece apenas “Cancelar”, embora o caso de uso real seja excluir um lançamento digitado por engano e, eventualmente, restaurá-lo.

A solução deve corrigir o ciclo de vida compartilhado, aplicar políticas diferentes por tipo de formulário e introduzir exclusão lógica restaurável para gastos.

## Objetivos

- Criar rascunho somente após uma alteração real do usuário.
- Impedir que um rascunho descartado seja recriado ao sair da página.
- Reduzir falsos conflitos e abrir diálogo apenas quando duas versões da mesma chave possuem alterações materiais diferentes.
- Abrir o formulário de novo gasto sempre com valores padrão, preservando um rascunho anterior separadamente até uma decisão do usuário ou a criação bem-sucedida.
- Permitir no máximo um rascunho ativo de obrigação por usuário.
- Oferecer uma tela de decisão antes de abrir “Nova obrigação” quando já existir uma obrigação em andamento.
- Limpar os rascunhos antigos de obrigação, mantendo somente o mais recentemente alterado.
- Substituir o conceito de “cancelamento” de gasto por exclusão lógica restaurável.
- Migrar gastos atualmente `CANCELLED` para `DELETED`.

## Fora de escopo

- Exclusão física de gastos pela interface.
- Histórico completo de auditoria por usuário nesta primeira versão.
- Edição de gastos já salvos.
- Reembolso ou estorno financeiro como entidade própria.
- Redesenho visual integral dos formulários guiados.
- Vários rascunhos simultâneos de obrigação.

# Regras compartilhadas de rascunho

## Estado interno do controlador

O controlador compartilhado deve distinguir:

- `dirty`: o payload atual difere do último estado persistido;
- `saving`: há uma operação de salvamento em andamento;
- `discarding`: um descarte foi iniciado;
- `submitting`: o formulário está sendo enviado definitivamente;
- `disposed`: o controlador não deve mais salvar;
- `lastPersistedPayload`: último payload confirmado pelo servidor;
- `initialPayload`: payload normalizado após toda inicialização programática do formulário.

## Alteração real

Apenas abrir o formulário não cria nem atualiza rascunho.

O controlador captura `initialPayload` depois que scripts específicos do formulário terminarem de preencher valores padrão e ajustar campos condicionais. Eventos programáticos de restauração, máscara, sincronização de datas ou mudança de visibilidade não contam como edição do usuário.

Eventos `input` e `change` originados do usuário recalculam o payload e marcam `dirty = true` somente quando ele difere de `lastPersistedPayload` ou, antes do primeiro salvamento, de `initialPayload`.

Se o usuário desfizer as alterações e retornar ao estado persistido, `dirty` volta para `false` e nenhum salvamento desnecessário é feito.

## Autosave e saída da página

- O primeiro autosave só ocorre após uma alteração real.
- Um salvamento bem-sucedido atualiza `lastPersistedPayload` e marca `dirty = false`.
- `pagehide` salva apenas se `dirty` for verdadeiro e o controlador não estiver em `discarding`, `submitting` ou `disposed`.
- Descarte cancela o timer e bloqueia novos salvamentos antes de chamar a API.
- Submissão final cancela o timer e impede um autosave paralelo com o POST principal.
- Salvamentos concorrentes continuam serializados para nunca reutilizar uma versão obsoleta.

## Descarte definitivo

O descarte explícito do proprietário será idempotente e não exigirá uma versão atual. A versão continuará protegendo salvamentos concorrentes, mas não impedirá o usuário de eliminar o próprio rascunho.

O fluxo deve:

1. apresentar uma única confirmação;
2. marcar o controlador como `discarding`;
3. cancelar o timer;
4. aguardar a operação em andamento e excluir a chave de forma idempotente;
5. remover a cópia de emergência local;
6. marcar o controlador como limpo e `disposed` antes de navegar;
7. impedir que `pagehide` recrie o rascunho.

A API nunca permitirá excluir rascunho pertencente a outro usuário.

## Cópia local de emergência

A cópia de emergência continua existindo para falhas de rede e incluirá:

- tipo e chave do rascunho;
- versão conhecida;
- payload normalizado;
- `savedAt` real;
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
- formulário vazio diante de um rascunho anterior;
- cópia de emergência de uma chave que foi descartada e não está mais ativa.

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

# Política de rascunho para Gastos

## Chaves independentes

O formulário não usará mais uma única chave estática `current` para todas as tentativas.

Cada abertura de `/expenses/new` receberá uma chave de sessão exclusiva, gerada pelo servidor e renderizada no formulário:

```text
expense:new:<uuid>
```

Um rascunho anterior continua com a própria chave. Assim, o usuário pode iniciar um novo preenchimento sem sobrescrever o anterior.

O formulário enviará duas chaves ocultas:

- `draftContextKey`: chave da sessão atual;
- `previousDraftContextKey`: chave do rascunho anterior mostrado no aviso, quando existir.

## Abrir com rascunho anterior

O cliente consulta a listagem de rascunhos `EXPENSE` e escolhe o ativo mais recentemente atualizado, excluindo a chave da sessão atual.

O formulário permanece vazio, com valores padrão, e mostra uma região não modal:

> **Existe um rascunho anterior**  
> Salvo em 14/07/2026 às 19:43.  
> **Continuar rascunho** · **Descartar rascunho**

Não haverá modal automático de recuperação.

## Continuar o rascunho anterior

Se o formulário atual não possui alterações:

1. o rascunho anterior é restaurado diretamente;
2. o controlador passa a usar sua chave e versão;
3. o aviso é removido.

Se o formulário atual possui alterações:

1. uma única confirmação informa que o preenchimento atual será descartado;
2. timers e salvamentos pendentes da sessão atual são interrompidos;
3. o rascunho temporário da sessão atual é removido, quando existir;
4. o rascunho anterior é restaurado;
5. o controlador passa a usar sua chave e versão.

## Descartar rascunho anterior

O rascunho anterior é excluído do servidor e do armazenamento local, o aviso desaparece e o formulário atual permanece inalterado.

## Descartar a sessão atual

A ação visível `Descartar rascunho` aplica o descarte definitivo compartilhado e navega para `/expenses/new`, gerando uma nova chave de sessão e mantendo o formulário vazio.

## Criar gasto com sucesso

A criação bem-sucedida remove na mesma transação:

- o rascunho da sessão atual;
- o rascunho anterior apresentado na abertura, se ainda existir.

As remoções são idempotentes. Se uma chave já não existir, a criação continua normalmente.

Em caso de erro de validação ou domínio, nenhum rascunho é removido.

# Política de rascunho para Obrigações

## Invariante de unicidade

Cada usuário pode possuir no máximo um rascunho ativo do tipo `OBLIGATION`.

Não será criada restrição global que afete outros tipos de formulário. A unicidade será aplicada pelo serviço de rascunhos para o par:

```text
proprietário + OBLIGATION
```

A chave continua no formato existente `draft:<uuid>`. Isso evita migração desnecessária do schema e permite reutilizar a validação atual. Uma nova chave só é persistida após a primeira alteração real.

## Limpeza dos rascunhos acumulados

Na primeira consulta da listagem de obrigações, na abertura de `/obligations/new` e antes de criar ou atualizar um rascunho `OBLIGATION`, o backend normaliza os rascunhos do usuário:

1. ordena por `updatedAt` decrescente;
2. usa o identificador do registro como desempate determinístico;
3. preserva apenas o mais recentemente alterado;
4. exclui todos os anteriores na mesma transação.

A limpeza é idempotente e restrita ao usuário autenticado.

A resposta da normalização informa as chaves removidas. O cliente elimina as cópias de emergência correspondentes do `localStorage`, evitando que uma cópia antiga tente recriar um rascunho descartado.

## Acesso a “Nova obrigação” sem rascunho

Quando não existe rascunho ativo:

1. `/obligations/new` abre o formulário vazio;
2. o servidor gera uma chave candidata `draft:<uuid>` e a renderiza no formulário;
3. nenhum registro é criado apenas pela abertura;
4. o primeiro autosave após alteração real cria o único rascunho ativo.

## Acesso a “Nova obrigação” com rascunho

Quando existe um rascunho ativo, `/obligations/new` não abre o formulário diretamente. Exibe uma tela curta:

> **Você já possui uma obrigação em andamento**  
> Salva em 14/07/2026 às 19:43.
>
> **Continuar rascunho**  
> **Descartar e começar novamente**  
> **Voltar para obrigações**

Essa tela não cria uma nova chave nem dispara autosave.

## Continuar rascunho

`Continuar rascunho` abre o formulário usando a chave e a versão do único rascunho ativo. O controlador restaura o payload sem marcar o formulário como alterado.

O backend valida que a chave pertence ao usuário e ainda é o rascunho ativo. Uma URL antiga não pode reabrir ou recriar uma chave removida.

## Descartar e começar novamente

A ação:

1. apresenta uma única confirmação explicando que o preenchimento salvo será perdido;
2. exclui o rascunho ativo de forma idempotente;
3. remove sua cópia local de emergência;
4. navega para uma abertura explicitamente nova do formulário;
5. gera outra chave candidata, ainda sem persistir rascunho;
6. impede `pagehide` de recriar a chave descartada.

## Sair do formulário

O botão ambíguo `Cancelar` será substituído por:

> **Sair e manter rascunho**

Se houver alterações sujas, a ação solicita um salvamento imediato e aguarda seu término antes de navegar para `/obligations`. Se não houver alteração real, apenas navega e nenhum rascunho é criado.

Uma ação separada `Descartar rascunho` ficará disponível dentro do formulário quando já houver rascunho persistido.

## Criar obrigação com sucesso

A submissão final cria a obrigação e remove o único rascunho ativo na mesma transação.

O serviço não confiará somente na chave enviada pelo navegador: após validar a propriedade, também eliminará qualquer rascunho `OBLIGATION` remanescente daquele usuário. Isso reforça a invariante de unicidade e encerra duplicatas antigas que tenham surgido por concorrência.

Se a criação falhar, o rascunho permanece disponível.

## Várias abas

Duas abas podem editar o mesmo rascunho ativo. Elas compartilham a mesma chave, portanto o controle de versão continua válido.

- payload equivalente é reconciliado silenciosamente;
- alterações materiais diferentes produzem o diálogo real de conflito;
- nenhuma aba pode criar um segundo rascunho enquanto o primeiro existir.

# API de rascunhos

## Listagem

A API permitirá listar rascunhos ativos de `EXPENSE` e `OBLIGATION` do usuário autenticado.

A resposta devolverá ao menos:

- `contextKey`;
- `version`;
- `currentStep`;
- `updatedAt`;
- `expiresAt`.

Para `OBLIGATION`, a listagem será normalizada e retornará zero ou um item, junto das chaves antigas removidas durante a limpeza.

## Descarte

O descarte explícito será idempotente e sem versão obrigatória. O endpoint sempre limita a operação ao proprietário autenticado.

## Conclusão múltipla de gastos

O serviço de submissão do gasto receberá `draftContextKey` e `previousDraftContextKey` e removerá ambas na mesma transação.

## Conclusão de obrigação

O serviço de submissão da obrigação removerá a chave usada e qualquer outro rascunho `OBLIGATION` remanescente do mesmo usuário na mesma transação.

# Exclusão lógica de gastos

## Estados

O estado persistido passa a usar:

- `ACTIVE` — lançamento vigente;
- `DELETED` — lançamento excluído logicamente.

O estado `CANCELLED` será removido do comportamento de negócio e migrado para `DELETED`.

## Migração

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

## Exclusão

A ação `Excluir lançamento`:

1. exige uma confirmação clara;
2. altera o estado para `DELETED`;
3. registra `deletedAt` com o relógio da aplicação;
4. remove o gasto de relatórios, totais e listagens padrão;
5. preserva o registro e seus vínculos.

A exclusão de um gasto já `DELETED` será idempotente.

## Restauração

A ação `Restaurar`:

1. altera o estado de `DELETED` para `ACTIVE`;
2. define `deletedAt = null`;
3. faz o lançamento voltar a participar dos cálculos.

A restauração de um gasto já `ACTIVE` será idempotente.

## Listagem

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

# Acessibilidade e UX

- O aviso de rascunho anterior de gasto será uma região não modal com título, data e ações nomeadas.
- A tela de decisão de obrigação terá um título único, explicação curta e três ações inequívocas.
- As ações funcionarão por teclado e terão foco visível.
- Mensagens de sucesso ou erro de descarte usarão `role="status"` ou `role="alert"` conforme a gravidade.
- Uma confirmação nunca será duplicada por outro `window.confirm()`.
- `Sair e manter rascunho` não será apresentado como cancelamento.
- A exclusão de gasto usará `Excluir lançamento` e `Restaurar` e explicará que a exclusão é reversível.

# Estratégia de implementação

A implementação será dividida em três trilhas sequenciais:

1. corrigir estado, autosave, descarte e conflitos no framework compartilhado;
2. aplicar as políticas específicas de Gastos e Obrigações, incluindo a limpeza do legado de obrigações;
3. introduzir exclusão lógica, migração e restauração de gastos.

As alterações compartilhadas serão cobertas por testes de regressão para os demais formulários guiados.

# Testes obrigatórios

## JavaScript compartilhado

- abrir formulário não cria rascunho;
- primeira alteração real cria rascunho;
- eventos programáticos de inicialização não marcam o formulário como sujo;
- desfazer uma edição volta a `dirty = false`;
- `pagehide` não salva quando o formulário está limpo;
- `pagehide` não salva durante descarte ou submissão final;
- descartar remove servidor e cópia local;
- payload igual com versões diferentes não abre conflito;
- payloads materiais diferentes abrem conflito;
- horário local exibido vem de `savedAt`;
- salvamentos concorrentes continuam serializados.

## Backend de rascunhos

- listar rascunhos de gasto por usuário;
- descarte idempotente sem versão;
- usuário não pode listar ou excluir rascunho de outro usuário;
- conclusão de gasto remove múltiplas chaves;
- falha de criação preserva os rascunhos relacionados.

## Obrigações

- zero rascunhos permite abrir o formulário vazio;
- abrir o formulário sem editar não cria rascunho;
- o primeiro autosave cria um único rascunho;
- vários rascunhos legados são reduzidos ao mais recente;
- empate de `updatedAt` usa critério determinístico;
- as chaves antigas removidas são informadas ao cliente;
- existir rascunho faz `/obligations/new` mostrar a tela de decisão;
- `Continuar rascunho` abre somente a chave ativa do proprietário;
- `Descartar e começar novamente` remove o rascunho e abre formulário vazio;
- `Sair e manter rascunho` aguarda o salvamento quando necessário;
- salvar obrigação remove todos os rascunhos `OBLIGATION` do usuário;
- falha de criação mantém o rascunho;
- duas abas editam a mesma chave e não criam um segundo rascunho;
- cópias locais das chaves legadas removidas são apagadas.

## Gastos

- formulário abre vazio mesmo com rascunho anterior;
- aviso de rascunho anterior é renderizado sem modal automático;
- rascunho anterior e sessão atual coexistem com chaves diferentes;
- continuar rascunho troca corretamente chave e versão;
- criar gasto remove rascunho antigo e rascunho da sessão;
- erro de validação mantém os rascunhos;
- `CANCELLED` é migrado para `DELETED`;
- migração preenche `deleted_at` para registros antigos;
- exclusão muda `ACTIVE` para `DELETED`;
- restauração muda `DELETED` para `ACTIVE` e limpa `deleted_at`;
- excluídos não aparecem na listagem padrão;
- filtro de excluídos mostra somente `DELETED`;
- excluídos não entram em dashboard e relatórios;
- restaurados voltam aos cálculos;
- listagem usa `Excluir lançamento` e `Restaurar`, nunca `Cancelar`;
- exclusão física não é exposta pela interface.

# Critérios de aceite

- O usuário consegue descartar um rascunho e ele não reaparece após recarregar ou sair da página.
- O sistema não pergunta repetidamente sobre o mesmo rascunho sem nova alteração material.
- Um rascunho anterior de gasto pode ser preservado enquanto um novo formulário é preenchido.
- A criação bem-sucedida de gasto remove todos os rascunhos relacionados ao fluxo.
- Cada usuário possui no máximo um rascunho de obrigação.
- Ao clicar em `Nova obrigação` com rascunho existente, o usuário escolhe entre continuar, descartar e começar novamente ou voltar.
- Entre rascunhos antigos de obrigação, somente o mais recentemente alterado é preservado.
- Salvar uma obrigação encerra seu rascunho e quaisquer duplicatas remanescentes.
- Falsos conflitos causados apenas por versão ou `keepalive` deixam de aparecer.
- Gastos podem ser excluídos e restaurados pela interface.
- Gastos excluídos não afetam resultados, relatórios ou totais.
- Registros antigos `CANCELLED` passam a aparecer como excluídos restauráveis.
- A CI completa passa antes de o trabalho ser considerado concluído.
