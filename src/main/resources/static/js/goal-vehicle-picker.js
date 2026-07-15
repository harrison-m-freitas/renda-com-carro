export const selectedVehicleValues = (fields) =>
  Array.from(fields ?? []).filter((field) => field.checked).map((field) => String(field.value));

export const selectedVehicleLabels = (fields) =>
  Array.from(fields ?? [])
    .filter((field) => field.checked)
    .map((field) => field.dataset?.vehicleLabel || String(field.value));

export const vehicleSelectionLabel = (labels) => {
  if (!labels?.length) return 'Selecione pelo menos um veículo';
  if (labels.length === 1) return labels[0];
  return `${labels.length} veículos selecionados`;
};

export const toggleVehicleField = (field, EventClass = Event) => {
  field.checked = !field.checked;
  field.dispatchEvent(new EventClass('change', { bubbles: true }));
};

export function initializeGoalVehiclePicker(root, documentObject = globalThis.document) {
  const picker = root?.querySelector?.('[data-goal-vehicle-picker]');
  if (!picker) return { refresh() {}, destroy() {} };
  const fields = Array.from(picker.querySelectorAll('[name="vehicleIds"]'));
  const trigger = picker.querySelector('[data-goal-vehicle-trigger]');
  const panel = picker.querySelector('[data-goal-vehicle-dialog]');
  const summary = picker.querySelector('[data-goal-vehicle-label]');
  const chips = picker.querySelector('[data-goal-vehicle-chips]');
  let open = false;
  if (trigger) trigger.hidden = false;
  if (panel) panel.hidden = true;

  const setOpen = (value, restoreFocus = false) => {
    open = Boolean(value);
    if (panel) panel.hidden = !open;
    trigger?.setAttribute('aria-expanded', String(open));
    if (open) fields[0]?.focus?.();
    else if (restoreFocus) trigger?.focus?.();
  };

  const renderChips = () => {
    if (!chips || !documentObject?.createElement) return;
    chips.replaceChildren();
    fields.filter((field) => field.checked).forEach((field) => {
      const chip = documentObject.createElement('span');
      chip.className = 'goal-picker__chip';
      chip.textContent = field.dataset?.vehicleLabel || field.value;
      const remove = documentObject.createElement('button');
      remove.type = 'button';
      remove.setAttribute('aria-label', `Remover ${chip.textContent}`);
      remove.textContent = '×';
      remove.addEventListener('click', () => {
        if (field.checked) toggleVehicleField(field);
      });
      chip.append(remove);
      chips.append(chip);
    });
  };

  const refresh = () => {
    const labels = selectedVehicleLabels(fields);
    if (summary) summary.textContent = vehicleSelectionLabel(labels);
    renderChips();
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
  fields.forEach((field) => field.addEventListener('change', refresh));
  documentObject?.addEventListener?.('click', onDocumentClick);
  documentObject?.addEventListener?.('keydown', onDocumentKeydown);
  refresh();

  return {
    refresh,
    destroy() {
      documentObject?.removeEventListener?.('click', onDocumentClick);
      documentObject?.removeEventListener?.('keydown', onDocumentKeydown);
    },
  };
}
