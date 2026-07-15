# Formulário de gasto: UX, acessibilidade, automações e rateio assistido

Data: 2026-07-14  
Status: aprovado em conversa, aguardando revisão do documento  
Escopo principal: cadastro de novo gasto  
Escopo transversal: preferência de fuso horário do usuário

## 1. Objetivo

Revisar o formulário `Novo gasto` para torná-lo mais claro, seguro e rápido no computador e no celular, sem enfraquecer as validações do domínio nem substituir cálculos autoritativos do servidor por valores derivados no navegador.

A solução deve:

- reduzir conceitos contábeis expostos sem necessidade;
- apresentar claramente o efeito profissional e pessoal do gasto;
- evitar combinações contraditórias de classificação e rateio;
- oferecer entrada localizada adequada ao padrão brasileiro;
- usar a data civil do fuso escolhido pelo usuário, e não a data do servidor;
- explicar o rateio por quilometragem com dados confirmados ou estimados já disponíveis no sistema;
- preservar rascunhos e valores digitados ao alternar opções;
- funcionar com teclado, leitor de tela, toque e diferentes larguras de tela;
- manter o backend como fonte de verdade.

## 2. Estratégia aprovada

Será usada divulgação progressiva, mantendo as três etapas atuais:

1. Dados do gasto;
2. Classificação, pagamento e rateio;
3. Revisão e observações.

O formulário continuará guiado no celular e integralmente visível no desktop. A mudança não transformará o fluxo em um assistente maior nem criará novos passos apenas para campos opcionais.

A implementação será dividida em dois incrementos isoláveis:

1. fundação de fuso horário do usuário;
2. revisão do formulário de gasto.

Essa separação permite validar o comportamento temporal global antes de depender dele no formulário.

## 3. Decisões de produto aprovadas

### 3.1 Estado inicial de um novo gasto

Em um formulário novo, sem rascunho e sem retorno de erro:

- classificação: `PROFESSIONAL`;
- situação do pagamento: `PAID`;
- data do gasto: data civil do fuso ativo do usuário;
- data do pagamento: igual à data do gasto;
- mês de referência: mês da data do gasto;
- ao selecionar `MIXED`, método de rateio inicial: `MILEAGE_RATIO`.

A precedência de dados será:

1. valores devolvidos após erro de validação;
2. rascunho restaurado;
3. contexto explícito de dia ou turno;
4. padrões de novo formulário.

Nenhum padrão poderá sobrescrever valor restaurado ou já editado.

### 3.2 Situação do pagamento

O formulário apresentará um grupo explícito:

- `Pago`;
- `Pendente`.

`Pago` será a opção inicial.

Quando `Pago` estiver selecionado:

- `paidDate` será visível e obrigatório;
- inicialmente será igual a `expenseDate`;
- acompanhará `expenseDate` enquanto não tiver sido editado manualmente.

Quando `Pendente` estiver selecionado:

- `paidDate` será ocultado e desabilitado;
- o payload enviado ao rascunho e à submissão final não conterá uma data de pagamento efetiva;
- o último valor digitado poderá ser preservado apenas no estado transitório do navegador para eventual retorno a `Pago`.

Ao voltar para `Pago`:

- se existia uma data alterada manualmente, ela será restaurada;
- caso contrário, a data voltará a acompanhar `expenseDate`.

A interface usará uma propriedade explícita de apresentação, como `paymentStatus`, sem exigir nova coluna em `expense`, pois o estado persistido continuará derivável de `paidDate`.

### 3.3 Data do gasto e mês de referência

`expenseDate` não será inicializado com `LocalDate.now()` diretamente dentro de `ExpenseForm`, pois isso usa o relógio e o fuso do processo servidor.

O valor inicial será produzido a partir do fuso ativo do usuário:

- no servidor, quando houver preferência salva;
- no navegador, para confirmar ou corrigir o valor antes de qualquer edição, especialmente no primeiro acesso sem preferência salva.

O código do navegador não usará `toISOString().slice(0, 10)`, pois essa conversão passa por UTC. A data civil será montada ou formatada no fuso IANA ativo.

Enquanto o usuário não editar manualmente os campos dependentes:

```text
expenseDate mudou
├── paidDate acompanha, se o status for Pago
└── competenceMonth acompanha
```

