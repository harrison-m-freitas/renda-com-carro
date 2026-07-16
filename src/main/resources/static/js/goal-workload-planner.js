import {
  calculateGoalWorkload,
  formatWorkloadDuration
} from './goal-workload-calculator.js';

const PERIOD_LABELS = {
  DAILY: 'por dia',
  WEEKLY: 'por semana',
  MONTHLY: 'por mês'
};

const normalizedDates = (field) => [...new Set(String(field?.value ?? '')
  .split(/[,;\s]+/)
  .map((value) => value.trim())
  .filter(Boolean))]
  .sort();

const numericValue = (field) => {
  const parsed = Number.parseInt(String(field?.value ?? ''), 10);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
};

export const initializeGoalWorkloadPlanner = (
  form,
  documentObject = document
) => {
  if (!form) return null;

  const periodicityFields = Array.from(
    form.querySelectorAll('[name="workloadPeriodicity"]')
  );
  const hours = form.querySelector('[name="workloadHours"]');
  const minutes = form.querySelector('[name="workloadMinutes"]');
  const month = form.querySelector('[name="month"]');
  const plannedDates = form.querySelector('[name="plannedDates"]');
  const summary = form.querySelector('[data-workload-summary]');
  const reviewSummary = form.querySelector('[data-goal-summary-workload]');

  if (!hours || !minutes) return null;
  let lastResult = null;

  const selectedPeriodicity = () => periodicityFields
    .find((field) => field.checked)?.value
    || form.querySelector('[name="workloadPeriodicity"]')?.value
    || 'MONTHLY';

  const renderPending = (sourceText) => {
    summary?.replaceChildren();

    if (reviewSummary) {
      reviewSummary.textContent = sourceText;
    }
  };

  const renderReady = (_periodicity, _enteredMinutes, result) => {
    summary?.replaceChildren();

    if (reviewSummary) {
      reviewSummary.textContent =
        `${formatWorkloadDuration(result.totalMinutes)} no mês`;
    }
  };

  const refresh = () => {
    const periodicity = selectedPeriodicity();
    const enteredMinutes = numericValue(hours) * 60 + numericValue(minutes);
    const sourceText = `${formatWorkloadDuration(enteredMinutes)} ${PERIOD_LABELS[periodicity]}`;

    if (enteredMinutes <= 0) {
      renderPending(sourceText, 'Informe uma duração maior que zero.');
      lastResult = null;
      return null;
    }

    try {
      const result = calculateGoalWorkload({
        month: month?.value,
        periodicity,
        enteredMinutes,
        plannedDates: normalizedDates(plannedDates)
      });
      if (result.status === 'pending') {
        renderPending(
          sourceText,
          'Selecione os dias planejados para calcular a distribuição.'
        );
        lastResult = result;
        return result;
      }
      renderReady(periodicity, enteredMinutes, result);
      lastResult = result;
      return result;
    } catch (error) {
      renderPending(sourceText, error.message || 'Não foi possível calcular a jornada.');
      lastResult = null;
      return null;
    }
  };

  periodicityFields.forEach((field) => field.addEventListener('change', refresh));
  [hours, minutes, month, plannedDates].forEach((field) => {
    field?.addEventListener('input', refresh);
    field?.addEventListener('change', refresh);
  });

  documentObject.addEventListener?.('guided-form:restored', (event) => {
    if (event.detail?.form === form) refresh();
  });

  refresh();
  return { refresh, getResult: () => lastResult };
};
