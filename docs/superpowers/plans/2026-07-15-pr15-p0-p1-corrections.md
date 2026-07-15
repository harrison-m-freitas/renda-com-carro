# PR #15 P0 and P1 Corrections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tornar o PR #15 integrável com a `main` atual e corrigir os defeitos financeiros P1 antes do teste manual.

**Architecture:** Mesclar a `main` na branch do PR, resolver conflitos preservando o ciclo de vida atual dos rascunhos e manter os cálculos autoritativos no backend. Cada regressão financeira receberá um teste dedicado antes da implementação mínima.

**Tech Stack:** Java 21, Spring Boot, Thymeleaf, JavaScript ES Modules, PostgreSQL, Flyway, Maven, Docker Compose e GitHub Actions.

## Global Constraints

- Não realizar merge ou auto-merge na `main`.
- Manter o PR em rascunho.
- Usar TDD para P0 e P1.
- Não preservar registros financeiros ou rascunhos antigos incompatíveis.
- Executar a CI completa antes de declarar conclusão.

---

### Task 1: Integrar a main e migrations

**Files:**
- Rename: `src/main/resources/db/migration/V11__redesign_financial_obligations.sql` para a próxima versão livre.
- Modify: conflitos em rascunhos, controlador, templates e testes.

- [ ] Mesclar `main` na branch do PR.
- [ ] Preservar V11 de fuso horário e V12 de rascunho único.
- [ ] Renomear a migration financeira para V13 e adicionar remoção explícita de rascunhos de obrigação antigos.
- [ ] Executar testes de inicialização/Flyway.
- [ ] Commitar a integração.

### Task 2: Integrar o rascunho único ao novo formulário

**Files:**
- Modify: `FinancialObligationController.java`, `ObligationFormSubmissionService.java`, `templates/obligations/form.html`, `templates/obligations/list.html`.
- Test: `ObligationWebTest.java`, testes de draft.

- [ ] Escrever testes falhando para decisão continuar/descartar e contexto de plano.
- [ ] Preservar `findLatestActive`, `fresh=true`, descarte e validação do `draftKey`.
- [ ] Preservar `acquisitionPlanId` e redirecionamento ao plano.
- [ ] Executar testes focados e commit.

### Task 3: Corrigir restauração numérica

**Files:**
- Modify: `financial-input-formatters.js`, `financial-inputs.js`.
- Test: `financial-input-formatters.test.mjs`, `financial-inputs.test.mjs`.

- [ ] Escrever regressão para `0.005` restaurado como percentual canônico.
- [ ] Separar parsing canônico de entrada brasileira.
- [ ] Garantir dinheiro com duas casas e percentual com até quatro.
- [ ] Executar testes JS focados e commit.

### Task 4: Normalizar referência de pagamento

**Files:**
- Modify: `PaymentForm.java`, `FinancialObligationService.java` e controlador de pagamento.
- Test: `FinancialObligationFlowTest.java` e teste web.

- [ ] Escrever testes para dois pagamentos sem referência e duplicidade explícita.
- [ ] Aplicar trim, vazio para null e limite 120.
- [ ] Converter duplicidade em erro de formulário.
- [ ] Executar testes focados e commit.

### Task 5: Distribuir centavos sem parcelas zero

**Files:**
- Modify: `InstallmentScheduleCalculator.java` e preview JS.
- Test: calculador Java e JS.

- [ ] Escrever regressão para R$ 1,00 em 18 parcelas.
- [ ] Distribuir centavos exatamente entre as parcelas.
- [ ] Garantir soma igual ao principal e nenhuma parcela zero.
- [ ] Executar testes focados e commit.

### Task 6: Corrigir estados apresentados

**Files:**
- Modify: `AcquisitionPlanService.java`, `templates/acquisition-plans/detail.html`, `templates/obligations/detail.html`.
- Test: `AcquisitionPlanFlowTest.java`, testes web.

- [ ] Escrever teste para total parcial com obrigação desconhecida.
- [ ] Representar agregado parcial sem assumir custo zero.
- [ ] Remover status de parcela não conciliado da interface.
- [ ] Executar testes focados e commit.

### Task 7: Verificação final

- [ ] Executar todos os testes JavaScript.
- [ ] Executar Maven/JUnit/Testcontainers.
- [ ] Executar package e verificações de credenciais.
- [ ] Executar smoke operacional, ARM64, backup e restauração.
- [ ] Atualizar o corpo do PR com o novo head e validações.
- [ ] Confirmar PR aberto, draft, mergeável e não mesclado.
