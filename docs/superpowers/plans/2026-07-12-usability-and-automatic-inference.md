# Usabilidade e Inferência Automática — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** impedir a exposição de valores técnicos de enums e tornar o fechamento mensal de quilometragem automático, rastreável e corrigível apenas com justificativa.

**Architecture:** a apresentação de enums será padronizada por um contrato `LabeledEnum`, sem alterar os valores persistidos. A inferência mensal será isolada em um serviço puro de aplicação que consulta leituras operacionais, produz uma prévia imutável com origens/alertas e delega a persistência ao serviço de fechamento. A atualização do odômetro atual será centralizada em `VehicleOdometerService` e chamada pelos módulos de operação, combustível e fechamento.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL 17, Flyway, Bean Validation, JUnit 5, MockMvc e Testcontainers.

## Global Constraints

- Manter os nomes técnicos atuais dos enums no Java e no PostgreSQL; nenhuma migração pode renomear valores persistidos.
- Interface server-side com Thymeleaf; JavaScript apenas para exibição condicional e desbloqueio visual de campos.
- Aplicação pessoal single-user; não adicionar cadastro público, RBAC ou endpoints públicos.
- O fechamento automático deve permitir correção manual somente com justificativa não vazia.
- Entradas históricas podem ser gravadas, mas nunca podem reduzir o odômetro atual do veículo.
- Regressões cronológicas reais devem gerar bloqueio explícito, não correção silenciosa.
- A branch `feature/usability-inference` deve permanecer verde; estados RED devem ser comprovados localmente ou em branch temporária.
- Permanecem fora do escopo: GPS, integração Uber/99, alteração de enums persistidos, internacionalização multilíngue e reconciliação contábil completa.

---

## Mapa de arquivos

### Novos arquivos

- `src/main/java/dev/harrison/rendacomcarro/shared/domain/LabeledEnum.java`: contrato de rótulo amigável.
- `src/main/java/dev/harrison/rendacomcarro/vehicle/domain/OdometerReadingSource.java`: origem rastreável da leitura atual.
- `src/main/java/dev/harrison/rendacomcarro/vehicle/application/OdometerUpdateResult.java`: resultado da tentativa de atualizar o odômetro.
- `src/main/java/dev/harrison/rendacomcarro/vehicle/application/VehicleOdometerService.java`: regra central de atualização e regressão.
- `src/main/java/dev/harrison/rendacomcarro/expense/application/MileageAlertSeverity.java`: severidade de alertas da prévia.
- `src/main/java/dev/harrison/rendacomcarro/expense/application/MileageAlert.java`: alerta imutável e identificável.
- `src/main/java/dev/harrison/rendacomcarro/expense/application/OdometerOrigin.java`: origem do odômetro inferido.
- `src/main/java/dev/harrison/rendacomcarro/expense/application/MonthlyMileagePreview.java`: resultado imutável da inferência.
- `src/main/java/dev/harrison/rendacomcarro/expense/application/MonthlyMileageInferenceService.java`: composição das leituras e cálculos.
- `src/main/resources/db/migration/V7__add_odometer_traceability_and_inferred_closing.sql`: migração aditiva.
- `src/main/resources/static/js/expense-form.js`: campos condicionais do gasto.
- `src/main/resources/static/js/mileage-closing-form.js`: modo de correção manual.
- `src/test/java/dev/harrison/rendacomcarro/shared/VisibleEnumLabelTest.java`.
- `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleOdometerServiceTest.java`.
- `src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageInferenceServiceTest.java`.
- `src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageClosingWebTest.java`.
- `src/test/java/dev/harrison/rendacomcarro/web/EnumPresentationWebTest.java`.

### Arquivos principais modificados

- todos os enums visíveis listados na especificação;
- templates de veículos, operação, receitas, gastos, abastecimentos, metas, obrigações e anexos;
- `Vehicle`, `VehicleService`, `ShiftService`, `OperationalDayService`, `FuelingService`;
- repositórios de dias, turnos, abastecimentos e fechamentos;
- `MonthlyOdometerClosing`, seu formulário, serviço, controller e templates.

---

