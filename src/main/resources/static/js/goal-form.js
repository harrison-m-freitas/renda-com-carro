import { initializeLocalizedInputs } from './localized-inputs.js';
import { parseLocalizedDecimal } from './localized-input-formatters.js';
import { initializeGoalMonthPicker, formatGoalMonth } from './goal-month-picker.js';
import { initializeGoalCalendarPicker } from './goal-calendar-picker.js';
import { initializeGoalWorkloadPlanner } from './goal-workload-planner.js';
import { initializeGoalFinancialPlanner } from './goal-financial-planner.js';
import { initializeGoalHelp } from './goal-help.js';

const form = document.getElementById('goal-form');

if (form) {
  initializeLocalizedInputs(form);
  initializeGoalHelp(form, document);
  form.classList.add('goal-form--enhanced');

  const month = form.querySelector('[name="month"]');
  const operationalGoal = form.querySelector('[name="operationalGoal"]');
  const plannedDates = form.querySelector('[name="plannedDates"]');

  const errorSummary = form.querySelector('[data-form-error-summary]');

  form.querySelectorAll('.invalid-feedback').forEach((feedback) => {
    if (!feedback.id || !feedback.textContent?.trim()) return;
    form.querySelectorAll(`[aria-describedby~="${feedback.id}"]`).forEach((field) => {
      field.setAttribute('aria-invalid', 'true');
    });
  });
  errorSummary?.focus();

  const monthPicker = initializeGoalMonthPicker(form, document);
  const calendar = initializeGoalCalendarPicker(form, document);
  const workload = initializeGoalWorkloadPlanner(form, document);
  const financial = initializeGoalFinancialPlanner(form, {
    documentObject: document,
    workload,
  });

  const normalizedDates = () => [...new Set(String(plannedDates?.value ?? '')
    .split(/[,;\s]+/)
    .map((value) => value.trim())
    .filter(Boolean))]
    .sort();

  const setText = (selector, value) => {
    const element = form.querySelector(selector);
    if (element) element.textContent = value;
  };

  const formatMoney = (value) => new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number(value) || 0);

  const refreshSummary = () => {
    setText('[data-goal-summary-month]', formatGoalMonth(month?.value) || '—');
    setText(
      '[data-goal-summary-operational]',
      formatMoney(parseLocalizedDecimal(operationalGoal?.value) ?? 0),
    );
    setText('[data-goal-summary-days]', String(normalizedDates().length));
    workload?.refresh?.();
    financial?.refreshRates?.();
  };

  const refreshContext = () => {
    form.dataset.draftContextKey = month?.value ? `month:${month.value}` : '';
    monthPicker.refresh();
    calendar.refresh();
    refreshSummary();
  };

  month?.addEventListener('change', refreshContext);
  plannedDates?.addEventListener('input', refreshSummary);
  operationalGoal?.addEventListener('input', refreshSummary);

  document.addEventListener('guided-form:before-save', (event) => {
    if (event.detail.form !== form) return;
    event.detail.contextKey = month?.value ? `month:${month.value}` : '';
    event.detail.payload.plannedDates = normalizedDates().join(',');
  });

  document.addEventListener('guided-form:restored', (event) => {
    if (event.detail.form !== form) return;
    monthPicker.refresh();
    calendar.refresh();
    financial.refresh();
    refreshContext();
  });

  refreshContext();
}