Depois da primeira edição manual de `paidDate` ou `competenceMonth`, o respectivo campo deixa de acompanhar automaticamente a data do gasto.

### 3.4 Mês de referência

O rótulo visível será `Mês de referência`, não `Competência`.

O campo `type="month"` ficará dentro de `Mais opções`, usando um componente acessível, preferencialmente `<details>` e `<summary>`.

A linha resumida ficará sempre visível:

> Referência: Julho de 2026

A apresentação usará `pt-BR` e capitalizará somente a primeira letra do mês. Não será aplicado `text-transform: capitalize` indiscriminadamente.

O campo continua editável para lançamentos em mês diferente da data do gasto.

### 3.5 Classificação em cartões

O `<select>` de classificação será substituído por três cartões selecionáveis:

- `Profissional` — todo o valor pertence à operação;
- `Pessoal` — nenhum valor reduz o resultado profissional;
- `Misto` — parte profissional e parte pessoal.

Os cartões serão apenas a apresentação visual de `radio buttons` nativos dentro de `fieldset` e `legend`.

Requisitos:

- desktop: três colunas quando houver largura suficiente;
- celular: cartões empilhados;
- toda a área do cartão será clicável;
- o radio não será removido da árvore de acessibilidade;
- foco por teclado será claramente visível;
- setas continuarão funcionando como navegação nativa do grupo;
- estado selecionado não dependerá somente de cor.

`Profissional` virá selecionado em um novo formulário. A escolha de categoria não mudará a classificação automaticamente.

### 3.6 Métodos de rateio do gasto misto

Ao selecionar `Misto`, será exibido um segundo grupo de `radio buttons` com:

1. pela quilometragem do mês;
2. por percentual profissional;
3. por valor profissional fixo.

`Pela quilometragem do mês` virá selecionado inicialmente.

Ao mudar de classificação ou método:

- campos incompatíveis serão ocultados e desabilitados;
- valores digitados serão preservados no estado transitório da página;
- campos incompatíveis serão removidos do payload de rascunho e da submissão;
- o backend continuará eliminando ou ignorando dados incompatíveis como defesa adicional.

A interface não apagará imediatamente percentual, valor fixo ou justificativa apenas porque o usuário tocou em outra opção por engano.

### 3.7 Percentual profissional natural

O percentual manual usará entrada natural:

```text
75   → 75%
75,5 → 75,50%
```

Será aceito ponto ou vírgula na colagem, com apresentação brasileira por vírgula.

Para um gasto `MIXED`, o valor válido será estritamente:

```text
0 < percentual profissional < 100
```

Erros específicos:

- `0%`: `Para 0%, classifique o gasto como Pessoal.`
- `100%`: `Para 100%, classifique o gasto como Profissional.`
- fora da faixa: `Informe um percentual maior que 0% e menor que 100%.`

O navegador não converterá automaticamente a classificação e não limitará silenciosamente o valor.

A implementação poderá adicionar um modo declarativo de percentual natural ao módulo compartilhado de inputs localizados, sem alterar o comportamento dos percentuais já aprovados em outros formulários.

### 3.8 Valor profissional fixo

Para um gasto `MIXED`, o valor válido será estritamente:

```text
R$ 0,00 < valor profissional < valor total do gasto
```

Erros específicos:

- zero: `Para nenhum valor profissional, classifique o gasto como Pessoal.`
- igual ao total: `Para atribuir todo o valor à operação, classifique o gasto como Profissional.`
- maior que o total: `O valor profissional deve ser menor que o valor total do gasto.`

A prévia não corrigirá silenciosamente valores inválidos. Enquanto houver inconsistência, o resumo exibirá o problema e a etapa não poderá avançar.

### 3.9 Justificativa manual

A justificativa continuará obrigatória apenas para:

- percentual manual;
- valor profissional fixo.

Ela permanecerá desnecessária para rateio por quilometragem.

A validação deve existir no navegador, no contrato de rascunho e no backend. Textos curtos podem ter espaços externos e sequências de espaços normalizados; observações livres continuarão preservando quebras de linha.

## 4. Rateio por quilometragem

### 4.1 Reutilização da estimativa existente

O sistema já possui `MonthlyMileageInferenceService`, que produz `MonthlyMileagePreview` com:

- odômetro inicial e final inferidos;
- quilômetros totais;
- quilômetros profissionais e pessoais;
- percentual profissional;
- origens das leituras;
- quantidade de dias, turnos e abastecimentos;
- alertas e bloqueios de qualidade dos dados.

