# Redesenho completo de obrigações e planejamento de compra

## Objetivo

Substituir o cadastro técnico de obrigação por uma jornada financeira compreensível para quem conhece o valor emprestado, a quantidade e o valor das parcelas, mas pode não conhecer a taxa de juros. O mesmo conjunto deve representar empréstimos flexíveis, pagamentos únicos e compras compostas por recursos próprios e mais de uma dívida.

## Princípios

- O usuário informa primeiro o que sabe; o sistema calcula o restante.
- Cálculos exibidos no navegador são auxiliares. O backend recalcula e persiste os valores autoritativos.
- Uma obrigação representa uma dívida com um credor.
- Um plano de compra agrupa o preço do bem, recursos próprios e várias obrigações.
- Controles visuais preservam elementos HTML nativos, sem introduzir React ou shadcn/ui.
- Nenhum merge ou auto-merge será realizado.

## Modelo financeiro

### Formas de pagamento

- `FIXED_INSTALLMENTS`: parcelas mensais com cronograma.
- `FLEXIBLE_PAYMENTS`: pagamentos livres orientados por uma meta mensal.
- `SINGLE_PAYMENT`: um único vencimento.

### Bases de cálculo

- `INSTALLMENT_KNOWN`: valor financiado, quantidade e parcela conhecidos; taxa inferida.
- `RATE_KNOWN`: valor financiado, quantidade e taxa conhecida; parcela calculada.
- `INTEREST_FREE`: divisão sem juros.
- `TOTAL_KNOWN`: valor total conhecido para pagamento único.
- `RATE_UNKNOWN`: dívida flexível cujo custo financeiro ainda não foi informado.

### Taxas

Taxas informadas podem ser mensais ou anuais efetivas. O domínio persiste taxa mensal e taxa anual efetiva normalizadas. Quando a parcela é conhecida, a taxa mensal é resolvida por busca binária sobre a fórmula Price.

### Cronograma

O backend gera cada parcela com principal, juros, total e vencimento. A última parcela absorve diferenças de arredondamento. Parcelas incompatíveis com o principal e o prazo são rejeitadas com erro associado ao campo da parcela.

## Plano de compra

Um `AcquisitionPlan` possui:

- título;
- veículo opcional;
- preço de compra;
- recursos próprios;
- data da compra;
- observações.

Obrigações podem pertencer a um plano. O detalhe apresenta preço, recursos próprios, principal das dívidas, valor ainda não coberto, total planejado de pagamentos e custo financeiro. O plano permite adicionar sucessivas fontes de financiamento.

Exemplo:

- preço do veículo: R$ 45.000;
- recursos próprios: R$ 0;
- empréstimo da mãe: R$ 10.000 sem juros;
- financiamento bancário: R$ 35.000 em 36 parcelas de R$ 1.386;
- cobertura total: R$ 45.000.

## Jornada de nova obrigação

### Etapa 1 — Identificação

Cartões de escolha para o tipo da dívida, campo contextual do credor e vínculo opcional com veículo ou plano de compra.

### Etapa 2 — Valor recebido

Valor emprestado ou financiado e data do contrato. A linguagem “principal” não aparece na interface.

### Etapa 3 — Forma de pagamento

Cartões para parcelas fixas, pagamentos livres e pagamento único. Os campos condicionais são preservados ao alternar opções e somente o conjunto ativo é enviado.

Para parcelas fixas, o usuário escolhe:

- conheço o valor da parcela;
- conheço a taxa;
- não há juros.

### Etapa 4 — Revisão

Resumo com parcela, total estimado, custo financeiro, taxas mensal/anual, primeiro e último vencimento, meta mensal ou pagamento único. Observações ficam junto à revisão.

## Máscaras

Campos financeiros usam entrada natural:

- `1000` representa mil reais, não dez reais;
- `1000,5` representa R$ 1.000,50;
- colagens com `R$`, ponto ou vírgula são aceitas;
- agrupamento e duas casas são aplicados no `blur`;
- percentuais aceitam `12` como 12%;
- o backend continua aceitando valores brasileiros e canônicos.

## Acessibilidade

- `fieldset` e `legend` para grupos de escolhas;
- radios reais apresentados como cartões;
- foco visível e alvo mínimo de 44 px;
- `aria-invalid` e `aria-describedby` nos campos;
- resumo de erros com links para os controles;
- resumo calculado com `aria-live="polite"` e atualização após debounce;
- etapas com lista ordenada e `aria-current="step"` no celular;
- foco no primeiro erro após submissão inválida;
- progressão funcional sem JavaScript.

## Automação da interface

- preencher a data atual;
- sugerir primeiro vencimento aproximadamente 30 dias depois;
- selecionar o veículo do plano de compra;
- preencher o valor restante do plano somente por ação explícita;
- inferir taxa quando parcela é conhecida;
- calcular parcela quando taxa é conhecida;
- calcular total, custo e último vencimento;
- estimar duração do pagamento flexível;
- alertar quando a meta mensal não amortiza a dívida;
- preservar valores ao alternar modos;
- limpar somente campos inativos do payload de rascunho e da submissão;
- bloquear duplo envio e anunciar “Salvando…”.

## Migração

Como não existem registros financeiros válidos, a migração é deliberadamente destrutiva para as tabelas de obrigações, parcelas e pagamentos. Ela recria o esquema coerente e adiciona o plano de compra. As demais áreas da aplicação não são apagadas.

## Não objetivos

- importação de contratos bancários;
- cálculo de CET com IOF, seguros e tarifas discriminadas;
- amortização SAC;
- atualização automática do saldo por juros entre pagamentos;
- edição de obrigações já criadas nesta primeira entrega.
