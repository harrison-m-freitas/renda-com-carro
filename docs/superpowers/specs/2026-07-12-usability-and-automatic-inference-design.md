# Design — Rótulos amigáveis e fechamento mensal automático

**Data:** 12/07/2026  
**Status:** aprovado conceitualmente; aguardando revisão do documento  
**Escopo:** refinamento do MVP na branch `feature/mvp`

## 1. Objetivo

Melhorar a usabilidade da aplicação em dois pontos:

1. impedir que valores técnicos de enums, como `PROFESSIONAL`, `MILEAGE_RATIO` ou `BANK_FINANCING`, apareçam para o usuário;
2. substituir o preenchimento manual do fechamento mensal de quilometragem por valores inferidos a partir dos registros operacionais já existentes.

Os valores técnicos continuarão sendo usados internamente no Java e no PostgreSQL. A alteração é de apresentação, cálculo e fluxo, sem renomear dados persistidos.

---

## 2. Rótulos amigáveis para enums

### 2.1 Estratégia

Todos os enums apresentados na interface deverão implementar um contrato comum, por exemplo `LabeledEnum`, com um método `getLabel()`.

Exemplo conceitual:

```java
public enum ExpenseClassification implements LabeledEnum {
    PROFESSIONAL("Profissional"),
    PERSONAL("Pessoal"),
    MIXED("Misto");
}
```

A aplicação continuará persistindo `PROFESSIONAL`, `PERSONAL` e `MIXED`. O usuário verá apenas os rótulos em português.

### 2.2 Cobertura

O rótulo deverá ser usado em:

- selects de formulários;
- tabelas;
- telas de detalhes;
- badges de status;
- filtros futuros;
- mensagens que mencionem a opção selecionada.

Enums inicialmente cobertos:

- `FuelType`;
- `VehicleStatus`;
- `OperationalDayStatus`;
- `ShiftStatus`;
- `DataSource`;
- `RevenueType`;
- `ExpenseClassification`;
- `AllocationMethod`;
- `GoalStatus`;
- `ObligationType`;
- `ObligationMode`;
- `ObligationStatus`;
- `InstallmentStatus`;
- `OwnerType`, quando exibido ao usuário.

### 2.3 Rótulos esperados

Exemplos:

| Valor interno | Rótulo |
|---|---|
| `PROFESSIONAL` | Profissional |
| `PERSONAL` | Pessoal |
| `MIXED` | Misto |
| `MILEAGE_RATIO` | Proporcional à quilometragem |
| `MANUAL_PERCENTAGE` | Percentual informado manualmente |
| `FIXED_AMOUNT` | Valor profissional fixo |
| `FAMILY_LOAN` | Empréstimo familiar |
| `BANK_FINANCING` | Financiamento bancário |
| `STRUCTURED` | Parcelas programadas |
| `FLEXIBLE` | Pagamentos flexíveis |
| `IN_PROGRESS` | Em andamento |
| `CANCELLED` | Cancelado |
| `GASOLINE` | Gasolina |
| `ETHANOL` | Etanol |

### 2.4 Regra de qualidade

Nenhum template poderá usar diretamente o nome técnico de um enum em conteúdo visível. Testes web deverão procurar rótulos em português e rejeitar a exposição de constantes técnicas conhecidas.

---

## 3. Fechamento mensal automático

### 3.1 Novo fluxo

O formulário será convertido em uma tela de prévia calculada:

1. o usuário seleciona veículo e mês;
2. o sistema calcula os valores disponíveis;
3. a tela mostra valores, origem dos dados e alertas;
4. os campos calculados permanecem bloqueados;
5. o usuário salva o fechamento ou ativa **Corrigir valores**;
6. qualquer correção manual exige justificativa.

### 3.2 Valores calculados

O sistema calculará:

- odômetro inicial;
- odômetro final;
- quilômetros totais;
- quilômetros profissionais;
- quilômetros pessoais;
- percentual de uso profissional.

Fórmulas:

```text
quilômetros totais = odômetro final − odômetro inicial
quilômetros profissionais = soma das distâncias dos turnos fechados no mês
quilômetros pessoais = quilômetros totais − quilômetros profissionais
percentual profissional = quilômetros profissionais ÷ quilômetros totais
```

Quando o total for zero, o percentual profissional será zero e não haverá divisão por zero.

### 3.3 Origem do odômetro inicial

A prioridade será:

1. odômetro final do fechamento mensal anterior do mesmo veículo;
2. odômetro inicial do primeiro dia operacional válido do mês;
3. odômetro inicial do primeiro turno válido do mês;
4. primeiro abastecimento registrado no mês;
5. odômetro inicial cadastrado no veículo, quando não houver histórico anterior suficiente.

A tela informará a origem escolhida.

### 3.4 Origem do odômetro final

A prioridade será considerar a leitura cronologicamente mais recente e confiável dentro do mês entre:

- odômetro final de dia operacional fechado;
- odômetro final de turno fechado;
- abastecimento;
- odômetro atual do veículo, somente para o mês corrente e quando sua atualização tiver origem rastreável.

A maior leitura não será escolhida cegamente: a data do registro e a ausência de regressão deverão ser respeitadas.

### 3.5 Quilometragem profissional

A quilometragem profissional será a soma das distâncias dos turnos fechados cuja data pertença ao mês e ao veículo selecionado.

Turnos cancelados ou abertos não entram no cálculo.

A diferença entre a distância do dia e a soma dos turnos não será automaticamente tratada como profissional. Ela ficará incluída no uso pessoal/não classificado e poderá gerar um aviso informativo.

### 3.6 Atualização automática do odômetro do veículo

Leituras válidas poderão atualizar o odômetro atual do veículo quando forem mais recentes e não representarem regressão.

Eventos considerados:

