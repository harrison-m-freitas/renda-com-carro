# Formulários guiados e rascunhos sincronizados

Data: 2026-07-13  
Status: aprovado em conversa, aguardando revisão do documento  
Escopo: Gastos, Fechamentos de km, Metas e Obrigações

## 1. Objetivo

Melhorar a experiência de preenchimento dos formulários mais complexos da aplicação, mantendo os formulários simples compactos. A solução deve oferecer orientação por etapas, inputs mais claros, salvamento automático de rascunhos, recuperação entre dispositivos e proteção contra perda ou sobrescrita silenciosa de dados.

## 2. Decisões aprovadas

- Formulários guiados serão usados apenas em Gastos, Fechamentos de km, Metas e Obrigações.
- No desktop, todas as seções guiadas permanecem visíveis na mesma página.
- No celular, o formulário funciona como passo a passo, exibindo uma etapa por vez.
- O avanço para a etapa seguinte salva o rascunho automaticamente.
- Rascunhos são sincronizados pelo servidor entre computador e celular.
- Rascunhos expiram após sete dias sem edição.
- Ao encontrar um rascunho, a aplicação pergunta se o usuário deseja continuar ou descartar.
- A organização de rascunhos é híbrida:
  - um gasto em andamento;
  - um fechamento por veículo e mês;
  - uma meta por mês;
  - várias obrigações em rascunho.
- A persistência será feita por uma entidade genérica `FormDraft`, com regras específicas por tipo de formulário.
- Conflitos entre dispositivos usam bloqueio otimista e nunca sobrescrevem silenciosamente a versão mais recente.

## 3. Experiência dos formulários

### 3.1 Desktop

Os quatro formulários guiados exibem todas as seções na mesma página. Cada seção contém:

1. título numerado;
2. descrição curta;
3. campos relacionados;
4. mensagens de erro junto ao campo;
5. valores automáticos claramente diferenciados;
6. resumo final antes do envio.

Exemplo de estrutura:

```text
1. Dados principais
Descrição breve.
[ campos ]

2. Classificação e cálculo
Descrição breve.
[ campos e valores automáticos ]

3. Revisão
[ resumo final ]
```

### 3.2 Celular

No celular:

- somente a etapa atual fica visível;
- aparece um indicador como `Etapa 2 de 4`;
- as ações principais são `Voltar` e `Continuar`;
- a última etapa usa `Revisar e salvar`;
- a barra de ações fica fixa na parte inferior em formulários longos;
- avançar exige validação dos campos obrigatórios da etapa atual;
- o avanço só acontece depois que o rascunho da etapa foi salvo com sucesso.

### 3.3 Recuperação de rascunho

Ao abrir um formulário com rascunho válido, a aplicação exibe uma escolha explícita:

```text
Existe um rascunho salvo em 13/07/2026 às 10:42.

Continuar rascunho
Começar novamente
```

`Começar novamente` exige confirmação antes de descartar o rascunho.

Ao restaurar:

1. valores editáveis são recuperados;
2. selects são revalidados contra os dados atuais;
3. valores derivados são recalculados;
4. referências removidas ou inválidas são sinalizadas;
5. a aplicação retorna à última etapa válida, não necessariamente à etapa gravada.

## 4. Padrão visual dos inputs

### 4.1 Campos obrigatórios e erros

- campos obrigatórios exibem `*` no rótulo;
- o erro aparece diretamente abaixo do campo;
- o erro é ligado ao input por `aria-describedby`;
- ao falhar uma validação, o foco vai para o primeiro campo inválido;
- nenhuma informação depende apenas de cor.

Exemplo:

```text
Valor *
[R$ 0,00]

Informe um valor maior que zero.
```

### 4.2 Valores monetários

- prefixo visual `R$`;
- aceitação de vírgula na entrada;
- formatação visual com duas casas decimais;
- persistência como número decimal, independente da máscara;
- valores automáticos ficam bloqueados e visualmente distintos.

### 4.3 Percentuais

O usuário informa valores de `0` a `100`.

```text
Percentual profissional
[ 75 ] %
```

A aplicação pode continuar convertendo internamente `75%` para `0,75`.

### 4.4 Odômetro e distâncias

- sufixo visual `km`;
- uma casa decimal;
- separador brasileiro na exibição;
- valor negativo bloqueado;
- aviso imediato quando a leitura é inferior à última leitura conhecida;
- validação definitiva permanece no servidor.

### 4.5 Datas e competências

- datas de evento usam dia completo;
- competências usam apenas mês e ano;
- campos de pagamento opcionais mostram a ajuda `Deixe vazio enquanto estiver pendente`.

### 4.6 Campos automáticos e correções

Campos calculados não são editáveis por padrão.

Quando a correção for permitida:

1. o usuário aciona `Corrigir valor`;
2. os campos manuais são exibidos;
3. a justificativa passa a ser obrigatória;
4. o servidor recalcula e valida novamente antes de persistir.

### 4.7 Seletores

As opções iniciais devem ser específicas:

- `Selecione um veículo`;
- `Selecione uma categoria`;
- `Selecione a classificação`.

Veículos devem exibir identificação e contexto útil, por exemplo:

```text
Honda HB20 · ABC2C26
Odômetro atual: 54.320,5 km
```

Escolhas pequenas e estáveis podem usar cartões ou botões de opção em vez de selects.

### 4.8 Ajuda contextual

Textos de ajuda devem ser curtos. Explicações longas ficam em:

- ícone de ajuda;
- bloco informativo da etapa;
- aviso contextual exibido apenas quando necessário.

## 5. Modelo de dados dos rascunhos

A entidade genérica será:

```text
FormDraft
├── id
├── owner
├── formType
├── contextKey
├── schemaVersion
├── currentStep
├── payloadJson
├── version
├── createdAt
├── updatedAt
└── expiresAt
```

### 5.1 Restrições

- combinação única entre `owner`, `formType` e `contextKey`;
- `payloadJson` limitado por tamanho;
- `formType` restrito a tipos conhecidos;
- `schemaVersion` obrigatório;
- `version` usado para bloqueio otimista;
- `expiresAt` renovado a cada alteração válida.

### 5.2 Chaves de contexto

| Formulário | Chave |
|---|---|
| Gasto | `current` |
| Fechamento de km | `vehicle:{id}:month:{YYYY-MM}` |
| Meta | `month:{YYYY-MM}` |
| Obrigação | identificador próprio |

A chave de fechamento impede dois rascunhos para o mesmo veículo e mês. A chave de meta impede duas metas para o mesmo mês. Obrigações podem ter vários rascunhos.

## 6. Definições específicas por formulário

O armazenamento é genérico, mas o contrato de cada formulário é explícito:

```text
ExpenseDraftDefinition
MileageClosingDraftDefinition
MonthlyGoalDraftDefinition
ObligationDraftDefinition
```

Cada definição informa:

- campos permitidos;
- etapas existentes;
- campos necessários para avançar;
- versão atual do payload;
- gerador da chave de contexto;
- regras de normalização;
- campos derivados que nunca são aceitos como fonte confiável;
- estratégia de migração entre versões compatíveis.

Campos desconhecidos são rejeitados. O servidor nunca trata o JSON como estrutura arbitrária.

## 7. Componentes do servidor

```text
FormDraftController
        ↓
FormDraftService
        ↓
FormDraftRepository
        ↓
PostgreSQL
```

### 7.1 `FormDraftController`

Responsável por endpoints autenticados para:

- consultar rascunho;
- criar ou atualizar;
- descartar;
- listar rascunhos de obrigações;
- resolver conflito de versão.

### 7.2 `FormDraftService`

Responsável por:

- construir e validar chaves;
- aplicar limites de quantidade;
- aplicar bloqueio otimista;
- atualizar o prazo de sete dias;
- validar tipo, tamanho, versão e estrutura do payload;
- excluir o rascunho depois de um envio definitivo bem-sucedido.

### 7.3 `FormDraftRepository`

Responsável apenas por persistência e consultas, sem regras de negócio.

## 8. Fluxo de salvamento automático

### 8.1 Disparos

O autosave ocorre:

- aproximadamente 1,5 segundo após a última alteração;
- imediatamente ao pressionar `Continuar`;
- ao sair de um campo considerado importante;
- antes de sair da página, quando o navegador permitir.

O atraso reinicia enquanto o usuário continua digitando. Não haverá requisição a cada tecla.

### 8.2 Conteúdo enviado

O cliente envia o estado editável do formulário e a versão conhecida. Valores derivados não são fonte de verdade.

A interface exibe estados discretos:

```text
Salvando…
Rascunho salvo às 10:42
Falha ao salvar — tentar novamente
```

### 8.3 Avanço de etapa

Ao tocar em `Continuar`:

1. o navegador valida os campos da etapa;
2. o rascunho é salvo imediatamente;
3. o servidor valida os campos exigidos para avançar;
4. o servidor retorna a nova versão;
5. somente então a próxima etapa é exibida.

## 9. Sincronização e conflitos

Cada atualização envia a versão carregada. Uma versão antiga não sobrescreve uma versão mais recente.

Em caso de conflito, o servidor responde com `409 Conflict` e informa a versão e horário atuais.

A interface mostra:

```text
Este rascunho foi alterado em outro dispositivo.

Versão deste dispositivo: 10:42
Versão salva no servidor: 10:48
```

Ações disponíveis:

- `Usar versão mais recente`;
- `Revisar minhas alterações`;
- `Substituir versão do servidor`.