### Task 1: Contrato e rótulos de todos os enums visíveis

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/shared/domain/LabeledEnum.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/domain/FuelType.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/domain/VehicleStatus.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/domain/OperationalDayStatus.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/domain/ShiftStatus.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/domain/DataSource.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/domain/RevenueType.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/domain/ExpenseClassification.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/domain/AllocationMethod.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/domain/GoalStatus.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationType.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationMode.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationStatus.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/InstallmentStatus.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/attachment/domain/OwnerType.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/shared/VisibleEnumLabelTest.java`

**Interfaces:**
- Produces: `interface LabeledEnum { String getLabel(); }`
- Produces: todos os enums acima implementando `LabeledEnum`.

- [ ] **Step 1: escrever o teste contratual RED**

```java
class VisibleEnumLabelTest {
    private static final List<Class<? extends Enum<?>>> VISIBLE_ENUMS = List.of(
        FuelType.class, VehicleStatus.class, OperationalDayStatus.class,
        ShiftStatus.class, DataSource.class, RevenueType.class,
        ExpenseClassification.class, AllocationMethod.class, GoalStatus.class,
        ObligationType.class, ObligationMode.class, ObligationStatus.class,
        InstallmentStatus.class, OwnerType.class
    );

    @Test
    void everyVisibleEnumHasFriendlyPortugueseLabel() {
        for (Class<? extends Enum<?>> type : VISIBLE_ENUMS) {
            assertTrue(LabeledEnum.class.isAssignableFrom(type));
            for (Enum<?> constant : type.getEnumConstants()) {
                String label = ((LabeledEnum) constant).getLabel();
                assertNotNull(label);
                assertFalse(label.isBlank());
                assertNotEquals(constant.name(), label);
                assertFalse(label.matches("[A-Z0-9_]+"));
            }
        }
    }
}
```

- [ ] **Step 2: executar o teste e confirmar falha pelo contrato ausente**

Run: `./mvnw -Dtest=VisibleEnumLabelTest test`

Expected: FAIL de compilação porque `LabeledEnum` ainda não existe.

- [ ] **Step 3: criar o contrato mínimo**

```java
package dev.harrison.rendacomcarro.shared.domain;

public interface LabeledEnum {
    String getLabel();
}
```

- [ ] **Step 4: implementar rótulos exatos**

Usar construtor privado, campo `label` e `getLabel()` em cada enum. Rótulos obrigatórios:

```text
FuelType: FLEX=Flex; GASOLINE=Gasolina; ETHANOL=Etanol; DIESEL=Diesel; ELECTRIC=Elétrico; HYBRID=Híbrido
VehicleStatus: ACTIVE=Ativo; ARCHIVED=Arquivado
OperationalDayStatus: IN_PROGRESS=Em andamento; CLOSED=Fechado; CANCELLED=Cancelado
ShiftStatus: OPEN=Aberto; CLOSED=Fechado; CANCELLED=Cancelado
DataSource: MANUAL=Manual; IMPORT=Importação; COLLECTOR=Coletor; API=API
RevenueType: TRIP=Corrida; BONUS=Bônus; PROMOTION=Promoção; TIP=Gorjeta; ADJUSTMENT=Ajuste; CONSOLIDATED=Consolidado
ExpenseClassification: PROFESSIONAL=Profissional; PERSONAL=Pessoal; MIXED=Misto
AllocationMethod: MILEAGE_RATIO=Proporcional à quilometragem; MANUAL_PERCENTAGE=Percentual informado manualmente; FIXED_AMOUNT=Valor profissional fixo
GoalStatus: BELOW=Abaixo da meta; ON_TRACK=Dentro da meta; ABOVE=Acima da meta
ObligationType: FAMILY_LOAN=Empréstimo familiar; BANK_FINANCING=Financiamento bancário; OTHER_ACQUISITION=Outro custo de aquisição
ObligationMode: STRUCTURED=Parcelas programadas; FLEXIBLE=Pagamentos flexíveis
ObligationStatus: ACTIVE=Ativa; PAID=Quitada; CANCELLED=Cancelada
InstallmentStatus: PENDING=Pendente; PARTIALLY_PAID=Parcialmente paga; PAID=Paga; OVERDUE=Em atraso; CANCELLED=Cancelada
OwnerType: REVENUE=Receita; EXPENSE=Gasto; FUELING=Abastecimento; OBLIGATION=Obrigação; PAYMENT=Pagamento; VEHICLE=Veículo
```

- [ ] **Step 5: executar teste contratual e suíte de domínio**

Run: `./mvnw -Dtest=VisibleEnumLabelTest,ExpenseAllocationServiceTest,FinancialObligationFlowTest test`

Expected: PASS.

- [ ] **Step 6: commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/shared/domain/LabeledEnum.java \
        src/main/java/dev/harrison/rendacomcarro/{vehicle,operation,expense,goal,finance,attachment} \
        src/test/java/dev/harrison/rendacomcarro/shared/VisibleEnumLabelTest.java
git commit -m "feat: add friendly labels to visible enums"
```