Nenhum segundo algoritmo de inferência será criado para o formulário.

### 4.2 Serviço de prévia do rateio

Será criado um serviço de apresentação, por exemplo `ExpenseAllocationPreviewService`, com a seguinte decisão:

```text
procurar fechamento confirmado
├── encontrado → CONFIRMED
└── não encontrado
    └── usar MonthlyMileageInferenceService.infer(...)
        ├── dados utilizáveis → ESTIMATED
        └── dados insuficientes ou inconsistentes → INSUFFICIENT_DATA
```

O serviço não persistirá fechamento e não alterará o gasto. Ele apenas prepara uma prévia explicável para a interface.

### 4.3 Endpoint de leitura

Será exposto um endpoint autenticado e somente de leitura:

```http
GET /expenses/allocation-preview?vehicleId=<uuid>&competenceMonth=2026-07
```

Estados de resposta:

#### `CONFIRMED`

Há fechamento mensal persistido. A resposta contém valores confirmados e `provisional: false`.

#### `ESTIMATED`

Não há fechamento, mas a inferência atual possui dados suficientes para apresentar uma estimativa. A resposta contém alertas e `provisional: true`.

#### `INSUFFICIENT_DATA`

Não há dados suficientes ou confiáveis para produzir uma proporção útil. A resposta explica o motivo e não inventa percentual.

Estrutura conceitual:

```json
{
  "status": "ESTIMATED",
  "referenceMonth": "2026-07",
  "totalKilometers": "620.0",
  "professionalKilometers": "430.0",
  "professionalPercentage": "69.3500",
  "provisional": true,
  "alerts": [
    {
      "code": "OPEN_SHIFT",
      "message": "Existe turno em andamento no mês.",
      "blocking": true
    }
  ]
}
```

O contrato final poderá separar `warnings` e `blockingAlerts`, mas deve preservar códigos estáveis e mensagens em português.

### 4.4 Apresentação na interface

Quando houver fechamento confirmado:

> Rateio confirmado de Julho de 2026  
> 725,4 km profissionais de 1.000,5 km totais.  
> 72,50% deste gasto será atribuído à operação.

Quando houver estimativa:

> Estimativa de Julho de 2026  
> Com os registros existentes até agora, foram identificados 430,0 km profissionais de 620,0 km totais.  
> Percentual profissional estimado: 69,35%.

Também será mostrado:

> Esta proporção ainda pode mudar porque o mês não foi fechado.

Quando os dados forem insuficientes:

> Ainda não existem leituras suficientes para estimar o rateio de Julho de 2026. O valor definitivo será conhecido no fechamento mensal.

A prévia do formulário não alterará a regra persistida. O gasto continuará armazenando `MILEAGE_RATIO`, nunca uma cópia do percentual estimado. Alterações posteriores nos dados ou no fechamento poderão recalcular o resultado com a proporção vigente.

A política contábil atual do dashboard para meses sem fechamento não será alterada implicitamente por este trabalho. A interface deverá distinguir claramente:

- estimativa exibida para orientação;
- proporção confirmada após fechamento;
- regra provisória eventualmente usada em relatórios ainda não fechados.

Qualquer mudança na política contábil do dashboard exigirá decisão e teste próprios.

### 4.5 Concorrência das consultas

A consulta será refeita quando mudar:

- veículo;
- mês de referência;
- classificação para `MIXED`;
- método para `MILEAGE_RATIO`.

O script usará `AbortController` ou token incremental para impedir que resposta antiga substitua a seleção mais recente.

Falha na consulta não bloqueará o salvamento do gasto. O backend continuará validando o método e calculando resultados em seus fluxos autoritativos.

## 5. Fuso horário do usuário

### 5.1 Persistência

`AppUser` receberá uma preferência opcional de fuso IANA, por exemplo:

```text
time_zone_id = America/Sao_Paulo
```

Será criada uma migração Flyway para a nova coluna. O valor será validado com `ZoneId.of(...)` antes de ser salvo.

Offsets fixos como `-03:00` não serão usados como preferência principal.

### 5.2 Detecção inicial

O navegador detectará o fuso com:

```javascript
Intl.DateTimeFormat().resolvedOptions().timeZone
```

Quando a conta ainda não possuir preferência:

