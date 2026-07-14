# Etapa 2 do formulário de gasto: layout compacto inspirado no Shadcn

Data: 2026-07-14  
Status: aprovado em conversa, aguardando revisão do documento  
Escopo: somente composição visual, densidade, hierarquia e responsividade da etapa 2 do formulário `Novo gasto`

## 1. Objetivo

Redesenhar a etapa **Como o gasto entra nos resultados?** para reduzir altura, peso visual e repetição de cartões, sobretudo no desktop, preservando integralmente as regras de negócio, o estado do formulário, o autosave, a acessibilidade e os contratos de backend já implementados no PR #13.

A inspiração no Shadcn será aplicada como linguagem de interface, não como adoção de React, Tailwind ou da biblioteca Shadcn. A implementação continuará em Thymeleaf, Bootstrap/Tabler e CSS próprio.

Princípios visuais:

- controles compactos e alinhados;
- superfícies neutras;
- bordas finas e estados selecionados discretos;
- menor uso de fundos coloridos extensos;
- divulgação progressiva;
- descrição somente onde ajuda a decisão atual;
- hierarquia clara entre decisão principal e configuração secundária.

## 2. Diagnóstico do layout atual

A etapa atual usa o mesmo padrão de cartão grande para três níveis diferentes:

1. classificação do gasto;
2. método de divisão do gasto misto;
3. situação do pagamento.

Essa repetição produz os seguintes problemas:

- a etapa cresce excessivamente no desktop;
- decisões principais e secundárias recebem o mesmo peso visual;
- o fundo azul do item selecionado ocupa áreas grandes demais;
- descrições das três opções permanecem visíveis ao mesmo tempo, mesmo quando apenas uma é relevante;
- o painel de rateio misto parece um segundo formulário dentro da etapa;
- `Mais opções` recebe a mesma largura e presença visual do campo de pagamento;
- a interface fica mais pesada no desktop do que no celular.

## 3. Estratégia aprovada

Será usada uma composição compacta em três blocos:

1. **Classificação** como grupo segmentado horizontal;
2. **Divisão do gasto misto** como subpainel progressivo e discreto;
3. **Pagamento** como escolha binária compacta, seguida dos campos dependentes.

Nenhuma regra de domínio será alterada.

## 4. Classificação do gasto

### 4.1 Composição

No desktop, as opções serão exibidas em uma única linha:

```text
Classificação do gasto *
[ Profissional ] [ Pessoal ] [ Misto ]
```

No celular, o componente poderá:

- manter três colunas quando houver largura suficiente; ou
- usar uma grade responsiva compacta;
- empilhar somente em telas muito estreitas.

Não serão usados três cartões altos com descrições permanentes.

### 4.2 Conteúdo

Cada opção mostrará somente:

- radio nativo;
- título curto.

A descrição da opção ativa aparecerá uma única vez abaixo do grupo:

- Profissional: `Todo o valor será considerado custo da operação.`
- Pessoal: `O valor não reduzirá o resultado profissional.`
- Misto: `Uma parte será profissional e a outra pessoal.`

Essa descrição funcionará como ajuda contextual do grupo, sem duplicar três parágrafos.

### 4.3 Aparência

Estado não selecionado:

- fundo branco ou transparente;
- borda neutra de 1 px;
- raio entre 6 e 8 px;
- altura aproximada de 40 a 44 px;
- tipografia de aproximadamente 14 px;
- sem sombra.

Estado selecionado:

- borda na cor primária;
- fundo primário muito sutil;
- radio ou check visível;
- sem preenchimento azul intenso;
- sem sombra dupla.

Foco por teclado:

- anel de foco externo claramente visível;
- não depender somente de mudança de cor.

## 5. Divisão do gasto misto

### 5.1 Divulgação progressiva

O bloco será renderizado somente quando `Misto` estiver selecionado.

Não haverá um painel externo grande contendo três cartões verticais. O conteúdo será subordinado visualmente à classificação.

