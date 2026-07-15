import { parseLocalizedDecimal } from './localized-input-formatters.js';

const money = (value) => new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
}).format(Number(value) || 0);

const decimalInput = (value) => Number(value || 0).toLocaleString('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export const calculateGoalRates = ({
  operationalGoal,
  personalGoal,
  dayCount,
  totalMinutes,
}) => ({
  operationalPerDay: dayCount > 0 ? Number(operationalGoal) / dayCount : null,
  operationalPerHour: totalMinutes > 0 ? Number(operationalGoal) * 60 / totalMinutes : null,
  personalPerDay: dayCount > 0 ? Number(personalGoal) / dayCount : null,
  personalPerHour: totalMinutes > 0 ? Number(personalGoal) * 60 / totalMinutes : null,
});

export const calculateOperationalSuggestion = (personalGoal, professionalCosts) =>
  Number(personalGoal || 0) + Number(professionalCosts || 0);

export const buildOperationalSuggestionUrl = (month, goalId = null) => {
  const params = new URLSearchParams();
  params.set('month', String(month ?? ''));
  if (goalId) params.set('goalId', String(goalId));
  return `/goals/operational-suggestion?${params.toString()}`;
};

export function initializeGoalFinancialPlanner(form, options = {}) {
  if (!form) return { refresh() {}, refreshRates() {}, destroy() {} };
  const documentObject = options.documentObject ?? globalThis.document;
  const fetchImpl = options.fetchImpl ?? globalThis.fetch?.bind(globalThis);
  const month = form.querySelector('[name="month"]');
  const personal = form.querySelector('[name="personalNetGoal"]');
  const operational = form.querySelector('[name="operationalGoal"]');
  const dates = form.querySelector('[name="plannedDates"]');
  const hours = form.querySelector('[name="workloadHours"]');
  const minutes = form.querySelector('[name="workloadMinutes"]');
  const periodicityFields = Array.from(form.querySelectorAll('[name="workloadPeriodicity"]'));
  const breakdown = form.querySelector('[data-goal-financial-breakdown]');
  const loading = form.querySelector('[data-goal-financial-loading]');
  const error = form.querySelector('[data-goal-financial-error]');
  const suggestedTotal = form.querySelector('[data-goal-suggested-total]');
  const applyButton = form.querySelector('[data-apply-goal-suggestion]');
  let requestId = 0;
  let controller = null;
  let suggestion = null;
  let applyingSuggestion = false;

  const selectedDates = () => [...new Set(String(dates?.value ?? '')
    .split(/[,;\s]+/)
    .map((value) => value.trim())
    .filter(Boolean))];

  const workloadMinutes = () => {
    const calculated = options.workload?.getResult?.();
    if (calculated?.status === 'ready' && Number.isFinite(calculated.totalMinutes)) {
      return calculated.totalMinutes;
    }
    const sourceMinutes = Math.max(0, Number.parseInt(hours?.value || '0', 10) || 0) * 60
      + Math.max(0, Number.parseInt(minutes?.value || '0', 10) || 0);
    const periodicity = periodicityFields.find((field) => field.checked)?.value || 'MONTHLY';
    const dayCount = selectedDates().length;
    if (periodicity === 'DAILY') return sourceMinutes * dayCount;
    if (periodicity === 'WEEKLY') {
      const activeWeeks = new Set(selectedDates().map((value) => {
        const date = new Date(`${value}T00:00:00Z`);
        const weekday = date.getUTCDay() || 7;
        date.setUTCDate(date.getUTCDate() - weekday + 1);
        return date.toISOString().slice(0, 10);
      })).size;
      return sourceMinutes * activeWeeks;
    }
    return sourceMinutes;
  };

  const setText = (selector, value) => {
    const element = form.querySelector(selector);
    if (element) element.textContent = value;
  };

  const refreshRates = () => {
    const rates = calculateGoalRates({
      operationalGoal: parseLocalizedDecimal(operational?.value) ?? 0,
      personalGoal: parseLocalizedDecimal(personal?.value) ?? 0,
      dayCount: selectedDates().length,
      totalMinutes: workloadMinutes(),
    });
    setText('[data-goal-rate-operational-day]', rates.operationalPerDay == null ? '—' : money(rates.operationalPerDay));
    setText('[data-goal-rate-operational-hour]', rates.operationalPerHour == null ? '—' : money(rates.operationalPerHour));
    setText('[data-goal-rate-personal-day]', rates.personalPerDay == null ? '—' : money(rates.personalPerDay));
    setText('[data-goal-rate-personal-hour]', rates.personalPerHour == null ? '—' : money(rates.personalPerHour));
  };

  const appendLine = (container, label, amount, className = '') => {
    if (!container || !documentObject?.createElement) return;
    const line = documentObject.createElement('div');
    line.className = `goal-financial-breakdown__line ${className}`.trim();
    const text = documentObject.createElement('span');
    text.textContent = label;
    const value = documentObject.createElement('strong');
    value.textContent = money(amount);
    line.append(text, value);
    container.append(line);
  };

  const renderSuggestion = (payload) => {
    suggestion = payload;
    const personalValue = parseLocalizedDecimal(personal?.value) ?? 0;
    const total = calculateOperationalSuggestion(personalValue, Number(payload.professionalCostsTotal));
    if (suggestedTotal) suggestedTotal.textContent = money(total);
    if (applyButton) applyButton.disabled = false;
    if (!breakdown) return;
    breakdown.replaceChildren();
    appendLine(breakdown, 'Meta líquida pessoal', personalValue);
    appendLine(breakdown, 'Gastos profissionais do mês', Number(payload.currentExpenses));
    appendLine(breakdown, 'Gastos profissionais pendentes', Number(payload.overdueProfessionalExpenses), 'is-overdue');
    appendLine(breakdown, 'Obrigações do veículo no mês', Number(payload.currentVehicleObligations));
    appendLine(breakdown, 'Obrigações vencidas do veículo', Number(payload.overdueVehicleObligations), 'is-overdue');
    appendLine(breakdown, 'Meta operacional sugerida', total, 'is-total');
    const details = documentObject.createElement('div');
    details.className = 'goal-financial-breakdown__items';
    (payload.items ?? []).forEach((item) => appendLine(
      details,
      `${item.label} · ${item.vehicleLabel}`,
      Number(item.amount),
      item.overdue ? 'is-overdue' : '',
    ));
    breakdown.append(details);
    (payload.warnings ?? []).forEach((warning) => {
      const warningElement = documentObject.createElement('p');
      warningElement.className = 'alert alert-warning py-2 mt-2 mb-0';
      warningElement.textContent = warning;
      breakdown.append(warningElement);
    });
  };

  const loadSuggestion = async () => {
    if (!fetchImpl || !month?.value) {
      suggestion = null;
      if (applyButton) applyButton.disabled = true;
      if (breakdown) breakdown.replaceChildren();
      return null;
    }
    const currentId = ++requestId;
    controller?.abort();
    controller = new AbortController();
    if (loading) loading.hidden = false;
    if (error) error.hidden = true;
    try {
      const response = await fetchImpl(buildOperationalSuggestionUrl(month.value, form.dataset.goalId), {
        signal: controller.signal,
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const payload = await response.json();
      if (currentId !== requestId) return null;
      renderSuggestion(payload);
      refreshRates();
      return payload;
    } catch (caught) {
      if (caught?.name === 'AbortError' || currentId !== requestId) return null;
      if (error) {
        error.hidden = false;
        error.textContent = 'Não foi possível atualizar a sugestão. Você ainda pode preencher a meta manualmente.';
      }
      return null;
    } finally {
      if (currentId === requestId && loading) loading.hidden = true;
    }
  };

  applyButton?.addEventListener('click', () => {
    if (!suggestion || !operational) return;
    const total = calculateOperationalSuggestion(
      parseLocalizedDecimal(personal?.value) ?? 0,
      Number(suggestion.professionalCostsTotal),
    );
    applyingSuggestion = true;
    operational.value = decimalInput(total);
    operational.dataset.userEdited = 'false';
    operational.dispatchEvent(new Event('input', { bubbles: true }));
    applyingSuggestion = false;
    refreshRates();
  });
  operational?.addEventListener('input', (event) => {
    if (!applyingSuggestion && (event.isTrusted || operational.dataset.userEdited !== 'false')) {
      operational.dataset.userEdited = 'true';
    }
    refreshRates();
  });
  personal?.addEventListener('input', () => {
    if (suggestion) renderSuggestion(suggestion);
    refreshRates();
  });
  [month, dates, hours, minutes].forEach((field) => {
    field?.addEventListener('change', field === month ? loadSuggestion : refreshRates);
    field?.addEventListener('input', field === month ? loadSuggestion : refreshRates);
  });
  periodicityFields.forEach((field) => field.addEventListener('change', refreshRates));
  loadSuggestion();
  refreshRates();

  return {
    refresh: loadSuggestion,
    refreshRates,
    destroy() { controller?.abort(); },
  };
}
