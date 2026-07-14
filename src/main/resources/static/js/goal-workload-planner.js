import {
  calculateGoalWorkload,
  formatWorkloadDuration
} from './goal-workload-calculator.js';

const PERIOD_LABELS = {
  DAILY: 'por dia',
  WEEKLY: 'por semana',
  MONTHLY: 'por mês'
};

const WEEKDAY_LABELS = {
  1: 'seg',
  2: 'ter',
  3: 'qua',
  4: 'qui',
  5: 'sex',
  6: 'sáb',
  7: 'dom'
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

const createElement = (documentObject, tag, className, text = '') => {
  const element = documentObject.createElement(tag);
  if (className) element.className = className;
  if (text) element.textContent = text;
  return element;
};

const formatPattern = (pattern) => pattern
  .map((weekday) => WEEKDAY_LABELS[weekday] ?? String(weekday))
  .join(', ');

const createEquivalentBlock = (documentObject, label, minutes) => {
  const block = createElement(documentObject, 'div', 'col-sm-6');
  block.append(
    createElement(documentObject, 'div', 'text-secondary small', label),
    createElement(documentObject, 'div', 'fw-semibold', formatWorkloadDuration(minutes))
  );
  return block;
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

  if (!hours || !minutes || !summary) return null;

  const selectedPeriodicity = () => periodicityFields
    .find((field) => field.checked)?.value
    || form.querySelector('[name="workloadPeriodicity"]')?.value
    || 'MONTHLY';

  const renderPending = (sourceText, message) => {
    summary.replaceChildren();
    const source = createElement(documentObject, 'div', 'fw-semibold', sourceText);
    const help = createElement(documentObject, 'div', 'text-secondary mt-1', message);
    summary.append(source, help);
    if (reviewSummary) reviewSummary.textContent = sourceText;
  };

  const renderReady = (periodicity, enteredMinutes, result) => {
    summary.replaceChildren();

    const top = createElement(documentObject, 'div', 'd-flex flex-wrap justify-content-between gap-3');
    const sourceBlock = createElement(documentObject, 'div');
    sourceBlock.append(
      createElement(documentObject, 'div', 'text-secondary small', 'Jornada informada'),
      createElement(
        documentObject,
        'div',
        'fw-semibold',
        `${formatWorkloadDuration(enteredMinutes)} ${PERIOD_LABELS[periodicity]}`
      )
    );
    const totalBlock = createElement(documentObject, 'div', 'text-md-end');
    totalBlock.append(
      createElement(documentObject, 'div', 'text-secondary small', 'Total planejado no mês'),
      createElement(
        documentObject,
        'div',
        'h4 mb-0',
        formatWorkloadDuration(result.totalMinutes)
      )
    );
    top.append(sourceBlock, totalBlock);
    summary.append(top);

    const equivalents = createElement(documentObject, 'div', 'row g-2 mt-2');
    equivalents.append(
      createEquivalentBlock(
        documentObject,
        'Média por dia planejado',
        result.averageDailyMinutes
      ),
      createEquivalentBlock(
        documentObject,
        'Média por semana ativa',
        result.averageWeeklyMinutes
      )
    );
    summary.append(equivalents);

    const list = createElement(documentObject, 'div', 'mt-3 d-grid gap-2');
    result.weeks.forEach((week) => {
      const line = createElement(
        documentObject,
        'div',
        'd-flex flex-wrap justify-content-between gap-2 border-top pt-2'
      );
      const interval = `${formatDate(week.weekStart)}–${formatDate(week.weekEnd)}`;
      const count = `${week.selectedDays} ${week.selectedDays === 1 ? 'dia' : 'dias'}`;
      const left = createElement(documentObject, 'span', '', `${interval} · ${count}`);
      const right = createElement(
        documentObject,
        'strong',
        '',
        formatWorkloadDuration(week.allocatedMinutes)
      );
      line.append(left, right);
      list.append(line);

      if (week.expectedDays && week.inferredPattern.length > 0) {
        list.append(createElement(
          documentObject,
          'div',
          'small text-secondary',
          `Semana parcial calculada pela rotina: ${formatPattern(week.inferredPattern)}.`
        ));
      } else if (week.expectedDays === 5 && week.inferredPattern.length === 0) {
        list.append(createElement(
          documentObject,
          'div',
          'small text-secondary',
          'Semana parcial calculada com referência provisória de 5 dias.'
        ));
      }
    });
    summary.append(list);

    if (reviewSummary) {
      reviewSummary.textContent = `${formatWorkloadDuration(result.totalMinutes)} no mês`;
    }
  };

  const refresh = () => {
    const periodicity = selectedPeriodicity();
    const enteredMinutes = numericValue(hours) * 60 + numericValue(minutes);
    const sourceText = `${formatWorkloadDuration(enteredMinutes)} ${PERIOD_LABELS[periodicity]}`;

    if (enteredMinutes <= 0) {
      renderPending(sourceText, 'Informe uma duração maior que zero.');
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
        return result;
      }
      renderReady(periodicity, enteredMinutes, result);
      return result;
    } catch (error) {
      renderPending(sourceText, error.message || 'Não foi possível calcular a jornada.');
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
  return { refresh };
};

function formatDate(value) {
  const [year, month, day] = String(value).split('-');
  return year && month && day ? `${day}/${month}` : value;
}
