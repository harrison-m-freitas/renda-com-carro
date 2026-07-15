# Ciclo de vida de rascunhos, obrigações e exclusão lógica de gastos

Data: 2026-07-14

## Contexto

Os formulários guiados compartilham o mesmo mecanismo de autosave, recuperação e conflito. O comportamento atual causa problemas diferentes:

1. um rascunho descartado pode ser salvo novamente pelo evento `pagehide`;
2. uma cópia local já sincronizada pode ser interpretada como conflito;
3. Gastos pergunta repetidamente sobre uma chave estática e não diferencia um preenchimento novo do rascunho anterior;
4. Obrigações gera chaves independentes a cada tentativa e acumula vários rascunhos sem ação clara para encerrá-los;
5. gastos salvos oferecem “Cancelar”, embora o caso de uso seja excluir um lançamento equivocado e poder restaurá-lo.

A solução aplica regras compartilhadas de ciclo de vida, políticas específicas para Gastos e Obrigações e exclusão lógica restaurável para gastos.

## Decisões aprovadas

- Rascunho só é criado após alteração real do usuário.
- Descartar é definitivo e não pode ser desfeito por autosave de saída.
- Conflito só aparece quando há diferença material.
- Gastos abre vazio e pode preservar temporariamente um rascunho anterior separado.
- Obrigações permite no máximo um rascunho ativo por usuário.
- Ao abrir Nova obrigação com rascunho existente, aparece uma tela para continuar, descartar e começar novamente ou voltar.
- Entre rascunhos antigos de obrigação, somente o mais recentemente alterado será preservado.
- Gastos `CANCELLED` serão migrados para `DELETED`, com restauração disponível.

## Fora de escopo

- Exclusão física de gastos pela interface.
- Edição de gastos já salvos.
- Histórico completo de auditoria por usuário.
- Reembolso ou estorno como entidade financeira própria.
- Vários rascunhos simultâneos de obrigação.
- Redesenho visual completo dos demais formulários guiados.

# 1. Regras compartilhadas de rascunho

## 1.1 Estado do controlador

O controlador compartilhado distinguirá:

- `initialPayload`: payload normalizado após a inicialização programática;
- `lastPersistedPayload`: último payload confirmado pelo servidor;
- `dirty`: o payload atual difere da base aplicável;
- `saving`: existe salvamento em andamento;
- `discarding`: descarte definitivo iniciado;
- `submitting`: submissão final iniciada;
- `disposed`: nenhum novo salvamento é permitido.

## 1.2 Alteração real

Abrir um formulário não cria nem atualiza rascunho.

O controlador captura `initialPayload` somente depois que os scripts específicos terminarem de preencher valores padrão, aplicar máscaras e configurar campos condicionais.

Eventos programáticos de inicialização, restauração, formatação, sincronização de datas ou mudança de visibilidade não contam como edição do usuário.

Eventos reais de `input` e `change` recalculam o payload:

- antes do primeiro salvamento, a comparação usa `initialPayload`;
- depois do primeiro salvamento, usa `lastPersistedPayload`;
- voltar exatamente ao estado-base define `dirty = false`.

## 1.3 Autosave e saída

- O primeiro autosave ocorre somente depois de alteração real.
- Salvamentos continuam serializados para não reutilizar versão obsoleta.
- Sucesso atualiza `lastPersistedPayload` e define `dirty = false`.
- `pagehide` salva somente quando `dirty = true` e o controlador não está em `discarding`, `submitting` ou `disposed`.
- A submissão final cancela o timer e impede autosave paralelo ao POST principal.

## 1.4 Descarte definitivo

O descarte explícito do proprietário é idempotente e não exige versão atual. A versão continua protegendo salvamentos, mas não impede que o usuário elimine o próprio rascunho.

O fluxo:

1. apresenta uma única confirmação;
2. define `discarding = true`;
3. cancela o timer;
4. aguarda o salvamento em andamento;
5. exclui a chave de forma idempotente;
6. remove a cópia local de emergência;
7. define `dirty = false` e `disposed = true` antes de navegar;
8. impede que `pagehide` recrie o rascunho.

A API limita listagem, leitura e descarte ao proprietário autenticado.

## 1.5 Cópia local de emergência

A cópia local permanece como proteção contra falha de rede e contém:

- tipo e chave;
- versão conhecida;
- payload normalizado;
- `savedAt` real;
- identificador da aba mantido em `sessionStorage`.