- fechamento de turno;
- fechamento de dia;
- abastecimento;
- fechamento mensal confirmado;
- ajuste manual explícito do veículo.

A regra será centralizada em um serviço de odômetro, evitando lógicas diferentes em cada módulo.

Entradas históricas poderão ser cadastradas sem reduzir o odômetro atual. Nesses casos, o registro histórico será preservado, mas não substituirá a leitura atual.

### 3.7 Alertas e bloqueios

A prévia deverá alertar quando:

- não houver leitura suficiente para calcular início ou fim;
- existir turno aberto no mês;
- existir dia operacional aberto no mês;
- o odômetro final for menor que o inicial;
- a soma profissional for maior que a distância total;
- houver regressão entre leituras cronológicas;
- já existir fechamento para o veículo e o mês;
- existirem lacunas relevantes entre dias e turnos.

Erros estruturais bloqueiam o fechamento. Avisos não estruturais permitem continuar após confirmação.

### 3.8 Correção manual

O botão **Corrigir valores** liberará:

- odômetro inicial;
- odômetro final;
- quilômetros profissionais.

Ao alterar qualquer valor inferido:

- a justificativa passa a ser obrigatória;
- os valores derivados são recalculados;
- o fechamento será marcado como ajustado manualmente;
- a tela de detalhes mostrará valor inferido, valor confirmado e justificativa.

---

## 4. Componentes previstos

### 4.1 Apresentação de enums

- `LabeledEnum` em `shared`;
- implementação nos enums visíveis;
- templates usando `enum.label`;
- testes de interface para rótulos.

### 4.2 Inferência mensal

Um serviço dedicado, por exemplo `MonthlyMileageInferenceService`, receberá veículo e mês e devolverá uma prévia imutável contendo:

- valores calculados;
- origem de cada valor;
- registros considerados;
- alertas;
- bloqueios.

O serviço de fechamento apenas validará a prévia ou a correção manual e persistirá o resultado.

### 4.3 Odômetro atual

Um `VehicleOdometerService` centralizará a atualização do odômetro atual e a validação contra regressões.

Os módulos de operação e combustível não deverão alterar diretamente o veículo.

---

## 5. Persistência

O fechamento mensal continuará armazenando os valores confirmados.

Serão acrescentados, conforme necessário:

- indicador de ajuste manual;
- valores originalmente inferidos;
- justificativa;
- origem do odômetro inicial;
- origem do odômetro final;
- data de cálculo/confirmação.

A migração Flyway deverá ser aditiva e preservar fechamentos existentes.

Não haverá renomeação dos valores de enum persistidos.

---

## 6. Interface

### 6.1 Formulários gerais

- opções traduzidas;
- descrições mais claras para métodos de rateio;
- campos condicionais exibidos apenas quando aplicáveis;
- valores técnicos nunca visíveis.

Exemplo no gasto:

- ao escolher **Profissional** ou **Pessoal**, ocultar método de rateio e campos manuais;
- ao escolher **Misto**, mostrar o método;
- ao escolher **Proporcional à quilometragem**, ocultar percentual e valor fixo;
- ao escolher **Percentual informado manualmente**, mostrar percentual e justificativa;
- ao escolher **Valor profissional fixo**, mostrar valor e justificativa.

### 6.2 Fechamento mensal

A tela mostrará:

- veículo e mês;
- resumo dos registros usados;
- odômetro inicial e origem;
- odômetro final e origem;
- total rodado;
- quilômetros profissionais;
- quilômetros pessoais;
- percentual profissional;
- avisos;
- ações **Salvar fechamento** e **Corrigir valores**.

---

## 7. Tratamento de erros

- falta de dados retorna uma prévia incompleta, não um erro 500;
- conflitos de fechamento duplicado retornam mensagem clara;
- regressões de odômetro indicam registros envolvidos;
- correção manual sem justificativa é rejeitada no formulário e no domínio;
- os dados preenchidos permanecem na tela após erro.

---

## 8. Testes

### 8.1 Unitários

- rótulo de todos os enums visíveis;
- escolha da leitura inicial por prioridade;
- escolha da leitura final por cronologia;
- soma de turnos fechados;
- exclusão de turnos abertos/cancelados;
- cálculo pessoal e percentual;
- total zero;
- regressão;
- correção manual e justificativa;
- atualização centralizada do odômetro.

### 8.2 Integração

- inferência usando PostgreSQL e registros reais;
- fechamento anterior alimentando o mês seguinte;
- fechamento de turno/dia e abastecimento atualizando o veículo corretamente;
- entrada histórica não reduzindo o odômetro atual;
- migração preservando dados existentes.

### 8.3 Web

- selects mostram português;
- constantes técnicas não aparecem;
- prévia automática do fechamento;
- campos calculados bloqueados;
- modo de correção manual;
- justificativa obrigatória;
- alertas e bloqueios apresentados corretamente.

---

## 9. Critérios de aceite

A melhoria será considerada concluída quando:

1. todos os enums visíveis possuírem rótulos em português;
2. nenhum select relevante mostrar constantes técnicas;
3. o fechamento mensal puder ser criado sem digitar manualmente os quilômetros profissionais;
4. início, fim e quilometragem profissional forem inferidos com origem visível;
5. inconsistências forem detectadas antes da gravação;
6. correções manuais exigirem justificativa;
7. o odômetro atual do veículo for atualizado por um fluxo centralizado;
8. entradas históricas não causarem regressão;
9. testes unitários, web, integração e CI permanecerem verdes.

---

## 10. Fora deste refinamento

- rastreamento GPS;
- inferência de distância sem odômetro;
- integração automática com Uber ou 99;
- alteração dos valores persistidos dos enums;
- internacionalização para múltiplos idiomas;
- reconciliação contábil completa do mês.
