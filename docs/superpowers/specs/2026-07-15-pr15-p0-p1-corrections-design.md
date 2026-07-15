# Correções P0 e P1 do PR #15

## Objetivo

Atualizar `feat/obligation-financing-redesign` com a `main`, preservar o fluxo atual de rascunho único e corrigir os defeitos financeiros P1 encontrados na revisão.

## Integração

- Mesclar `main` na branch do PR, sem merge final na `main`.
- Preservar fuso horário, formulário de despesas e ciclo de vida atual dos rascunhos.
- Renomear a migration financeira para a próxima versão livre após V12.
- Manter um único rascunho de obrigação por usuário, incluindo decisão continuar/descartar e `fresh=true`.
- Preservar `acquisitionPlanId` no rascunho e no retorno após salvar.
- Remover rascunhos antigos de obrigação incompatíveis com schema 2, pois não há registros válidos a preservar.

## Correções financeiras

- Distinguir valores canônicos restaurados de entradas brasileiras; `0.005` deve permanecer `0,005%`.
- Normalizar referência de pagamento com `trim`, vazio para `null` e limite de 120 caracteres.
- Distribuir parcelamentos sem juros em centavos exatos, sem parcela zero.
- Remover status enganoso do cronograma até existir conciliação de pagamento por parcela.
- Representar totais de plano como completos ou parciais; taxa/prazo desconhecidos não equivalem a custo zero.

## Validação

Aplicar TDD para cada regressão, executar testes JavaScript, Maven/JUnit/Testcontainers, empacotamento e smoke operacional. O PR permanece em rascunho e sem merge automático.