---

### Task 2: Aplicar rótulos e comportamento condicional aos formulários

**Files:**
- Modify: `src/main/resources/templates/vehicles/form.html`
- Modify: `src/main/resources/templates/vehicles/detail.html`
- Modify: `src/main/resources/templates/operation-days/list.html`
- Modify: `src/main/resources/templates/operation-days/detail.html`
- Modify: `src/main/resources/templates/revenues/form.html`
- Modify: `src/main/resources/templates/expenses/form.html`
- Modify: `src/main/resources/templates/expenses/list.html`
- Modify: `src/main/resources/templates/fuelings/form.html`
- Modify: `src/main/resources/templates/goals/detail.html`
- Modify: `src/main/resources/templates/obligations/form.html`
- Modify: `src/main/resources/templates/obligations/list.html`
- Modify: `src/main/resources/templates/obligations/detail.html`
- Create: `src/main/resources/static/js/expense-form.js`
- Test: `src/test/java/dev/harrison/rendacomcarro/web/EnumPresentationWebTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`

**Interfaces:**
- Consumes: `LabeledEnum#getLabel()`.
- Produces: templates usando `enum.label`, nunca `enum.toString()` ou nome técnico.

- [ ] **Step 1: escrever testes web RED para rótulos**

```java
@Test
@WithMockUser
void expenseFormShowsLabelsAndHidesTechnicalEnumNames() throws Exception {
    mvc.perform(get("/expenses/new"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Profissional")))
        .andExpect(content().string(containsString("Proporcional à quilometragem")))
        .andExpect(content().string(not(containsString("PROFESSIONAL"))))
        .andExpect(content().string(not(containsString("MILEAGE_RATIO"))));
}
```

Adicionar casos equivalentes para veículo, receita, abastecimento e obrigação.

- [ ] **Step 2: executar testes e confirmar exposição atual dos nomes técnicos**

Run: `./mvnw -Dtest=EnumPresentationWebTest,ExpenseWebTest test`

Expected: FAIL porque os templates ainda usam `${enum}`.

- [ ] **Step 3: trocar todos os textos visíveis por `.label`**

Exemplo obrigatório:

```html
<option th:each="classification : ${classifications}"
        th:value="${classification}"
        th:text="${classification.label}"></option>
```

Em badges e detalhes:

```html
<span class="badge" th:text="${expense.classification.label}"></span>
```

- [ ] **Step 4: implementar campos condicionais do gasto**

O script deve:

```javascript
const classification = document.querySelector('[name="classification"]');
const allocationMethod = document.querySelector('[name="allocationMethod"]');

function refreshExpenseFields() {
  const mixed = classification.value === 'MIXED';
  document.querySelector('[data-allocation-group]').hidden = !mixed;
  document.querySelector('[data-percentage-group]').hidden =
    !mixed || allocationMethod.value !== 'MANUAL_PERCENTAGE';
  document.querySelector('[data-fixed-group]').hidden =
    !mixed || allocationMethod.value !== 'FIXED_AMOUNT';
  document.querySelector('[data-reason-group]').hidden =
    !mixed || !['MANUAL_PERCENTAGE', 'FIXED_AMOUNT'].includes(allocationMethod.value);
}

classification.addEventListener('change', refreshExpenseFields);
allocationMethod.addEventListener('change', refreshExpenseFields);
refreshExpenseFields();
```

Os campos ocultos devem ser limpos antes do submit para impedir valores residuais incompatíveis.

- [ ] **Step 5: validar comportamento sem depender apenas de JavaScript**

Manter as validações de domínio existentes em `Expense.create(...)`; adicionar teste POST provando que classificação `PROFESSIONAL` ignora/rejeita método manual incompatível conforme a regra definida pelo domínio.

- [ ] **Step 6: executar testes web completos**

Run: `./mvnw -Dtest=EnumPresentationWebTest,ExpenseWebTest,VehicleWebTest test`

Expected: PASS.

