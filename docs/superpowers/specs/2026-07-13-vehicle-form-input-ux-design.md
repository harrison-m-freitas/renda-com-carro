# UX dos campos do formulário de veículo

## Contexto

O formulário de veículo já possui navegação mobile, seções semânticas, validação de servidor e suporte a números em formato brasileiro. O próximo refinamento deve melhorar o preenchimento dos campos, principalmente preço e odômetro, e ajustar as proporções do layout sem substituir a estrutura visual existente.

## Objetivo

Tornar o formulário mais rápido e previsível para preenchimento no desktop e no celular, evitando valores técnicos como `0,0`, campos excessivamente largos e ambiguidades entre placa antiga e Mercosul.

## Escopo

A mudança será restrita ao formulário de criação e edição de veículo. Não será adicionada biblioteca externa de máscaras, novo wizard, novo layout ou alteração nas regras de negócio já consolidadas.

## Comportamento dos campos

### Preço de compra

O preço continuará opcional. Um valor ausente deve permanecer visualmente vazio e ser enviado como vazio, preservando a semântica de preço não informado.

A entrada usará máscara de maquininha/cartão durante toda a edição:

- `1` resulta em `0,01`;
- `12` resulta em `0,12`;
- `123` resulta em `1,23`;
- `2399000` resulta em `23.990,00`;
- apagar todos os dígitos deixa o campo vazio;
- o prefixo `R$` permanece fora do input;
- o cursor permanece no final após a formatação;
- colagens com `R$`, espaços, pontos ou vírgulas são convertidas para a mesma representação monetária;
- o valor apresentado continua compatível com o binder brasileiro do controller.

### Odômetro

O odômetro será formatado progressivamente como distância brasileira:

- `248351` resulta em `248.351`;
- `248351,5` resulta em `248.351,5`;
- uma entrada inteira não recebe `,0` automaticamente;
- somente uma vírgula e uma casa decimal são permitidas;
- letras e símbolos não numéricos são removidos;
- o sufixo `km` permanece fora do input;
- o valor inicial deve aparecer como `0` quando for zero, e não como `0,0`.

### Placa

A máscara deve reconhecer os dois padrões durante a digitação:

- `abc1234` resulta em `ABC-1234`;
- `abc1d23` resulta em `ABC1D23`;
- espaços, hífens digitados e caracteres inválidos são normalizados;
- a entrada é limitada a sete caracteres úteis;
- o hífen é aplicado apenas ao padrão antigo;
- entradas incompletas não recebem mensagens de erro agressivas antes da validação normal do formulário;
- o backend continua armazenando a placa normalizada sem hífen.

### Apelido, marca e modelo

Os campos textuais devem:

- remover espaços no início e no fim ao perder foco e antes do envio;
- reduzir sequências de espaços internos para um único espaço;
- preservar maiúsculas, minúsculas, siglas e hífens;
- não alterar valores como `BMW`, `BYD`, `CR-V` ou `Land Rover`.

## Layout e CSS

A estrutura atual será preservada, com ajustes proporcionais e responsivos.

- O formulário continua centralizado e com largura máxima definida pelo contêiner existente.
- Marca e modelo permanecem em colunas equivalentes.
- Ano e placa ficam lado a lado por padrão, inclusive no celular, com ano ocupando a menor fração e placa o restante.
- A quebra para uma coluna ocorre apenas quando não houver largura utilizável para os dois campos.
- Combustível e odômetro usam proporções relacionadas ao conteúdo; o odômetro pode receber uma fração ligeiramente maior.
- O preço usa uma coluna compacta e expansível, sem ocupar metade de uma tela grande quando isso não for necessário.
- As grades usam `grid`, `minmax()`, `fr`, `clamp()` e media/container queries quando úteis; campos não recebem larguras rígidas em pixels.
- Prefixos e sufixos de `R$` e `km` devem manter altura, alinhamento e estado visual consistentes com o input.
- O estado de foco e erro deve envolver visualmente todo o `input-group`.
- O espaçamento vertical entre cabeçalho da seção, ajuda e campo será reduzido apenas onde houver excesso, mantendo legibilidade.
- A barra de ações continua fixa somente no mobile.

## Validação e estados

- A máscara não substitui a validação HTML nem a validação do servidor.
- O botão entra em `Salvando…` somente após `form.checkValidity()` confirmar que o formulário pode ser enviado.
- Uma submissão inválida não pode deixar o botão permanentemente desabilitado.
- Campos inválidos recebem `aria-invalid="true"` e o primeiro erro recebe foco.
- Ao corrigir um campo, o estado visual inválido é removido quando a restrição correspondente estiver satisfeita.
- O estado do botão deve ser restaurado no evento `pageshow`, cobrindo retorno pelo histórico do navegador.
- O aviso de alterações não salvas permanece ativo e não é disparado durante uma submissão válida.

## Organização do JavaScript

O `vehicle-form.js` continuará sendo o ponto de entrada da tela, mas será dividido em funções pequenas e testáveis:

- normalização e formatação de dinheiro;
- normalização e formatação de odômetro;
- detecção e formatação de placa;
- limpeza de texto;
- sincronização de validação e acessibilidade;
- estado de submissão e navegação.

Não será criada abstração global para toda a aplicação neste momento. As funções devem, porém, evitar dependência desnecessária do DOM para permitir extração futura.

## Testes

### JavaScript

Serão cobertos:

- dinheiro com um, dois e vários dígitos;
- exclusão completa do preço;
- colagem de preço formatado;
- odômetro inteiro e com uma casa decimal;
- rejeição de múltiplas casas decimais;
- placa antiga e Mercosul;
- entradas parciais de placa;
- limpeza de espaços em campos textuais;
- submissão inválida sem bloquear o botão;
- restauração do botão em `pageshow`.

### Web e integração

Serão verificados:

- atributos e classes necessários no HTML;
- persistência do preço transformado pela máscara;
- persistência de odômetro inteiro e decimal;
- criação e edição com placas antiga e Mercosul;
- preço vazio permanecendo `NULL`.

## Critérios de aceitação

A implementação estará pronta quando:

1. preço e odômetro puderem ser preenchidos apenas com o teclado numérico do celular;
2. o preço vazio não reaparecer como zero;
3. o odômetro inteiro não receber decimal artificial;
4. placas antiga e Mercosul forem formatadas corretamente durante a digitação;
5. os campos ocuparem larguras proporcionais ao conteúdo sem quebrar a responsividade;
6. uma validação inválida não bloquear o formulário;
7. todos os testes novos e a suíte existente passarem;
8. a CI operacional concluir sem regressões.