- o fuso detectado será usado como fuso ativo;
- será salvo como preferência sem interromper o preenchimento;
- falha ao salvar a preferência não impedirá o uso do formulário.

Quando não houver suporte ou a detecção for inválida, será usado o fuso salvo; na ausência dele, o fuso padrão configurado na aplicação será o fallback.

### 5.3 Divergência entre dispositivo e conta

Quando o fuso detectado for diferente do fuso salvo, a aplicação não atualizará a conta silenciosamente.

Será exibido um banner global após autenticação:

> Fuso horário diferente detectado  
> Este dispositivo está usando `America/New_York`, mas sua conta está configurada como `America/Sao_Paulo`.

Ações:

- `Usar fuso deste dispositivo`;
- `Manter fuso configurado`.

Ao usar o fuso do dispositivo:

- a preferência é atualizada no servidor;
- o fuso ativo da página muda;
- a aplicação emite um evento global de mudança;
- datas padrão ainda não editadas são recalculadas.

Ao manter o fuso configurado:

- nenhuma preferência é alterada;
- a decisão para o par `fuso salvo + fuso detectado` é lembrada localmente nesse navegador;
- o aviso não reaparece para a mesma combinação;
- o aviso aparece novamente quando o dispositivo detectar outro fuso ou a preferência salva mudar.

### 5.4 Fuso ativo e datas ainda não editadas

Enquanto houver divergência não resolvida, o fuso salvo será o fuso ativo por segurança e previsibilidade.

Após a decisão do usuário, campos padrão ainda intocados poderão ser atualizados. Campos já editados, restaurados de rascunho ou devolvidos após erro nunca serão sobrescritos.

A funcionalidade será global para que gastos, turnos, abastecimentos e futuros relatórios compartilhem a mesma referência temporal.

## 6. Estrutura visual do formulário

### 6.1 Etapa 1 — Dados do gasto

Campos principais:

- veículo;
- categoria;
- valor;
- data do gasto.

Automações:

- listar apenas veículos ativos;
- pré-selecionar o veículo principal ativo quando não houver outro valor;
- iniciar com a data civil do fuso ativo;
- manter máscara monetária brasileira;
- preservar contexto opcional de dia e turno.

### 6.2 Etapa 2 — Como o gasto entra nos resultados

Ordem:

1. cartões de classificação;
2. opções de rateio, somente para `MIXED`;
3. prévia da quilometragem, somente para `MILEAGE_RATIO`;
4. situação do pagamento;
5. data do pagamento, somente para `PAID`;
6. `Mais opções`, com mês de referência.

O título da etapa será alterado de `Classificação e competência` para uma linguagem mais direta, como:

> Como este gasto entra nos resultados?

### 6.3 Etapa 3 — Revisão e observações

O resumo mostrará, quando calculável:

- valor total;
- parte profissional;
- parte pessoal;
- situação do pagamento;
- data de pagamento, quando pago;
- referência formatada;
- critério de rateio;
- estado confirmado ou estimado.

Frase conclusiva:

- profissional: `Este gasto reduzirá o resultado da operação em R$ X.`
- pessoal: `Este gasto não reduzirá o resultado profissional.`
- misto: `R$ X será atribuído à operação e R$ Y à parte pessoal.`

Para quilometragem estimada, os rótulos incluirão `estimada`. Para dados insuficientes, o resumo não apresentará uma falsa divisão numérica.

## 7. Cálculos no navegador

Os cálculos monetários da prévia usarão centavos inteiros ou uma representação decimal segura, evitando multiplicações monetárias baseadas diretamente em ponto flutuante binário.

O script não usará o padrão atual de limitar silenciosamente o resultado com `Math.min` e `Math.max`.

Estados inválidos serão representados explicitamente:

- erro junto ao campo;
- `aria-invalid="true"`;
- resumo indicando que o cálculo depende de correção;
- bloqueio do avanço da etapa.

O backend repetirá todas as validações cruzadas.

## 8. Acessibilidade

### 8.1 Resumo de erros

Quando houver erro do servidor:

- será exibido um resumo no início do formulário;
- o bloco receberá foco programático após o carregamento;
- usará `role="alert"` ou semântica equivalente;
- informará a quantidade de erros;
- cada item será um link para o campo correspondente quando houver campo associado.

### 8.2 Campos inválidos

Campos com erro receberão:

- `aria-invalid="true"`;
- `aria-describedby` apontando para ajuda e erro;
- classe visual compatível com Bootstrap/Tabler;
- mensagem textual, sem depender apenas de cor.

Grupos de `input-group` deverão apresentar borda e foco inválidos de forma coerente, incluindo prefixo `R$` e sufixo `%`.

### 8.3 Conteúdo condicional

Campos condicionais serão removidos da navegação por teclado quando ocultos por meio de `hidden` e `disabled`.

Uma região de status discreta poderá anunciar:

- opções de rateio exibidas;
- consulta de quilometragem em andamento;
- prévia atualizada;
- falha de consulta.

O foco não será movido automaticamente ao simples ato de trocar classificação.

### 8.4 Resumo dinâmico

O bloco visual de revisão não será inteiro uma região `aria-live`, evitando anúncios excessivos a cada tecla.

Será usada uma região `role="status"`, pequena e atômica, atualizada com debounce para anúncios essenciais.

### 8.5 Obrigatoriedade e instruções

A página informará no início que campos com `*` são obrigatórios. O atributo `required` continuará sendo a fonte semântica principal.

## 9. Rascunhos

O rascunho de gasto passará do schema 1 para o schema 2.

Novos campos permitidos incluem:

- `paymentStatus`;
- `operationalDayId`, quando houver contexto;
- `shiftId`, quando houver contexto.

A migração lógica do schema 1 para o schema 2 será:

```text
paidDate preenchido → paymentStatus = PAID
paidDate ausente    → paymentStatus = PENDING
```

Valores derivados nunca serão aceitos como fonte confiável no rascunho:

- parte profissional calculada;
- parte pessoal calculada;
- percentual inferido de quilometragem;
- texto do mês formatado;
- estado de prévia.

Ao restaurar, a aplicação recalculará tudo a partir dos campos editáveis e dos dados atuais do servidor.

O botão `Voltar aos gastos` não descartará automaticamente o rascunho. Uma ação separada `Descartar rascunho` exigirá confirmação explícita.

## 10. Regras do backend

Na criação do gasto, o servidor deverá validar:

- veículo existente e ativo;
- categoria existente e ativa;
- dia operacional pertencente ao veículo;
- turno pertencente ao dia informado;
- `PAID` exige data de pagamento;
- `PENDING` persiste `paidDate = null`;
- `MIXED + MANUAL_PERCENTAGE` exige percentual estritamente entre 0 e 100;
- `MIXED + FIXED_AMOUNT` exige valor estritamente entre zero e o total;
- métodos manuais exigem justificativa;
- campos incompatíveis não influenciam a entidade criada.

Erros previsíveis deverão ser convertidos em erros de campo sempre que possível, em vez de aparecerem somente como erro global.

## 11. Arquivos principais afetados

### 11.1 Formulário de gasto

- `src/main/resources/templates/expenses/form.html`
- `src/main/resources/static/js/expense-form.js`
- novo módulo puro, por exemplo `src/main/resources/static/js/expense-form-state.js`
- `src/main/resources/static/css/app.css`
- `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseForm.java`
- `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java`
- `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseFormSubmissionService.java`
- `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseService.java`
- `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ExpenseDraftDefinition.java`
- novo serviço e DTO de prévia de rateio em `expense/application` ou `expense/web`;
- `src/main/java/dev/harrison/rendacomcarro/expense/infrastructure/MonthlyOdometerClosingRepository.java`
- reutilização de `MonthlyMileageInferenceService.java` e `MonthlyMileagePreview.java`.

### 11.2 Fuso horário

- `src/main/java/dev/harrison/rendacomcarro/security/domain/AppUser.java`
- `src/main/java/dev/harrison/rendacomcarro/security/application/CurrentUserService.java`
- novo serviço de preferência de fuso;
- novo endpoint autenticado de leitura e atualização da preferência;
- layout global `src/main/resources/templates/layouts/app.html` ou fragmento equivalente;
- novo módulo JavaScript global de fuso;
- `src/main/resources/db/migration/V11__add_user_time_zone.sql`.

### 11.3 Componentes compartilhados, somente quando justificável

- `src/main/resources/static/js/localized-inputs.js`
- `src/main/resources/static/js/localized-input-formatters.js`
- `src/main/resources/static/js/guided-form.js`
- `src/main/resources/templates/fragments/guided-form.html`