- [ ] **Step 7: commit**

```bash
git add src/main/resources/templates src/main/resources/static/js/expense-form.js \
        src/test/java/dev/harrison/rendacomcarro/web/EnumPresentationWebTest.java \
        src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java
git commit -m "feat: present friendly labels in forms and views"
```

---

### Task 3: Centralizar atualização rastreável do odômetro do veículo

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/vehicle/domain/OdometerReadingSource.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/vehicle/application/OdometerUpdateResult.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/vehicle/application/VehicleOdometerService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/domain/Vehicle.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/application/VehicleService.java`
- Modify: `src/main/resources/db/migration/V7__add_odometer_traceability_and_inferred_closing.sql`
- Test: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleOdometerServiceTest.java`

**Interfaces:**
- Produces: `OdometerUpdateResult registerReading(UUID vehicleId, BigDecimal reading, LocalDateTime recordedAt, OdometerReadingSource source, UUID sourceId)`.
- Produces: `Vehicle#registerOdometerReading(...)` encapsulando comparação temporal e numérica.

- [ ] **Step 1: escrever testes RED das três decisões**

```java
@Test
void newerHigherReadingUpdatesVehicle() { /* UPDATED */ }

@Test
void olderHistoricalReadingIsPreservedButDoesNotReduceCurrentOdometer() { /* IGNORED_HISTORICAL */ }

@Test
void newerLowerReadingIsRejectedAsRegression() { /* DomainValidationException */ }
```

- [ ] **Step 2: executar e confirmar classes ausentes**

Run: `./mvnw -Dtest=VehicleOdometerServiceTest test`

Expected: FAIL de compilação.

- [ ] **Step 3: criar migração aditiva**

```sql
ALTER TABLE vehicle
  ADD COLUMN current_odometer_recorded_at TIMESTAMP,
  ADD COLUMN current_odometer_source VARCHAR(40),
  ADD COLUMN current_odometer_source_id UUID;

UPDATE vehicle
SET current_odometer_recorded_at = created_at,
    current_odometer_source = 'VEHICLE_MANUAL'
WHERE current_odometer_recorded_at IS NULL;
```

A mesma migração será ampliada na Task 6 para os campos do fechamento; manter um único arquivo V7.

- [ ] **Step 4: implementar fontes e resultados**

```java
public enum OdometerReadingSource {
    VEHICLE_MANUAL, SHIFT_CLOSE, OPERATIONAL_DAY_CLOSE,
    FUELING, MONTHLY_CLOSING
}

public enum OdometerUpdateResult {
    UPDATED, IGNORED_EQUAL, IGNORED_HISTORICAL
}
```

- [ ] **Step 5: implementar regra central**

Regras exatas:

```text
recordedAt anterior à leitura atual -> manter evento histórico e retornar IGNORED_HISTORICAL
recordedAt igual/posterior e reading < currentOdometer -> lançar DomainValidationException
reading == currentOdometer -> atualizar metadados somente se recordedAt for posterior; retornar IGNORED_EQUAL
reading > currentOdometer -> atualizar valor, data, origem e sourceId; retornar UPDATED
```

- [ ] **Step 6: executar testes unitários e integração do veículo**

Run: `./mvnw -Dtest=VehicleOdometerServiceTest,VehicleServiceTest test`

Expected: PASS.

- [ ] **Step 7: commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/vehicle \
        src/main/resources/db/migration/V7__add_odometer_traceability_and_inferred_closing.sql \
        src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleOdometerServiceTest.java
git commit -m "feat: centralize vehicle odometer updates"
```

---

### Task 4: Integrar odômetro central aos eventos existentes

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/application/ShiftService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/application/OperationalDayService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/fuel/application/FuelingService.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/operation/ShiftServiceTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/operation/OperationalDayFlowTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/fuel/FuelingFlowTest.java`

**Interfaces:**
- Consumes: `VehicleOdometerService#registerReading(...)`.
- Produces: cada fechamento/abastecimento registra a leitura com data e origem corretas.

- [ ] **Step 1: adicionar testes RED de integração**

Casos obrigatórios:

```text
fechar turno em 10/07 com 50.120 km -> veículo passa para 50.120 km
fechar dia em 10/07 com 50.125 km -> veículo passa para 50.125 km
abastecimento em 11/07 com 50.140 km -> veículo passa para 50.140 km
cadastrar abastecimento histórico em 01/07 com 49.900 km -> registro salvo, veículo permanece 50.140 km
```