### 5.2 Escolha do método

No desktop:

```text
Divisão do gasto
(●) Quilometragem   ( ) Percentual   ( ) Valor
```

No celular, as opções poderão ocupar uma grade ou ser empilhadas, mantendo altura reduzida.

Os rótulos serão curtos:

- Quilometragem;
- Percentual;
- Valor.

A explicação do método ativo aparecerá abaixo, não dentro de todos os itens.

### 5.3 Conteúdo dependente

#### Quilometragem

Mostrar somente o estado atual da consulta:

- confirmado;
- estimado;
- dados insuficientes;
- erro de consulta.

O componente será uma faixa informativa compacta, com:

- título curto;
- mensagem principal;
- aviso adicional apenas quando necessário.

Não usar fundo ciano intenso em toda a largura. Preferir:

- fundo neutro ou azul muito suave;
- ícone discreto;
- borda lateral ou superior sutil;
- padding reduzido.

#### Percentual

Mostrar:

- campo `Percentual profissional`;
- campo `Justificativa`.

#### Valor

Mostrar:

- campo `Valor profissional`;
- campo `Justificativa`.

Somente os controles do método ativo permanecerão visíveis e habilitados.

### 5.4 Preservação de comportamento

Continuam válidas as regras já aprovadas:

- quilometragem é o método inicial ao selecionar `Misto`;
- percentual deve ser maior que 0% e menor que 100%;
- valor profissional deve ser maior que zero e menor que o total;
- justificativa é obrigatória somente nos métodos manuais;
- valores temporários são preservados ao alternar métodos;
- campos incompatíveis não entram no payload;
- o backend permanece autoritativo.

## 6. Situação do pagamento

### 6.1 Componente compacto

A escolha será apresentada como controle binário:

```text
Situação do pagamento *
[ Pago ] [ Pendente ]
```

As descrições longas deixam de permanecer dentro de dois cartões grandes.

Uma ajuda contextual única poderá aparecer abaixo:

- Pago: `O pagamento já foi realizado.`
- Pendente: `O gasto ainda será pago.`

### 6.2 Campos dependentes

Quando `Pago` estiver selecionado:

- exibir `Data do pagamento`;
- manter a sincronização com a data do gasto até edição manual.

Quando `Pendente` estiver selecionado:

- ocultar e desabilitar a data;
- manter o comportamento transitório e de payload já implementado.

## 7. Mês de referência e opções secundárias

`Mais opções` deixará de ocupar uma coluna visualmente equivalente à data de pagamento.

No desktop, a linha poderá ser organizada assim:

```text
Data do pagamento                     Referência: Julho de 2026  [Alterar]
[ 11/07/2026 ]
```

A ação poderá continuar tecnicamente implementada com `<details>` e `<summary>`, mas sua apresentação será compacta e semelhante a um botão ou link secundário.

Requisitos:

- o texto `Referência: Julho de 2026` permanece sempre visível;
- a ação para editar será `Alterar` ou `Mais opções` em estilo discreto;
- o campo `type="month"` aparece somente quando o detalhe for aberto;
- no celular, referência e ação podem ocupar uma linha própria abaixo da data;
- não usar um bloco vazio da mesma altura do campo de data.

## 8. Hierarquia e espaçamento

A etapa terá espaçamento vertical menor e consistente:

- entre título da seção e primeiro grupo: aproximadamente 16 px;
- entre label e controle: 6 a 8 px;
- entre grupos principais: 20 a 24 px;
- dentro do subpainel misto: 12 a 16 px;
- padding dos itens segmentados: aproximadamente 8 a 12 px horizontal e 8 px vertical.

O subpainel misto poderá usar:

- borda neutra fina;
- fundo levemente contrastante;
- raio de 8 px;
- padding entre 12 e 16 px.

Não deverá parecer outro card de página dentro do card da etapa.

## 9. Responsividade

### Desktop

