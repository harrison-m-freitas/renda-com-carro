const ISO_MONTH = /^(\d{4})-(\d{2})$/;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

const parseMonth = (value) => {
  const match = ISO_MONTH.exec(String(value ?? ''));
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  if (month < 1 || month > 12) return null;
  return { year, month };
};

const isoDate = (year, month, day) =>
  `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

export const serializePlannedDates = (dates) => [...new Set(
  Array.from(dates ?? []).map(String).filter((date) => ISO_DATE.test(date)),
)].sort().join(',');

export const formatGoalCalendarDate = (value) => {
  if (!ISO_DATE.test(String(value ?? ''))) return String(value ?? '');
  const [year, month, day] = value.split('-').map(Number);
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'full',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, day)));
};

export const buildGoalCalendarDays = (monthValue) => {
  const parsed = parseMonth(monthValue);
  if (!parsed) return [];
  const { year, month } = parsed;
  const count = new Date(Date.UTC(year, month, 0)).getUTCDate();
  const firstWeekdaySundayZero = new Date(Date.UTC(year, month - 1, 1)).getUTCDay();
  const firstWeekdayMondayZero = (firstWeekdaySundayZero + 6) % 7;
  const cells = Array.from({ length: firstWeekdayMondayZero }, () => ({
    iso: null,
    day: null,
    weekday: null,
    disabled: true,
  }));
  for (let day = 1; day <= count; day += 1) {
    const date = new Date(Date.UTC(year, month - 1, day));
    const weekday = date.getUTCDay();
    cells.push({
      iso: isoDate(year, month, day),
      day,
      weekday,
      disabled: weekday === 0,
    });
  }
  while (cells.length % 7 !== 0) {
    cells.push({ iso: null, day: null, weekday: null, disabled: true });
  }
  return cells;
};

export const datesForGoalWeekdays = (monthValue, weekdays) => {
  const allowed = new Set(Array.from(weekdays ?? []).map(Number));
  return buildGoalCalendarDays(monthValue)
    .filter((day) => day.iso && !day.disabled && allowed.has(day.weekday))
    .map((day) => day.iso);
};

export function initializeGoalCalendarPicker(root, documentObject = globalThis.document) {
  const container = root?.querySelector?.('[data-goal-calendar]');
  const monthField = root?.querySelector?.('[name="month"]');
  const datesField = root?.querySelector?.('[name="plannedDates"]');
  if (!container || !monthField || !datesField) {
    return { refresh() {}, dates: () => [], destroy() {} };
  }

  const grid = container.querySelector('[data-goal-calendar-grid]') || container;
  const live = root.querySelector('[data-goal-calendar-live]');
  const weekdayButtons = Array.from(root.querySelectorAll('[data-goal-weekday]'));
  const presetButtons = Array.from(root.querySelectorAll('[data-goal-calendar-preset]'));

  const fallbackContainer = root.querySelector(
    '[data-goal-calendar-fallback]'
  );

  const fallbackWasHidden = Boolean(fallbackContainer?.hidden);
  const fallbackWasRequired = Boolean(datesField.required);

  if (fallbackContainer) {
    fallbackContainer.hidden = true;
  } else {
    datesField.hidden = true;
  }

  datesField.required = false;

  const selected = new Set(serializePlannedDates(
    String(datesField.value ?? '').split(/[,;\s]+/),
  ).split(',').filter(Boolean));

  const announce = () => {
    if (!live) return;
    live.textContent = `${selected.size} ${selected.size === 1 ? 'dia selecionado' : 'dias selecionados'}.`;
  };

  const commit = () => {
    datesField.value = serializePlannedDates(selected);
    datesField.dispatchEvent(new Event('input', { bubbles: true }));
    announce();
  };

  const toggle = (value) => {
    if (selected.has(value)) selected.delete(value);
    else selected.add(value);
    commit();
    render();
  };

  const setDates = (values) => {
    selected.clear();
    values.forEach((value) => selected.add(value));
    commit();
    render();
  };

  const moveFocus = (button, offset) => {
    const buttons = Array.from(grid.querySelectorAll('button:not([disabled])'));
    const index = buttons.indexOf(button);
    if (index < 0 || !buttons.length) return;
    buttons[(index + offset + buttons.length) % buttons.length]?.focus();
  };

  const render = () => {
    if (!documentObject?.createElement) return;
    grid.replaceChildren();
    const headers = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
    headers.forEach((label) => {
      const header = documentObject.createElement('span');
      header.className = 'goal-calendar__weekday';
      header.textContent = label;
      header.setAttribute('role', 'columnheader');
      grid.append(header);
    });
    buildGoalCalendarDays(monthField.value).forEach((cell) => {
      if (!cell.iso) {
        const spacer = documentObject.createElement('span');
        spacer.className = 'goal-calendar__day goal-calendar__day--empty';
        spacer.setAttribute('aria-hidden', 'true');
        grid.append(spacer);
        return;
      }
      const button = documentObject.createElement('button');
      button.type = 'button';
      button.className = 'goal-calendar__day';
      button.textContent = String(cell.day);
      button.dataset.goalDate = cell.iso;
      button.disabled = cell.disabled;
      button.setAttribute('role', 'gridcell');
      button.setAttribute('aria-label', cell.disabled
        ? `${formatGoalCalendarDate(cell.iso)} — indisponível para planejamento`
        : formatGoalCalendarDate(cell.iso));
      button.setAttribute('aria-pressed', String(selected.has(cell.iso)));
      button.setAttribute('aria-selected', String(selected.has(cell.iso)));
      button.addEventListener('click', () => toggle(cell.iso));
      button.addEventListener('keydown', (event) => {
        const offsets = { ArrowRight: 1, ArrowLeft: -1, ArrowDown: 7, ArrowUp: -7 };
        if (offsets[event.key]) moveFocus(button, offsets[event.key]);
        else if (event.key === 'Home') grid.querySelector('button:not([disabled])')?.focus();
        else if (event.key === 'End') Array.from(grid.querySelectorAll('button:not([disabled])')).at(-1)?.focus();
        else return;
        event.preventDefault();
      });
      grid.append(button);
    });
    weekdayButtons.forEach((button) => {
      const weekday = Number(button.dataset.goalWeekday);
      const matching = datesForGoalWeekdays(monthField.value, [weekday]);
      button.setAttribute('aria-pressed', String(
        matching.length > 0 && matching.every((date) => selected.has(date)),
      ));
    });
  };

  const onMonthChange = () => {
    for (const date of Array.from(selected)) {
      if (!date.startsWith(`${monthField.value}-`)) selected.delete(date);
    }
    commit();
    render();
  };

  weekdayButtons.forEach((button) => button.addEventListener('click', () => {
    const weekday = Number(button.dataset.goalWeekday);
    const matching = datesForGoalWeekdays(monthField.value, [weekday]);
    const allSelected = matching.length > 0 && matching.every((date) => selected.has(date));
    matching.forEach((date) => allSelected ? selected.delete(date) : selected.add(date));
    commit();
    render();
  }));
  presetButtons.forEach((button) => button.addEventListener('click', () => {
    const preset = button.dataset.goalCalendarPreset;
    if (preset === 'clear') setDates([]);
    if (preset === 'weekdays') setDates(datesForGoalWeekdays(monthField.value, [1, 2, 3, 4, 5]));
    if (preset === 'monday-saturday') setDates(datesForGoalWeekdays(monthField.value, [1, 2, 3, 4, 5, 6]));
  }));
  monthField.addEventListener('change', onMonthChange);
  render();
  announce();

  return {
    refresh: render,
    dates: () => Array.from(selected).sort(),
    destroy() {
      monthField.removeEventListener('change', onMonthChange);
      if (fallbackContainer) {
        fallbackContainer.hidden = fallbackWasHidden;
      } else {
        datesField.hidden = false;
      }

      datesField.required = fallbackWasRequired;
    },
  };
}