- [ ] **Step 2: executar e confirmar que o veículo não é atualizado atualmente**

Run: `./mvnw -Dtest=ShiftServiceTest,OperationalDayFlowTest,FuelingFlowTest test`

Expected: FAIL nas asserções do odômetro atual.

- [ ] **Step 3: chamar o serviço após persistir cada evento**

```java
odometerService.registerReading(
    vehicleId,
    finalOdometer,
    endedAt,
    OdometerReadingSource.SHIFT_CLOSE,
    shift.getId()
);
```

Usar respectivamente `OPERATIONAL_DAY_CLOSE` e `FUELING`. O `recordedAt` do dia deverá usar `closedAt`, e não `LocalDate.now()`.

- [ ] **Step 4: executar testes de integração**

Run: `./mvnw -Dtest=ShiftServiceTest,OperationalDayFlowTest,FuelingFlowTest test`

Expected: PASS.

- [ ] **Step 5: commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/{operation,fuel}/application \
        src/test/java/dev/harrison/rendacomcarro/{operation,fuel}
git commit -m "feat: synchronize odometer from operational events"
```

---

### Task 5: Serviço de inferência do fechamento mensal

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/MileageAlertSeverity.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/MileageAlert.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/OdometerOrigin.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/MonthlyMileagePreview.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/MonthlyMileageInferenceService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/infrastructure/OperationalDayRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/operation/infrastructure/ShiftRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/fuel/infrastructure/FuelingRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/infrastructure/MonthlyOdometerClosingRepository.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageInferenceServiceTest.java`

**Interfaces:**
- Produces: `MonthlyMileagePreview infer(UUID vehicleId, YearMonth month)`.
- Produces: `boolean MonthlyMileagePreview.hasBlockingAlerts()`.

- [ ] **Step 1: criar teste RED com cenário completo**

Cenário de referência:

```text
fechamento de junho termina em 10.000 km
julho tem turnos fechados de 30 km e 20 km
última leitura confiável de julho é 10.080 km
resultado: início 10.000; fim 10.080; total 80; profissional 50; pessoal 30; percentual 0,6250
origem inicial: PREVIOUS_MONTH_CLOSING
origem final: CLOSED_OPERATIONAL_DAY ou CLOSED_SHIFT conforme o registro mais recente
```

- [ ] **Step 2: executar e confirmar tipos ausentes**

Run: `./mvnw -Dtest=MonthlyMileageInferenceServiceTest test`

Expected: FAIL de compilação.

- [ ] **Step 3: adicionar consultas orientadas ao período**

Assinaturas exatas:

```java
List<OperationalDay> findAllByVehicleIdAndDateBetweenOrderByDateAsc(
    UUID vehicleId, LocalDate start, LocalDate end);

List<Shift> findAllByOperationalDayVehicleIdAndStartedAtBetweenOrderByStartedAtAsc(
    UUID vehicleId, LocalDateTime start, LocalDateTime end);

List<Fueling> findAllByVehicleIdAndFueledAtBetweenOrderByFueledAtAsc(
    UUID vehicleId, LocalDateTime start, LocalDateTime end);

Optional<MonthlyOdometerClosing>
findTopByVehicleIdAndReferenceMonthBeforeOrderByReferenceMonthDesc(
    UUID vehicleId, LocalDate referenceMonth);
```

- [ ] **Step 4: implementar modelo imutável da prévia**

```java
public record MonthlyMileagePreview(
    UUID vehicleId,
    YearMonth month,
    BigDecimal inferredInitialOdometer,
    BigDecimal inferredFinalOdometer,
    BigDecimal totalKilometers,
    BigDecimal professionalKilometers,
    BigDecimal personalKilometers,
    BigDecimal professionalPercentage,
    OdometerOrigin initialOrigin,
    OdometerOrigin finalOrigin,
    int closedDays,
    int closedShifts,
    int fuelings,
    List<MileageAlert> alerts,
    LocalDateTime calculatedAt
) {
    public boolean hasBlockingAlerts() {
        return alerts.stream().anyMatch(MileageAlert::blocking);
    }
}
```

- [ ] **Step 5: implementar prioridades e cálculos**

Prioridade inicial exata:

```text
PREVIOUS_MONTH_CLOSING
FIRST_OPERATIONAL_DAY
FIRST_SHIFT
FIRST_FUELING
VEHICLE_INITIAL
```

