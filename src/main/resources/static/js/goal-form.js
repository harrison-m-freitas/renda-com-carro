import { initializeLocalizedInputs } from './localized-inputs.js';
import { parseLocalizedDecimal } from './localized-input-formatters.js';
import { initializeGoalWorkloadPlanner } from './goal-workload-planner.js';

const form = document.getElementById('goal-form');

if (form) {
  initializeLocalizedInputs(form);

  const month = form.querySelector('[name="month"]');
  const personalGoal = form.querySelector('[name="personalNetGoal"]');
  const operationalGoal = form.querySelector('[name="operationalGoal"]');
  const plannedDates = form.querySelector('[name="plannedDates"]');
  const datePicker = form.querySelector('[data-planned-date-picker]');
  const addButton = form.querySelector('[data-add-planned-date]');
  const chips = form.querySelector('[data-planned-date-chips]');

  function normalizedDates() {
    return Array.from(new Set(String(plannedDates?.value ?? '')
      .split(/[,;\s]+/)
      .map((value) => value.trim())
      .filter(Boolean)))
      .sort();
  }

  function setDates(dates) {
    if (plannedDates) plannedDates.value = dates.join(', ');
    renderChips();
    refreshSummary();
    plannedDates?.dispatchEvent(new Event('input', { bubbles: true }));
  }

  function addDate() {
    const value = datePicker?.value;
    if (!value) return;
    if (month?.value && !value.startsWith(`${month.value}-`)) {
      datePicker.setCustomValidity('O dia precisa pertencer ao mês selecionado.');
      datePicker.reportValidity();
      return;
    }
    const date = new Date(`${value}T12:00:00`);
    if (date.getDay() === 0) {
      datePicker.setCustomValidity('Domingos não podem ser planejados.');
      datePicker.reportValidity();
      return;
    }
    datePicker.setCustomValidity('');
    setDates([...normalizedDates(), value]
      .filter((item, index, all) => all.indexOf(item) === index)
      .sort());
    datePicker.value = '';
  }

  function renderChips() {
    if (!chips) return;
    chips.replaceChildren();
    for (const value of normalizedDates()) {
      const chip = document.createElement('span');
      chip.className = 'badge rounded-pill text-bg-light d-inline-flex align-items-center gap-2';
      const label = document.createElement('span');
      label.textContent = formatDate(value);
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'btn-close';
      remove.setAttribute('aria-label', `Remover ${formatDate(value)}`);
      remove.addEventListener('click', () => {
        setDates(normalizedDates().filter((date) => date !== value));
      });
      chip.append(label, remove);
      chips.append(chip);
    }
  }

  function refreshContext() {
    form.dataset.draftContextKey = month?.value ? `month:${month.value}` : '';
    if (datePicker && month?.value) {
      datePicker.min = `${month.value}-01`;
      const [year, monthNumber] = month.value.split('-').map(Number);
      const end = new Date(year, monthNumber, 0).getDate();
      datePicker.max = `${month.value}-${String(end).padStart(2, '0')}`;
    }
    refreshSummary();
  }

  function refreshSummary() {
    setText('[data-goal-summary-month]', month?.value || '—');
    setText(
      '[data-goal-summary-operational]',
      formatMoney(parseLocalizedDecimal(operationalGoal?.value) ?? 0)
    );
    setText('[data-goal-summary-days]', String(normalizedDates().length));
  }

  function setText(selector, value) {
    const element = form.querySelector(selector);
    if (element) element.textContent = value;
  }

  addButton?.addEventListener('click', addDate);
  datePicker?.addEventListener('change', () => datePicker.setCustomValidity(''));
  month?.addEventListener('change', refreshContext);
  [personalGoal, operationalGoal, plannedDates].forEach((field) => {
    field?.addEventListener('input', () => {
      renderChips();
      refreshSummary();
    });
  });

  document.addEventListener('guided-form:before-save', (event) => {
    if (event.detail.form !== form) return;
    event.detail.contextKey = month?.value ? `month:${month.value}` : '';
    event.detail.payload.plannedDates = normalizedDates().join(',');
  });

  document.addEventListener('guided-form:restored', (event) => {
    if (event.detail.form !== form) return;
    refreshContext();
    renderChips();
    refreshSummary();
  });

  refreshContext();
  renderChips();
  refreshSummary();
  initializeGoalWorkloadPlanner(form, document);
}

function formatMoney(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(Number(value) || 0);
}

function formatDate(value) {
  const date = new Date(`${value}T12:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('pt-BR').format(date);
}