Após resposta bem-sucedida, a cópia correspondente é removida.

Quando um `keepalive` pode ter sido concluído sem retorno ao JavaScript, a reconciliação compara conteúdo, não apenas versão.

## 1.6 Conflitos

O diálogo compartilhado aparece somente para versões da mesma chave com conteúdos materiais diferentes.

Não há diálogo quando:

- os payloads são equivalentes;
- a diferença é apenas de versão ou horário;
- a cópia local já está integralmente representada no servidor;
- as chaves são diferentes;
- a cópia pertence a uma chave definitivamente descartada.

Quando há conflito real, o título será:

> **Existem alterações diferentes neste rascunho**

Ações:

- `Usar versão salva`;
- `Manter minhas alterações`;
- `Revisar antes de decidir`.

A interface mostra o `savedAt` real da cópia local e o `updatedAt` do servidor.

# 2. Política de rascunho para Gastos

## 2.1 Chaves independentes

Cada abertura de `/expenses/new` recebe uma chave candidata exclusiva:

```text
expense:new:<uuid>
```

Ela só é persistida após alteração real.

O formulário envia:

- `draftContextKey`: chave da tentativa atual;
- `previousDraftContextKey`: rascunho anterior apresentado ao usuário, quando existir.

## 2.2 Abertura

O formulário sempre abre vazio, com os valores padrão.

A listagem de rascunhos `EXPENSE` identifica o mais recentemente atualizado, excluindo a chave atual. Quando existe, aparece uma região não modal:

> **Existe um rascunho anterior**  
> Salvo em 14/07/2026 às 19:43.  
> **Continuar rascunho** · **Descartar rascunho**

Não há modal automático.

## 2.3 Continuar rascunho anterior

Sem alterações na sessão atual, o sistema restaura o rascunho diretamente e passa a usar sua chave e versão.

Com alterações na sessão atual, uma única confirmação informa que o preenchimento atual será descartado. O sistema interrompe timers, elimina o rascunho temporário atual quando existir e restaura o anterior.

## 2.4 Descartar

- Descartar o rascunho anterior remove apenas aquela chave e mantém o formulário atual.
- Descartar a sessão atual aplica o descarte definitivo e navega para uma nova abertura vazia.

## 2.5 Criação bem-sucedida

A criação do gasto remove na mesma transação:

- a chave da sessão atual;
- a chave anterior mostrada na abertura, quando ainda existir.

As remoções são idempotentes. Erro de validação ou domínio preserva os rascunhos.

# 3. Política de rascunho para Obrigações

## 3.1 Invariante

Cada usuário pode possuir no máximo um rascunho persistido do tipo `OBLIGATION`.

A chave continua no formato atual:

```text
draft:<uuid>
```

Uma chave candidata pode existir apenas no HTML ou na memória antes da primeira alteração; ela não representa um registro persistido.

## 3.2 Limpeza dos rascunhos existentes

Uma migração Flyway elimina as duplicatas existentes, preservando o rascunho mais recentemente alterado por usuário:

```sql
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY owner_id
               ORDER BY updated_at DESC, id DESC
           ) AS position
    FROM form_draft
    WHERE form_type = 'OBLIGATION'
)
DELETE FROM form_draft
WHERE id IN (
    SELECT id
    FROM ranked
    WHERE position > 1
);
```

O desempate por `id DESC` torna o resultado determinístico quando `updated_at` é igual.

## 3.3 Garantia atômica

Depois da limpeza, a mesma migração cria um índice único parcial:

```sql
CREATE UNIQUE INDEX ux_form_draft_single_obligation_owner
    ON form_draft (owner_id)
    WHERE form_type = 'OBLIGATION';
```

Esse índice garante a regra mesmo com duas abas ou requisições concorrentes.

Antes de criar um rascunho de obrigação, o serviço exclui rascunhos expirados daquele usuário. Assim, uma linha vencida não bloqueia uma nova obrigação pelo índice.

Se duas abas tentarem criar chaves diferentes:

1. o primeiro insert confirmado vence;
2. o segundo é rejeitado pelo índice;
3. o serviço captura a violação de unicidade e devolve conflito específico `OBLIGATION_DRAFT_ALREADY_EXISTS`, incluindo o resumo do rascunho ativo;
4. nenhum segundo registro é criado.

## 3.4 Limpeza das cópias locais antigas

Ao entrar na listagem ou nos fluxos de obrigação, o cliente varre apenas as chaves de emergência do tipo `OBLIGATION`:

- preserva a chave ativa devolvida pelo servidor;
- remove as demais chaves locais;
- nunca toca nas cópias de Gastos, Metas ou Fechamentos.

Isso evita que rascunhos removidos pela migração reapareçam por `localStorage`.

## 3.5 Nova obrigação sem rascunho

Quando não existe rascunho ativo:

1. `/obligations/new` abre o formulário vazio;
2. o servidor gera uma chave candidata `draft:<uuid>`;
3. abrir ou sair sem editar não cria registro;
4. a primeira alteração real cria o único rascunho.

## 3.6 Nova obrigação com rascunho

Quando existe rascunho ativo, `/obligations/new` mostra uma tela curta em vez do formulário:

> **Você já possui uma obrigação em andamento**  
> Salva em 14/07/2026 às 19:43.
>
> **Continuar rascunho**  
> **Descartar e começar novamente**  
> **Voltar para obrigações**

A tela não gera chave candidata e não dispara autosave.

## 3.7 Continuar

`Continuar rascunho` abre o formulário com a chave e versão ativas. A restauração não marca o formulário como alterado.

O backend valida propriedade e atividade. URL antiga não reabre nem recria chave removida.

## 3.8 Descartar e começar novamente

A ação:

1. apresenta uma única confirmação;
2. descarta o rascunho ativo;
3. remove sua cópia local;
4. navega para uma abertura explicitamente nova;
5. gera uma nova chave candidata ainda não persistida.

## 3.9 Sair do formulário

O botão `Cancelar` é substituído por:

> **Sair e manter rascunho**

Com alterações sujas, a ação solicita salvamento imediato e aguarda o resultado antes de navegar para `/obligations`. Sem alteração real, apenas navega e não cria rascunho.

Quando já existe rascunho persistido, o formulário também oferece `Descartar rascunho`.

## 3.10 Corrida entre abas antes do primeiro autosave

Quando uma segunda aba recebe `OBLIGATION_DRAFT_ALREADY_EXISTS`, suas alterações ainda não persistidas permanecem no formulário e em uma cópia local temporária. A interface apresenta:

> **Outra obrigação em andamento foi salva primeiro**

Ações:

- `Continuar obrigação salva`: confirma o descarte do preenchimento desta aba e abre a chave ativa;
- `Substituir pela obrigação desta aba`: confirma, descarta a chave ativa e salva o preenchimento atual como o único rascunho;
- `Revisar minhas alterações`: fecha o aviso sem tentar novo autosave até nova decisão.

A substituição é uma operação atômica do proprietário: remove a chave ativa e cria a nova na mesma transação. Nunca existem dois registros após o commit.

## 3.11 Criação bem-sucedida

A submissão final cria a obrigação e remove a chave ativa na mesma transação.

Como defesa adicional, o serviço remove qualquer outro rascunho `OBLIGATION` do usuário antes de concluir. Com o índice parcial, isso só deve encontrar zero ou um registro.

Erro de validação ou domínio preserva o rascunho.

# 4. API de rascunhos

## 4.1 Listagem

A API lista rascunhos `EXPENSE` e `OBLIGATION` do usuário autenticado e devolve:

- `contextKey`;
- `version`;
- `currentStep`;
- `updatedAt`;
- `expiresAt`.

A listagem `OBLIGATION` devolve zero ou um item.

## 4.2 Descarte

O descarte explícito é idempotente, não exige versão e atua somente sobre rascunho do proprietário.

## 4.3 Substituição de obrigação

A API oferece uma operação específica e transacional para substituir o único rascunho de obrigação. Ela valida o usuário, elimina o ativo e persiste o novo payload dentro da mesma transação.

## 4.4 Conclusão

- Gasto: remove `draftContextKey` e `previousDraftContextKey`.
- Obrigação: remove o único rascunho `OBLIGATION` do usuário.

# 5. Exclusão lógica de gastos

## 5.1 Estados e migração

Estados válidos:

- `ACTIVE`;
- `DELETED`.

Uma migração adiciona `deleted_at` e converte registros antigos:

```sql
ALTER TABLE expense ADD COLUMN deleted_at timestamp NULL;

UPDATE expense
SET status = 'DELETED',
    deleted_at = COALESCE(deleted_at, created_at)
WHERE status = 'CANCELLED';
```

Não haverá `restored_at` nesta versão. Restaurar define `deleted_at = NULL`.

## 5.2 Excluir lançamento

A ação:

1. apresenta confirmação explicando que é reversível;
2. define `status = 'DELETED'`;
3. registra `deletedAt` com o relógio da aplicação;
4. remove o gasto das consultas operacionais;
5. preserva o registro e seus vínculos.

Excluir um gasto já excluído é idempotente.

## 5.3 Restaurar

A restauração define `status = 'ACTIVE'`, limpa `deletedAt` e devolve o gasto aos cálculos. Restaurar um ativo é idempotente.

## 5.4 Listagem e consultas

A página de gastos terá:

- `Ativos` — padrão;
- `Excluídos`.

Ativos oferecem `Excluir lançamento`; excluídos oferecem `Restaurar`. O termo `Cancelar` não será usado.

Dashboard, relatórios, rateios e totais excluem `DELETED` por padrão. A tela de restauração consulta excluídos explicitamente.

# 6. Acessibilidade e linguagem

- O aviso de rascunho anterior de gasto será uma região não modal.
- A decisão de obrigação terá título único, texto curto e três ações inequívocas.
- Foco visível e navegação por teclado serão preservados.
- Sucesso usa `role="status"`; erro relevante usa `role="alert"`.
- Não haverá modal seguido de outro `window.confirm()` para a mesma decisão.
- `Sair e manter rascunho`, `Descartar rascunho`, `Excluir lançamento` e `Restaurar` terão significados distintos.

# 7. Estratégia de implementação

A execução será sequencial:

1. corrigir `dirty`, autosave, descarte e conflito no framework compartilhado;
2. implementar o fluxo de Gastos;
3. migrar, restringir e redesenhar o fluxo de Obrigações;
4. implementar exclusão lógica e restauração de gastos;
5. validar a CI completa e corrigir falhas com debugging sistemático.

# 8. Testes obrigatórios

## 8.1 JavaScript compartilhado

- abrir não cria rascunho;
- primeira alteração real cria rascunho;
- eventos programáticos não marcam `dirty`;
- desfazer edição volta a `dirty = false`;
- `pagehide` respeita `dirty`, `discarding`, `submitting` e `disposed`;
- descarte remove cópia local e não recria servidor;
- payload equivalente não abre conflito;
- diferença material abre conflito;
- horário local usa `savedAt`;
- salvamentos permanecem serializados.

## 8.2 Gastos

- abre vazio com rascunho anterior;
- mostra aviso sem modal automático;
- sessão atual e anterior usam chaves diferentes;
- continuar troca chave e versão;
- criação remove as duas chaves;
- erro preserva os rascunhos.

## 8.3 Obrigações

- migração preserva somente o rascunho mais recente por usuário;
- empate usa `id DESC`;
- índice parcial impede segundo rascunho;
- rascunho expirado é removido antes de nova criação;
- abrir sem editar não cria registro;
- existir rascunho mostra a tela de decisão;
- continuar aceita somente a chave ativa do proprietário;
- descartar e começar abre formulário vazio;
- sair e manter aguarda salvamento;
- chaves locais antigas são removidas sem afetar outros tipos;
- corrida entre abas retorna `OBLIGATION_DRAFT_ALREADY_EXISTS`;
- substituir rascunho é atômico;
- salvar obrigação remove o rascunho;
- erro de criação preserva o rascunho.

## 8.4 Exclusão de gastos

- `CANCELLED` migra para `DELETED` com `deleted_at`;
- exclusão e restauração são idempotentes;
- excluídos somem da listagem padrão, dashboard e relatórios;
- filtro mostra excluídos;
- restaurados voltam aos cálculos;
- interface nunca usa `Cancelar` para gasto.

# 9. Critérios de aceite

- Rascunho descartado não reaparece após recarregar ou sair.
- O sistema não repete perguntas sem nova diferença material.
- Gasto novo pode coexistir temporariamente com um rascunho anterior sem sobrescrevê-lo.
- Criar gasto remove os rascunhos relacionados.
- Cada usuário possui no máximo um rascunho persistido de obrigação, inclusive sob concorrência.
- Nova obrigação com rascunho existente exige uma decisão explícita.
- Dos rascunhos antigos de obrigação, somente o mais recente permanece.
- Salvar obrigação encerra o rascunho.
- Gastos podem ser excluídos e restaurados.
- Gastos excluídos não afetam cálculos.
- Registros antigos `CANCELLED` aparecem como excluídos restauráveis.
- A CI completa passa antes de o trabalho ser considerado concluído.
