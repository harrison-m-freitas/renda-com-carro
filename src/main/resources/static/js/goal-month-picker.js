const MONTH_NAME = new Intl.DateTimeFormat('pt-BR', {
  month: 'long',
  timeZone: 'UTC',
});

export const formatGoalMonth = (value) => {
  const match = /^(\d{4})-(\d{2})$/.exec(String(value ?? ''));
  if (!match) return '';
  const year = Number(match[1]);
  const month = Number(match[2]);
  if (month < 1 || month > 12) return '';
  const raw = new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, 1)));
  return raw.charAt(0).toLocaleUpperCase('pt-BR') + raw.slice(1);
};

export const goalMonthValue = (year, zeroBasedMonth) =>
  `${Number(year)}-${String(Number(zeroBasedMonth) + 1).padStart(2, '0')}`;

export const prepareGoalMonthInput = (input) => {
  if (!input) return () => {};
  const wasHidden = Boolean(input.hidden);
  const wasRequired = Boolean(input.required);
  input.hidden = true;
  input.required = false;
  return () => {
    input.hidden = wasHidden;
    input.required = wasRequired;
  };
};

export const monthLabelsForYear = (year) => Array.from({ length: 12 }, (_, index) => {
  const raw = MONTH_NAME.format(new Date(Date.UTC(Number(year), index, 1)));
  return {
    value: goalMonthValue(year, index),
    label: raw.charAt(0).toLocaleUpperCase('pt-BR') + raw.slice(1),
  };
});

export function initializeGoalMonthPicker(root, documentObject = globalThis.document) {
  const picker = root?.querySelector?.('[data-goal-month-picker]');
  if (!picker) return { refresh() {}, destroy() {} };

  const input = picker.querySelector('[name="month"]');
  const trigger = picker.querySelector('[data-goal-month-trigger]');
  const dialog = picker.querySelector('[data-goal-month-dialog]');
  const grid = picker.querySelector('[data-goal-month-grid]');
  const yearLabel = picker.querySelector('[data-goal-month-year]');
  const previous = picker.querySelector('[data-goal-month-previous]');
  const next = picker.querySelector('[data-goal-month-next]');
  let displayYear = Number(String(input?.value ?? '').slice(0, 4)) || new Date().getFullYear();
  let open = false;

  const restoreInput = prepareGoalMonthInput(input);
  if (trigger) trigger.hidden = false;

  const setOpen = (value, restoreFocus = false) => {
    open = Boolean(value);
    if (dialog) dialog.hidden = !open;
    trigger?.setAttribute('aria-expanded', String(open));
    if (open) grid?.querySelector?.('[aria-selected="true"]')?.focus?.();
    else if (restoreFocus) trigger?.focus?.();
  };

  const select = (value) => {
    if (!input) return;
    input.value = value;
    input.dispatchEvent(new Event('change', { bubbles: true }));
    setOpen(false, true);
    refresh();
  };

  const render = () => {
    if (!grid || !documentObject?.createElement) return;
    grid.replaceChildren();
    if (yearLabel) yearLabel.textContent = String(displayYear);
    monthLabelsForYear(displayYear).forEach((option, index) => {
      const button = documentObject.createElement('button');
      button.type = 'button';
      button.className = 'goal-picker__month';
      button.textContent = option.label;
      button.dataset.monthValue = option.value;
      button.setAttribute('role', 'gridcell');
      button.setAttribute('aria-selected', String(input?.value === option.value));
      button.tabIndex = input?.value === option.value || (!input?.value && index === 0) ? 0 : -1;
      button.addEventListener('click', () => select(option.value));
      button.addEventListener('keydown', (event) => {
        const buttons = Array.from(grid.querySelectorAll('button'));
        const current = buttons.indexOf(button);
        const movements = { ArrowRight: 1, ArrowLeft: -1, ArrowDown: 3, ArrowUp: -3 };
        if (event.key === 'Home') buttons[0]?.focus();
        else if (event.key === 'End') buttons.at(-1)?.focus();
        else if (movements[event.key]) buttons[(current + movements[event.key] + 12) % 12]?.focus();
        else if (event.key === 'Escape') setOpen(false, true);
        else return;
        event.preventDefault();
      });
      grid.append(button);
    });
  };

  const refresh = () => {
    const label = formatGoalMonth(input?.value);
    const target = picker.querySelector('[data-goal-month-label]');
    if (target) target.textContent = label || 'Selecione o mês';
    const year = Number(String(input?.value ?? '').slice(0, 4));
    if (year) displayYear = year;
    render();
  };

  const onDocumentClick = (event) => {
    if (open && !picker.contains(event.target)) setOpen(false);
  };
  const onDocumentKeydown = (event) => {
    if (open && event.key === 'Escape') {
      event.preventDefault();
      setOpen(false, true);
    }
  };
  trigger?.addEventListener('click', () => setOpen(!open));
  previous?.addEventListener('click', () => { displayYear -= 1; render(); });
  next?.addEventListener('click', () => { displayYear += 1; render(); });
  input?.addEventListener('change', refresh);
  documentObject?.addEventListener?.('click', onDocumentClick);
  documentObject?.addEventListener?.('keydown', onDocumentKeydown);
  refresh();

  return {
    refresh,
    destroy() {
      restoreInput();
      if (trigger) trigger.hidden = true;
      documentObject?.removeEventListener?.('click', onDocumentClick);
      documentObject?.removeEventListener?.('keydown', onDocumentKeydown);
    },
  };
}