- classificação em uma linha;
- métodos de rateio em uma linha;
- pagamento em controle binário compacto;
- data e referência organizadas sem duas caixas equivalentes;
- alvo: reduzir significativamente a altura da etapa.

### Tablet

- permitir quebra controlada dos controles segmentados;
- evitar textos comprimidos ou cartões muito largos.

### Celular

- manter alvos de toque adequados;
- evitar três cartões altos empilhados;
- permitir grade compacta ou empilhamento curto;
- descrições contextuais continuam abaixo do grupo;
- conteúdo do método misto aparece somente quando necessário.

## 10. Acessibilidade

A compactação não removerá semântica.

Requisitos:

- `fieldset` e `legend` continuam presentes;
- radios nativos continuam na árvore de acessibilidade;
- toda a área do item continua clicável;
- navegação por setas continua nativa;
- `aria-describedby` aponta para a descrição contextual ativa;
- grupos inválidos continuam acessíveis pelo resumo de erros;
- estados selecionados não dependem somente de cor;
- contraste de bordas, texto e foco atende ao padrão do projeto;
- conteúdo progressivo não move o foco automaticamente;
- atualizações de rateio continuam usando região de status adequada.

## 11. Arquivos esperados

Principais alterações:

- `src/main/resources/templates/expenses/form.html`;
- `src/main/resources/static/css/app.css`;
- `src/main/resources/static/js/expense-form.js`, somente se necessário para a descrição contextual ou abertura de opções;
- `src/test/js/expense-form.test.mjs`;
- testes de contrato web do formulário de gasto.

Não são esperadas alterações em:

- banco de dados;
- entidades;
- regras de cálculo;
- endpoint de rateio;
- schema de rascunho;
- serviço de submissão.

## 12. Testes e critérios de aceitação

### Contratos de marcação

- classificação continua sendo um radio group acessível;
- métodos de rateio continuam sendo um radio group acessível;
- pagamento continua sendo um radio group acessível;
- descrições contextuais possuem IDs estáveis;
- campos condicionais permanecem associados aos respectivos erros.

### Comportamento

- alternar classificação atualiza a descrição contextual;
- selecionar `Misto` revela o subpainel compacto;
- sair de `Misto` oculta o subpainel sem perder o estado transitório;
- alternar método revela somente os controles necessários;
- alternar Pago/Pendente mantém a lógica de data atual;
- abrir referência não altera seu valor;
- autosave e restauração continuam funcionando.

### Visual e responsivo

- no desktop, o estado Profissional + Pago não exibe cartões altos;
- no desktop, o estado Misto não contém três cartões verticais dentro de outro painel grande;
- o estado selecionado usa fundo sutil, não uma grande superfície azul;
- a etapa permanece utilizável em 320 px de largura;
- não há overflow horizontal;
- alvos de toque permanecem adequados no celular;
- a etapa 2 fica sensivelmente menor que a versão anterior.

## 13. Resultado esperado

### Profissional ou Pessoal

```text
Classificação do gasto *
[ Profissional ] [ Pessoal ] [ Misto ]
Todo o valor será considerado custo da operação.

Situação do pagamento *
[ Pago ] [ Pendente ]
O pagamento já foi realizado.

Data do pagamento                 Referência: Julho de 2026 [Alterar]
[ 11/07/2026 ]
```

### Misto por quilometragem

```text
Classificação do gasto *
[ Profissional ] [ Pessoal ] [ Misto ]
Uma parte será profissional e a outra pessoal.

Divisão do gasto
[ Quilometragem ] [ Percentual ] [ Valor ]
Ainda não há dados suficientes para estimar o rateio de Julho de 2026.

Situação do pagamento *
[ Pago ] [ Pendente ]
Data do pagamento                 Referência: Julho de 2026 [Alterar]
[ 11/07/2026 ]
```

O resultado deve transmitir a sobriedade e densidade de interfaces inspiradas no Shadcn, preservando a linguagem visual geral do projeto e evitando uma dependência nova de framework ou biblioteca.