A substituição exige confirmação explícita. Não haverá mesclagem automática campo a campo na primeira versão da funcionalidade.

## 10. Falhas de conexão

Quando o servidor estiver indisponível:

```text
Não foi possível sincronizar o rascunho. Suas alterações estão preservadas neste dispositivo.
```

O navegador mantém temporariamente a alteração não sincronizada como proteção emergencial. Esse armazenamento local:

- não é a fonte principal;
- não substitui o rascunho do servidor;
- é removido após sincronização bem-sucedida;
- participa da verificação de conflito ao reconectar.

O usuário pode continuar preenchendo a etapa atual, mas a interface não indica que o rascunho está sincronizado. O avanço de etapa depende da sincronização bem-sucedida.

## 11. Envio definitivo

O rascunho nunca é convertido diretamente em registro final.

```text
Payload final
   ↓
Validação completa no servidor
   ↓
Recálculo de valores derivados
   ↓
Persistência do registro real
   ↓
Exclusão do rascunho na mesma transação
```

Regras:

- totais, rateios, percentuais, quilometragem, projeções e duplicidades são recalculados;
- o registro final e a exclusão do rascunho pertencem à mesma transação;
- se o envio falhar, o rascunho permanece disponível;
- após sucesso, o rascunho correspondente é removido.

## 12. Expiração e limpeza

- cada alteração válida renova `expiresAt` para sete dias após `updatedAt`;
- uma rotina diária remove rascunhos expirados;
- rascunhos expirados não são oferecidos para recuperação;
- a limpeza deve ser idempotente;
- não há lixeira ou restauração após expiração na primeira versão.

## 13. Segurança

Os endpoints de rascunho terão:

- autenticação obrigatória;
- isolamento por usuário;
- proteção CSRF;
- validação de `formType`;
- limite de tamanho do JSON;
- rejeição de campos desconhecidos;
- sanitização de textos exibidos;
- proibição de anexos binários dentro do JSON;
- validação completa novamente no envio final.

Anexos ficam fora deste escopo e continuam sendo tratados no fluxo definitivo do domínio.

## 14. Melhoria progressiva

JavaScript melhora a experiência, mas não pode tornar o registro impossível.

Sem JavaScript:

- o desktop continua mostrando o formulário completo;
- o usuário ainda consegue enviar o formulário final;
- autosave, recuperação dinâmica e passo a passo móvel ficam indisponíveis;
- a validação no servidor permanece completa.

## 15. Testes

### 15.1 Domínio

- geração de chave por tipo;
- unicidade por usuário, tipo e contexto;
- renovação do prazo;
- expiração após sete dias;
- limites de rascunho;
- conflito de versão;
- rejeição de campo desconhecido;
- rejeição de payload excessivo;
- migração de payload compatível;
- recálculo de campos derivados.

### 15.2 Integração

- usuário não acessa rascunho de outro usuário;
- criação, atualização, recuperação e descarte;
- retorno `409` em conflito;
- rascunho preservado quando o envio final falha;
- exclusão transacional após envio bem-sucedido;
- limpeza de expirados;
- revalidação de referências atuais.

### 15.3 Interface

- desktop mostra todas as seções;
- celular mostra uma etapa por vez;
- etapa inválida não avança;
- `Continuar` aguarda salvamento;
- recuperação pergunta antes de restaurar;
- estados `Salvando`, `Salvo` e `Falha`;
- conflito oferece as três ações previstas;
- foco no primeiro campo inválido;
- prefixos `R$`, sufixos `%` e `km`;
- barra móvel fixa em formulário longo;
- funcionamento básico sem JavaScript.

## 16. Fora de escopo

- anexos em rascunhos;
- mesclagem automática campo a campo;
- histórico completo de todas as versões do rascunho;
- restauração de rascunho expirado;
- colaboração simultânea entre usuários;
- rascunhos para formulários simples;
- mudança dos valores técnicos já persistidos pelos enums.

## 17. Critérios de aceite

A funcionalidade estará concluída quando:

1. os quatro formulários complexos adotarem o padrão guiado;
2. o desktop mostrar seções na mesma página;
3. o celular operar em etapas;
4. o autosave sincronizar entre dispositivos;
5. a recuperação perguntar antes de restaurar ou descartar;
6. os rascunhos expirarem após sete dias sem edição;
7. as regras híbridas de quantidade forem respeitadas;
8. conflitos nunca sobrescreverem silenciosamente outra versão;
9. valores derivados forem recalculados no envio final;
10. o rascunho for removido apenas após persistência final bem-sucedida;
11. inputs monetários, percentuais, datas e odômetros seguirem o padrão aprovado;
12. a aplicação continuar permitindo envio básico sem JavaScript;
13. os testes de domínio, integração e interface cobrirem os fluxos críticos.
