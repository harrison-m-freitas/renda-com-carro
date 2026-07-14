# Fluxo guiado móvel do formulário de veículo

## Contexto

O formulário de criação e edição de veículo já possui seções semânticas, campos localizados, validação HTML e de servidor, estado de envio e barra de ações fixa no celular. Apesar disso, todos os campos continuam visíveis ao mesmo tempo e possuem peso visual semelhante. No celular, o resultado é uma tela administrativa longa, com pouca orientação sobre a ordem de preenchimento.

A aplicação já possui um mecanismo compartilhado de formulários guiados com rascunhos sincronizados. O formulário de veículo, porém, é curto e não deve participar desse mecanismo. Ele receberá somente uma navegação móvel própria, sem autosave, recuperação de rascunho ou controle de versão.

## Objetivo

Tornar o cadastro e a edição de veículo mais fáceis no celular por meio de duas etapas curtas, mantendo o formulário completo e direto no desktop.

A mudança deve:

- priorizar os dados essenciais antes dos opcionais;
- validar a etapa atual antes de avançar;
- abrir automaticamente a etapa que contém erro de servidor;
- preservar o funcionamento sem JavaScript;
- melhorar placa, cancelamento, foco, acessibilidade e prevenção de envio duplicado;
- permanecer isolada do sistema compartilhado de formulários guiados e rascunhos.

## Decisões aprovadas

- O fluxo guiado será ativado somente abaixo de `768px`.
- O desktop continuará exibindo todas as seções em uma página.
- O celular terá duas etapas.
- A primeira etapa será validada antes de avançar.
- Erros de servidor abrirão a primeira etapa inválida.
- O preço de compra ficará recolhido no celular em “Informações de aquisição”.
- A segunda etapa não terá bloco de revisão da identificação.
- O progresso será textual, com “Etapa 1 de 2” ou “Etapa 2 de 2”, acompanhado por uma barra linear.
- O apelido ficará depois dos dados essenciais e receberá apenas uma sugestão textual, sem preenchimento automático.
- A regra atual de ano, de `1980` a `2100`, será preservada nesta entrega.
- O cancelamento com alterações reais exigirá confirmação.

## Escopo

### Incluído

- criação e edição de veículo;
- reorganização da ordem dos campos;
- fluxo móvel em duas etapas;
- progresso móvel;
- aquisição recolhível no celular;
- validação por etapa;
- abertura automática da etapa com erro;
- ajuda dinâmica do nome para identificação;
- formatação de placa com preservação do cursor;
- detecção real de alterações não salvas;
- confirmação ao cancelar;
- estados de foco, erro e salvamento;
- testes JavaScript, MockMvc e contrato web.

### Não incluído

- mudança de entidade, banco ou migration;
- alteração da regra de ano;
- consulta externa de marcas ou modelos;
- preenchimento automático do apelido;
- autosave ou rascunho de veículo;
- integração com `data-guided-form`;
- terceira etapa de revisão;
- mudança das regras de preço, combustível, odômetro ou placa no backend.

## Experiência no desktop

A partir de `768px`, todas as seções permanecem visíveis no mesmo cartão.

A ordem será:

1. **Identificação**
   - Marca
   - Modelo
   - Ano
   - Placa
   - Nome para identificação, opcional
2. **Dados de operação**
   - Combustível
   - Odômetro inicial ou atual
3. **Aquisição**
   - Preço de compra, opcional

O progresso, os botões “Continuar” e “Voltar” e o comportamento recolhível da aquisição não serão exibidos no desktop. O rodapé continuará contendo “Cancelar” e o botão final de envio.

## Experiência no celular

### Progresso

Acima do conteúdo da etapa será exibido um indicador compacto:

```text
Etapa 1 de 2                  Identificação
[████████░░░░░░░░]
```

ou:

```text
Etapa 2 de 2                       Operação
[████████████████]
```

O progresso é informativo e não funciona como navegação clicável. A navegação ocorre somente pelos botões do rodapé.

### Etapa 1 — Identificação

A ordem dos campos será:

1. Marca;
2. Modelo;
3. Ano e placa lado a lado quando houver largura suficiente;
4. Nome para identificação, opcional.

O rodapé exibirá:

- **Cancelar**;
- **Continuar**.

Ao tocar em “Continuar”, somente os controles da primeira etapa serão validados. Se houver erro, a etapa permanece aberta, o primeiro campo inválido recebe foco e uma mensagem curta informa: “Revise os campos destacados para continuar.”

### Etapa 2 — Operação

A segunda etapa conterá:

1. Combustível;
2. Odômetro inicial ou atual;
3. “Informações de aquisição”, recolhida por padrão quando não houver valor nem erro;
4. Preço de compra dentro da região expansível.

O rodapé exibirá:

- **Voltar**;
- **Cadastrar veículo** ou **Salvar alterações**.

Não haverá resumo dos dados da primeira etapa.

### Aquisição recolhível

O controle será um botão real, com área de toque mínima de 44 px, `aria-expanded` e `aria-controls`.

A aquisição será aberta automaticamente quando:

- o preço já estiver preenchido na carga inicial;
- o preço tiver erro devolvido pelo servidor;
- o usuário a tiver expandido na sessão atual.

Ao alternar entre as etapas, o estado de expansão será preservado. No desktop, a aquisição sempre permanecerá visível.

## Melhoria progressiva e responsividade

O HTML será funcional antes da inicialização do JavaScript:

- todas as seções estarão visíveis;
- o botão final de envio estará disponível;
- as validações HTML e de servidor continuarão operando;
- os controles exclusivamente móveis começarão ocultos.

Após a inicialização, o script adicionará uma classe de melhoria, como `vehicle-form--enhanced`, e aplicará o fluxo apenas quando `matchMedia('(max-width: 767.98px)')` corresponder.

O estado da etapa atual será mantido em memória. Ao redimensionar:

- entrando no desktop, todas as seções ficam visíveis e os controles móveis somem;
- retornando ao celular, a etapa anteriormente selecionada volta a ser aplicada;
- nenhum valor de campo é recriado ou perdido.

## Estrutura de marcação

O formulário utilizará atributos próprios e não receberá `data-guided-form`.

Estrutura conceitual:

```html
<form id="vehicleForm" data-vehicle-flow>
  <div data-vehicle-progress hidden>...</div>

  <section data-vehicle-step="identification">...</section>

  <div data-vehicle-step="operation">
    <section>Dados de operação</section>
    <section data-vehicle-acquisition>...</section>
  </div>

  <footer>
    <a data-vehicle-cancel>Cancelar</a>
    <button type="button" data-vehicle-previous hidden>Voltar</button>
    <button type="button" data-vehicle-next hidden>Continuar</button>
    <button type="submit" data-vehicle-submit>...</button>
  </footer>
</form>
```

Os atributos são contratos de comportamento e de teste. Classes continuam responsáveis apenas pela apresentação.

## Entrada inicial e erros de servidor

Na inicialização móvel, o script procurará controles com `.is-invalid`.

- Se não houver erro, a etapa inicial será “Identificação”.
- Se houver erro, será aberta a primeira etapa que contiver um controle inválido.
- Se o erro estiver no preço, a etapa “Operação” será aberta e a aquisição será expandida.

A busca respeitará a ordem do DOM, garantindo comportamento determinístico quando existirem erros em mais de uma etapa.

## Validação e navegação

### Validação da primeira etapa

A validação parcial não chamará `form.checkValidity()`, pois isso também validaria campos ocultos da segunda etapa. O controlador obterá os controles elegíveis dentro da etapa atual e chamará `checkValidity()` ou `reportValidity()` de forma controlada.

O fluxo deverá:

1. sincronizar `aria-invalid` e o estado visual de cada controle inválido;
2. permanecer na etapa atual quando houver erro;
3. exibir o aviso da etapa;
4. focar o primeiro controle inválido em `requestAnimationFrame`;
5. posicioná-lo em área visível com rolagem compatível com o cabeçalho e o rodapé fixos.

### Envio final

Antes do envio, os campos localizados serão formatados no modo final, como ocorre atualmente.

Se a validação completa falhar:

- o envio será impedido;
- o botão será restaurado;
- o fluxo abrirá a primeira etapa inválida;
- a aquisição será aberta se necessário;
- o primeiro erro receberá foco.

Se a validação for válida:

- o estado de submissão será marcado;
- a detecção de alterações será suspensa;
- o botão final será desabilitado;
- o texto mudará para “Salvando…”;
- um indicador discreto de carregamento acompanhará o texto sem alterar a largura de forma abrupta.

O evento `pageshow` continuará restaurando o botão e o estado de submissão quando a página voltar pelo cache do navegador.

## Nome para identificação

O campo deixará de ser o primeiro da tela e passará a ser apresentado depois de marca, modelo, ano e placa.

O valor não será preenchido pelo JavaScript. A ajuda será atualizada conforme marca e modelo:

```text
Se ficar vazio, o veículo será exibido como Renault Sandero.
```

Regras:

- usar somente valores não vazios após remoção de espaços externos;
- quando apenas marca ou modelo estiver preenchido, usar somente o valor disponível;
- quando ambos estiverem vazios, manter uma ajuda genérica;
- nunca substituir texto digitado no campo de nome;
- não emitir eventos sintéticos de `input` ao atualizar a ajuda.

## Placa e preservação do cursor

A regra de apresentação continuará aceitando placa antiga e Mercosul:

- `abc1234` → `ABC-1234`;
- `abc1d23` → `ABC1D23`;
- espaços, hífens e caracteres inválidos serão normalizados;
- serão mantidos no máximo sete caracteres úteis.

A melhoria principal será preservar a intenção de edição. O cursor não será enviado ao final após cada entrada.

A lógica pura de placa deverá receber o valor e a seleção atual e retornar:

- valor formatado;
- nova posição inicial da seleção;
- nova posição final da seleção.

A posição será calculada pela quantidade de caracteres úteis existentes antes de cada limite da seleção, considerando o hífen inserido automaticamente no padrão antigo. Isso cobrirá digitação, colagem, backspace, seleção parcial e correção no meio do valor.

A máscara não substituirá as validações HTML ou de servidor e não exibirá erro agressivo durante uma entrada ainda parcial. O campo receberá uma restrição HTML equivalente aos formatos aceitos pelo backend, incluindo a apresentação com hífen da placa antiga. Ao tentar avançar ou enviar, uma entrada incompleta ou incompatível exibirá a mensagem específica: “Informe uma placa no formato ABC-1234 ou ABC1D23”.

## Alterações não salvas e cancelamento

A variável booleana simples de formulário alterado será substituída por comparação com um estado inicial normalizado.

O estado inicial será capturado depois que as máscaras tiverem formatado os valores carregados. A comparação abrangerá controles de formulário relevantes, preservando a distinção entre valores vazios e preenchidos.

Não haverá aviso quando:

- o usuário modificar e depois restaurar os valores originais;
- somente a máscara alterar a apresentação inicial;
- a interface trocar de etapa ou expandir a aquisição;
- a ajuda do nome for atualizada;
- o formulário estiver em submissão válida.

Haverá aviso de saída do navegador quando existirem alterações reais.

Ao clicar em “Cancelar”:

- sem alterações, a navegação ocorre imediatamente;
- com alterações, será usado um diálogo nativo de confirmação com a mensagem “Descartar as alterações deste veículo?”;
- confirmando, a proteção de saída será desativada antes da navegação;
- recusando, o usuário permanece no formulário e conserva os dados.

## Acessibilidade

O fluxo deverá oferecer:

- região de progresso com `aria-live="polite"`;
- texto completo da etapa, sem depender apenas da barra visual;
- `aria-current="step"` no estado ativo;
- título da etapa focalizável programaticamente com `tabindex="-1"`;
- foco no título ao avançar ou voltar por ação explícita;
- foco no primeiro campo inválido quando a navegação falhar;
- botões de navegação com `type="button"`;
- botão final com `type="submit"`;
- `hidden` aplicado somente pela melhoria progressiva;
- estado expandido da aquisição comunicado por ARIA;
- alvos de toque de pelo menos 44 px;
- foco visível já compatível com o padrão da aplicação.

Uma carga limpa não moverá o foco automaticamente. Quando a página for devolvida com erros de servidor, depois da ativação do fluxo móvel o primeiro controle inválido receberá foco e será rolado para uma posição visível.

## Organização do JavaScript

`vehicle-form.js` continuará como ponto de entrada da página, mas as responsabilidades serão separadas em funções pequenas e testáveis:

- inicialização do fluxo responsivo;
- leitura e alteração da etapa atual;
- sincronização do progresso e dos botões;
- validação parcial e completa;
- localização da primeira etapa inválida;
- expansão da aquisição;
- ajuda dinâmica do nome;
- captura e comparação do estado inicial;
- confirmação de cancelamento;
- estado de submissão;
- sincronização visual e acessível dos erros.

`vehicle-form-inputs.js` continuará concentrando a regra específica da placa. A função simples `formatVehiclePlate` será preservada para compatibilidade e uma função pura orientada à edição será adicionada para devolver valor e seleção.

Não será criada biblioteca global de wizard. O controlador de veículo poderá reutilizar pequenas utilidades existentes, mas não dependerá do salvamento de rascunhos.

## CSS e layout

O CSS deverá:

- manter a largura máxima e os grids atuais no desktop;
- ocultar progresso e botões móveis a partir de `768px`;
- ocultar somente a etapa inativa quando o formulário estiver melhorado e em viewport móvel;
- reservar espaço inferior suficiente para o rodapé fixo;
- manter ano e placa lado a lado até a largura mínima já definida;
- apresentar a barra de progresso com altura discreta e contraste suficiente;
- estilizar o aviso de validação sem competir com os erros dos campos;
- transformar a aquisição em disclosure apenas no celular;
- preservar os estados de foco e erro do `input-group`;
- evitar animações obrigatórias e respeitar `prefers-reduced-motion` caso haja transição.

## Arquivos afetados

### Produção

- `src/main/resources/templates/vehicles/form.html`
- `src/main/resources/static/js/vehicle-form.js`
- `src/main/resources/static/js/vehicle-form-inputs.js`
- `src/main/resources/static/css/app.css`

### Testes

- `src/test/js/vehicle-form.test.mjs`
- `src/test/js/vehicle-form-inputs.test.mjs`
- `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`
- `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`

`VehicleForm.java`, entidades, serviços, migrations e banco não deverão ser alterados, salvo se um teste revelar uma incompatibilidade real não prevista. Nesse caso, a alteração deverá ser discutida antes de ampliar o escopo.

## Estratégia de testes

### JavaScript — fluxo

Os testes deverão cobrir:

- ausência segura quando `#vehicleForm` não existe;
- melhoria ativada somente em viewport móvel;
- todas as seções visíveis no desktop;
- etapa inicial de identificação sem erros;
- etapa inicial determinada pelo primeiro erro de servidor;
- erro em preço abrindo operação e aquisição;
- “Continuar” validando somente a primeira etapa;
- bloqueio do avanço com foco no primeiro erro;
- avanço válido para operação;
- retorno para identificação;
- progresso e botões sincronizados;
- envio inválido voltando à primeira etapa com erro;
- envio válido desabilitando o botão;
- `pageshow` restaurando o botão;
- redimensionamento preservando a etapa atual;
- ajuda do nome sem sobrescrever o campo;
- aquisição preservando o estado de expansão;
- comparação de estado sem falso positivo de alteração;
- alteração restaurada ao original deixando de ser considerada suja;
- cancelamento sem mudança navegando diretamente;
- cancelamento com mudança exigindo confirmação;
- `beforeunload` somente com mudança real.

### JavaScript — placa

Os testes deverão cobrir:

- placa antiga completa e parcial;
- placa Mercosul completa e parcial;
- normalização de letras minúsculas e caracteres inválidos;
- limite de sete caracteres úteis;
- digitação no final;
- inserção no meio;
- backspace próximo ao hífen automático;
- substituição de seleção parcial;
- seleção total e colagem;
- preservação de seleção recolhida e não recolhida;
- idempotência da formatação.

### Web e contrato

Os testes MockMvc deverão verificar:

- ordem de marca, modelo, ano, placa e nome no HTML;
- `data-vehicle-flow` e as duas etapas;
- indicador de progresso móvel;
- botões “Continuar” e “Voltar” com `type="button"`;
- botão final mantendo `type="submit"`;
- aquisição com botão expansível e atributos ARIA;
- manutenção dos atributos dos campos localizados;
- ausência de `data-guided-form`;
- ausência de atributos de rascunho;
- criação e edição continuando com ações e textos corretos;
- retorno com erro de validação preservando `.is-invalid` no campo correspondente.

O contrato de formulários simples será ajustado para registrar explicitamente que veículo possui navegação móvel própria, mas continua fora do sistema compartilhado de formulários guiados.

## Critérios de aceitação

A implementação estará pronta quando:

1. o celular exibir exatamente duas etapas com progresso textual e barra linear;
2. o desktop continuar exibindo todas as seções sem navegação por etapas;
3. a primeira etapa não avançar com marca, modelo, ano ou placa inválidos;
4. erros de servidor abrirem a primeira etapa correspondente;
5. erro ou valor de preço abrirem a aquisição quando necessário;
6. o nome opcional aparecer depois dos dados essenciais e receber ajuda contextual;
7. a placa puder ser corrigida no meio sem o cursor saltar para o final;
8. cancelar solicitar confirmação somente quando houver alteração real;
9. o formulário continuar utilizável sem JavaScript;
10. o veículo continuar sem rascunho e sem `data-guided-form`;
11. nenhuma migration ou alteração de domínio for necessária;
12. os testes JavaScript e Java novos passarem;
13. a suíte completa e a CI operacional concluírem sem regressões;
14. nenhuma alteração for mesclada automaticamente.