Final: leitura cronologicamente mais recente entre dia fechado, turno fechado e abastecimento; para mês corrente, aceitar veículo atual somente quando `currentOdometerRecordedAt` estiver dentro do mês e houver origem registrada.

Somar apenas turnos `CLOSED`. Calcular com `DecimalPolicy.distance` e percentual com escala 4. Total zero gera percentual `0.0000`.

- [ ] **Step 6: implementar alertas e bloqueios**

Códigos obrigatórios:

```text
MISSING_INITIAL_ODOMETER (blocking)
MISSING_FINAL_ODOMETER (blocking)
OPEN_SHIFT (blocking)
OPEN_OPERATIONAL_DAY (blocking)
ODOMETER_REGRESSION (blocking)
PROFESSIONAL_EXCEEDS_TOTAL (blocking)
DUPLICATE_CLOSING (blocking)
DAY_SHIFT_DISTANCE_GAP (warning quando diferença absoluta > 1,0 km)
NO_PROFESSIONAL_DISTANCE (warning quando há distância total e nenhum turno fechado)
```

- [ ] **Step 7: executar testes unitários e PostgreSQL**

Run: `./mvnw -Dtest=MonthlyMileageInferenceServiceTest test`

Expected: PASS para prioridade, cronologia, turnos excluídos, total zero, regressão e lacunas.

- [ ] **Step 8: commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/expense/application \
        src/main/java/dev/harrison/rendacomcarro/{operation,fuel,expense}/infrastructure \
        src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageInferenceServiceTest.java
git commit -m "feat: infer monthly mileage from recorded operations"
```

---

### Task 6: Persistir valores inferidos, origem e correção manual

**Files:**
- Modify: `src/main/resources/db/migration/V7__add_odometer_traceability_and_inferred_closing.sql`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/domain/MonthlyOdometerClosing.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/application/MonthlyOdometerClosingService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/MonthlyOdometerClosingForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/MonthlyOdometerClosingController.java`
- Modify: `src/main/resources/templates/mileage-closings/form.html`
- Modify: `src/main/resources/templates/mileage-closings/list.html`
- Create: `src/main/resources/static/js/mileage-closing-form.js`
- Test: `src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageClosingWebTest.java`

**Interfaces:**
- Consumes: `MonthlyMileageInferenceService#infer(...)`.
- Produces: `MonthlyOdometerClosing confirm(ConfirmCommand command)`.

- [ ] **Step 1: escrever testes web RED**

Cobrir:

```text
GET /mileage-closings/new?vehicleId=<id>&month=2026-07 mostra prévia automática
campos inferidos aparecem readonly
origens e contagem de registros aparecem
POST sem alteração salva fechamento automático
POST com alteração e justificativa vazia retorna formulário com erro
POST com alteração e justificativa salva manualAdjustment=true
prévia com bloqueio não permite salvar
```

- [ ] **Step 2: executar e confirmar fluxo manual atual**

Run: `./mvnw -Dtest=MonthlyMileageClosingWebTest test`

Expected: FAIL porque o formulário ainda exige números digitados diretamente.

- [ ] **Step 3: completar migração V7**

```sql
ALTER TABLE monthly_odometer_closing
  ADD COLUMN manual_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN inferred_initial_odometer NUMERIC(12,1),
  ADD COLUMN inferred_final_odometer NUMERIC(12,1),
  ADD COLUMN inferred_professional_kilometers NUMERIC(12,1),
  ADD COLUMN initial_odometer_origin VARCHAR(50),
  ADD COLUMN final_odometer_origin VARCHAR(50),
  ADD COLUMN calculated_at TIMESTAMP,
  ADD COLUMN confirmed_at TIMESTAMP;

UPDATE monthly_odometer_closing
SET calculated_at = CURRENT_TIMESTAMP,
    confirmed_at = CURRENT_TIMESTAMP
WHERE confirmed_at IS NULL;
```

- [ ] **Step 4: implementar comando de confirmação**

```java
public record ConfirmCommand(
    UUID vehicleId,
    YearMonth month,
    boolean manualAdjustment,
    BigDecimal confirmedInitialOdometer,
    BigDecimal confirmedFinalOdometer,
    BigDecimal confirmedProfessionalKilometers,
    String adjustmentReason
) {}
```

