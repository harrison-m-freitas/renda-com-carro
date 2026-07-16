# PR #15 — Correções P0 e P1

## Objetivo

Tornar o PR #15 integrável com a `main` atual e corrigir os defeitos financeiros graves encontrados na revisão antes de qualquer teste manual.

## Restrições

- O PR permanece em rascunho.
- Não realizar merge ou auto-merge na `main`.
- Integrar a `main` na branch `feat/obligation-financing-redesign` por merge commit, preservando o histórico.
- Usar TDD para cada comportamento financeiro corrigido.
- Executar a CI completa no novo head antes de afirmar conclusão.
- Não ampliar o escopo para os itens P2.

## P0 — Integração com a main

### Merge e conflitos

A branch deve incorporar a `main` atual. Nas resoluções, as funcionalidades recentes da `main` têm precedência fora do domínio de obrigações; o redesenho financeiro do PR tem precedência somente onde for necessário para entregar o novo modelo.

### Flyway

A `main` já possui `V11__add_user_time_zone.sql` e `V12__enforce_single_obligation_draft.sql`. A migration financeira deve passar a usar a próxima versão livre, atualmente `V16__redesign_financial_obligations.sql`.

A V13 continua deliberadamente destrutiva apenas para as tabelas financeiras sem dados válidos a preservar. Ela também remove rascunhos antigos de `OBLIGATION` antes de o formulário passar a operar exclusivamente no schema 2. Os demais rascunhos e áreas permanecem intactos.

### Rascunho único

O formulário redesenhado deve preservar o fluxo atual da `main`:

- um único rascunho ativo de obrigação por usuário;
- tela de decisão para continuar ou descartar;
- `fresh=true` para iniciar após descarte;
- validação de que o `draftKey` pertence ao usuário;
- descarte explícito;
- autosave, conflito otimista e recuperação atuais;
- `acquisitionPlanId` preservado no payload do rascunho;
- ao salvar uma obrigação iniciada em um plano, redirecionar para o detalhe desse plano.

## P1 — Correções financeiras

### Valores localizados e canônicos

O navegador deve distinguir entrada brasileira de valor canônico restaurado. Dinheiro será restaurado com duas casas e percentuais com até quatro. O valor canônico `0.005` deve representar `0,005`, nunca `5`.

A API de formatação deve expor funções explícitas para valores de usuário e valores canônicos, evitando heurísticas ambíguas na restauração de rascunhos.

### Referência de pagamento

`externalReference` deve ser normalizada com `trim`; vazio vira `null`; o limite é 120 caracteres. Vários pagamentos sem referência devem ser aceitos. Referência duplicada deve produzir erro de formulário, não erro 500.

### Parcelamento sem juros

A divisão deve ser feita em centavos inteiros. A soma das parcelas deve ser exatamente igual ao principal, todas as parcelas devem ser positivas e a diferença de centavos deve ser distribuída deterministicamente entre as parcelas, sem criar uma parcela final de R$ 0,00.

### Status do cronograma

Como pagamentos ainda não são conciliados automaticamente com parcelas, a interface não deve afirmar status `Pendente`/`Pago` por parcela. O status enganoso será removido da tabela neste ciclo; a conciliação fica fora do escopo.

### Totais desconhecidos do plano

O resumo do plano deve indicar quando o total de quitação e o custo financeiro são parciais ou desconhecidos. Obrigações sem `plannedTotalAmount` não podem ser tratadas como custo zero. Os valores conhecidos podem ser exibidos como subtotal, acompanhados por indicador de incompletude.

### Rascunhos schema 1

Como não há registros financeiros válidos a preservar e o payload antigo é semanticamente ambíguo, a V13 removerá somente rascunhos de `OBLIGATION`. Não haverá conversão schema 1 → 2.

## Testes obrigatórios

- migração completa até V13 sem versões duplicadas;
- rascunho único com tela de decisão, descarte e `fresh=true`;
- obrigação iniciada a partir de plano e restauração do `acquisitionPlanId`;
- restauração canônica de `0.005` e valores monetários canônicos;
- dois pagamentos sem referência;
- referência duplicada tratada no formulário;
- divisão de R$ 1,00 em 18 parcelas sem parcela zero;
- soma exata das parcelas sem juros;
- detalhe do cronograma sem status enganoso;
- resumo de plano completo e parcial;
- JavaScript completo, Maven/JUnit/Testcontainers, pacote e smoke operacional da CI.

## Critério de conclusão

O PR #15 deve ficar atualizado com a `main`, sem conflitos, com migrations coerentes, testes completos verdes e ainda em rascunho, aguardando revisão e merge manual do mantenedor.