A abstração compartilhada deve ser pequena e baseada em comportamentos já necessários, sem transformar `expense-form.js` em dependência de outros formulários prematuramente.

## 12. Estratégia de testes

### 12.1 JavaScript

Testes do estado puro do formulário:

- profissional atribui 100% ao profissional;
- pessoal atribui 0%;
- percentual natural aceita `75`, `75,5` e colagem com ponto;
- 0% e 100% são rejeitados para misto;
- valor fixo zero, igual ou superior ao total é rejeitado;
- alternar opções preserva valores transitórios;
- payload remove campos incompatíveis;
- data do pagamento acompanha a data do gasto até edição manual;
- mês de referência acompanha a data até edição manual;
- pendente remove data efetiva do payload;
- restauração do rascunho tem precedência sobre padrões;
- respostas antigas da consulta de quilometragem são ignoradas;
- resumo distingue confirmado, estimado e insuficiente;
- valores monetários não sofrem arredondamento binário incorreto;
- submissão duplicada é bloqueada.

Testes do fuso:

- data civil correta próxima à mudança de dia em UTC;
- preferência ausente usa e salva fuso detectado;
- divergência não atualiza sem confirmação;
- manter preferência não reapresenta aviso para a mesma combinação;
- outro fuso detectado reapresenta aviso;
- mudança de fuso atualiza apenas campos ainda intocados.

### 12.2 Backend

Testes web e de aplicação:

- novo formulário usa classificação profissional e pagamento pago;
- data inicial usa preferência de fuso, não o fuso do processo;
- veículo principal ativo é selecionado;
- veículos arquivados não são listados nem aceitos por POST manipulado;
- categorias inativas não são aceitas;
- pagamento pendente limpa a data;
- pagamento pago exige data;
- percentual misto exige faixa aberta `(0, 100)`;
- valor fixo exige faixa aberta `(0, total)`;
- rateio manual exige justificativa;
- endpoint retorna fechamento confirmado quando existente;
- endpoint reutiliza a inferência quando não há fechamento;
- endpoint retorna dados insuficientes sem inventar percentual;
- prévia não persiste nem altera fechamento;
- campos derivados adulterados são ignorados;
- rascunho schema 1 migra para schema 2;
- vínculo com dia e turno continua protegido.

### 12.3 Acessibilidade e contratos de template

Testes de contrato devem verificar:

- `fieldset` e `legend` nos grupos;
- radios reais para classificação, rateio e pagamento;
- labels e descrições associadas;
- resumo de erros focável;
- `aria-invalid` quando houver erro;
- região de status separada do resumo visual;
- campos ocultos também desabilitados;
- ordem de foco e ações mobile.

## 13. Critérios de aceitação

O trabalho estará pronto quando:

1. um gasto novo abrir como profissional, pago e com datas do fuso ativo do usuário;
2. data de pagamento e mês de referência acompanharem a data do gasto até edição manual;
3. divergência de fuso exigir confirmação antes de alterar a preferência;
4. a mesma divergência recusada não gerar avisos repetidos no mesmo navegador;
5. classificação for apresentada em cartões acessíveis;
6. gasto misto iniciar com rateio por quilometragem;
7. a interface mostrar fechamento confirmado, estimativa existente ou dados insuficientes;
8. percentual e valor fixo obedecerem às faixas abertas aprovadas;
9. nenhum valor inválido for corrigido ou escondido silenciosamente na prévia;
10. o resumo apresentar partes profissional e pessoal com estado confirmado ou estimado;
11. rascunhos antigos continuarem recuperáveis;
12. veículos e categorias inativos forem recusados no servidor;
13. teclado e leitor de tela conseguirem completar todo o fluxo;
14. testes JavaScript, Java, MockMvc, Testcontainers e CI completa passarem;
15. nenhuma mudança de política contábil do dashboard ocorrer sem especificação explícita.

## 14. Fora de escopo

- alterar retroativamente gastos já persistidos;
- mudar automaticamente a classificação com base na categoria;
- transformar a estimativa mensal em fechamento automático;
- mudar a política contábil do dashboard para meses não fechados;
- criar novas categorias dentro do formulário;
- redesenhar a listagem de gastos;
- editar gastos existentes, enquanto não houver fluxo de edição aprovado;
- fundir esta implementação automaticamente na branch principal.