O serviço deve recalcular a prévia no POST, rejeitar `hasBlockingAlerts()`, comparar confirmado versus inferido e exigir justificativa quando qualquer valor divergir.

- [ ] **Step 5: persistir valor inferido e confirmado**

A entidade deverá guardar ambos. Valores derivados finais são sempre calculados no servidor:

```text
total = confirmedFinal - confirmedInitial
personal = total - confirmedProfessional
professionalPercentage = total == 0 ? 0 : confirmedProfessional / total
manualAdjustment = algum valor confirmado difere do inferido
```

Não confiar em `total`, `personal` ou percentual enviados pelo navegador.

- [ ] **Step 6: atualizar o odômetro central após confirmação**

```java
odometerService.registerReading(
    vehicleId,
    confirmedFinalOdometer,
    month.atEndOfMonth().atTime(23, 59, 59),
    OdometerReadingSource.MONTHLY_CLOSING,
    closing.getId()
);
```

Para o mês corrente, usar `LocalDateTime.now()` se ainda não chegou ao fim do mês.

- [ ] **Step 7: construir tela de prévia e correção**

A tela deve exibir:

```text
odômetro inicial + origem
odômetro final + origem
total, profissional, pessoal, percentual
quantidade de dias, turnos e abastecimentos considerados
alertas separados em bloqueios e avisos
botão Salvar fechamento
botão Corrigir valores
```

Campos começam `readonly`. O script remove `readonly`, marca `manualAdjustment=true` e torna justificativa obrigatória ao clicar em **Corrigir valores**.

- [ ] **Step 8: executar testes web e domínio**

Run: `./mvnw -Dtest=MonthlyMileageClosingWebTest,MonthlyMileageInferenceServiceTest,ExpenseAllocationServiceTest test`

Expected: PASS.

- [ ] **Step 9: commit**

```bash
git add src/main/resources/db/migration/V7__add_odometer_traceability_and_inferred_closing.sql \
        src/main/java/dev/harrison/rendacomcarro/expense \
        src/main/resources/templates/mileage-closings \
        src/main/resources/static/js/mileage-closing-form.js \
        src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageClosingWebTest.java
git commit -m "feat: add automatic monthly mileage closing"
```

---

### Task 7: Aceite integrado, documentação e CI final

**Files:**
- Modify: `src/test/java/dev/harrison/rendacomcarro/AcceptanceFlowTest.java`
- Modify: `docs/architecture.md`
- Modify: `docs/mvp-acceptance-checklist.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: todos os fluxos anteriores.
- Produces: evidência automatizada do refinamento completo.

- [ ] **Step 1: ampliar o teste de aceite**

Fluxo obrigatório:

```text
criar veículo
abrir dia e turno
fechar turno e dia
registrar abastecimento
criar gasto misto e verificar rótulo português
abrir prévia mensal
confirmar valores automáticos
verificar rateio e odômetro atual
cadastrar leitura histórica menor e verificar que veículo não regride
```

- [ ] **Step 2: executar suíte completa**

Run: `./mvnw test`

Expected: BUILD SUCCESS, todos os testes verdes.

- [ ] **Step 3: empacotar aplicação**

Run: `./mvnw -DskipTests package`

Expected: `target/renda-com-carro-*.jar` criado.

- [ ] **Step 4: validar Compose e ARM64 pela GitHub Actions**

A CI existente deve continuar executando:

```text
Maven tests
Maven package
credential check
linux/arm64 image build
production stack startup
healthcheck
backup and isolated restore
```

Expected: ambos os jobs `test` e `ops-smoke` em `success`.

- [ ] **Step 5: atualizar documentação**

Registrar que:

```text
enums são persistidos em inglês e apresentados por getLabel()
odômetro atual tem origem/data rastreáveis
fechamento mensal é inferido e ajustes exigem justificativa
leituras históricas não reduzem o veículo
```

- [ ] **Step 6: commit final**

```bash
git add src/test/java/dev/harrison/rendacomcarro/AcceptanceFlowTest.java \
        docs/architecture.md docs/mvp-acceptance-checklist.md README.md
git commit -m "test: verify automatic mileage closing acceptance flow"
```

- [ ] **Step 7: abrir PR para `main`**

Título: `feat: improve labels and automate mileage closing`

O PR deve listar os critérios de aceite e só ser marcado como pronto após a CI completa ficar verde.
