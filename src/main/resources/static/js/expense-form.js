import {
  formatLocalizedInputs,
  initializeLocalizedInputs
} from './localized-inputs.js';
import {
  createExpenseState,
  expenseDraftPayload,
  expenseSummary,
  formatMoneyFromCents,
  updateExpenseState,
  validateExpenseState
} from './expense-form-state.js';
import { civilDateInTimeZone } from './user-time-zone.js';
import { FormDraftClient } from './form-draft-client.js';

export function formatReferenceMonth(value) {
  const match = /^(\d{4})-(\d{2})$/.exec(String(value ?? ''));
  if (!match) return '—';
  const date = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, 1));
  const formatted = new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC'
  }).format(date);
  return formatted.charAt(0).toLocaleUpperCase('pt-BR') + formatted.slice(1);
}

export function buildAllocationPreviewUrl(vehicleId, competenceMonth) {
  const params = new URLSearchParams({ vehicleId, competenceMonth });
  return `/expenses/allocation-preview?${params.toString()}`;
}

export class AllocationPreviewCoordinator {
  constructor() {
    this.sequence = 0;
    this.current = null;
  }

  begin() {
    this.current?.controller.abort();
    const token = {
      id: ++this.sequence,
      controller: new AbortController()
    };
    token.signal = token.controller.signal;
    this.current = token;
    return token;
  }

  isCurrent(token) {
    return this.current === token && !token.signal.aborted;
  }

  cancel() {
    this.current?.controller.abort();
    this.current = null;
  }
}

export function setExpenseSubmitState(form, button, submitting) {
  if (submitting) {
    form?.setAttribute?.('aria-busy', 'true');
    if (button) {
      button.dataset.originalText ||= button.textContent;
      button.disabled = true;
      button.textContent = 'Salvando…';
    }
    return;
  }

  form?.removeAttribute?.('aria-busy');
  if (button) {
    button.disabled = false;
    if (button.dataset.originalText) button.textContent = button.dataset.originalText;
  }
}

const checkedValue = (form, name) => form
  .querySelector(`[name="${name}"]:checked`)?.value ?? '';

const formatCivilDate = (value) => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value ?? ''));
  return match ? `${match[3]}/${match[2]}/${match[1]}` : '—';
};

const formatKilometers = (value) => value === null || value === undefined
  ? '—'
  : `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(Number(value))} km`;

const formatPercentage = (value) => value === null || value === undefined
  ? '—'
  : `${new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number(value))}%`;

const setHiddenAndDisabled = (group, controls, hidden, required = false) => {
  if (group) group.hidden = hidden;
  controls.filter(Boolean).forEach((control) => {
    control.disabled = hidden;
    control.required = !hidden && required;
  });
};

const setText = (element, value) => {
  if (element) element.textContent = value;
};

const clientErrorElement = (field) => field?.id
  ? field.ownerDocument?.getElementById(`${field.id}-error`)
  : null;

const synchronizeFieldValidity = (field, message = '') => {
  if (!field) return;
  field.setCustomValidity?.(message);
  const invalid = Boolean(message) || field.classList?.contains('is-invalid');
  if (invalid) field.setAttribute?.('aria-invalid', 'true');
  else field.removeAttribute?.('aria-invalid');

  const group = field.closest?.('.input-group');
  group?.classList.toggle?.('is-invalid-group', invalid);

  const feedback = clientErrorElement(field);
  if (!feedback) return;
  if (message) {
    feedback.dataset.clientError = 'true';
    feedback.textContent = message;
    feedback.classList.add('d-block');
  } else if (feedback.dataset.clientError === 'true') {
    feedback.textContent = '';
    delete feedback.dataset.clientError;
  }
};

const replaceDraftPayload = (payload, replacement) => {
  Object.keys(payload).forEach((key) => delete payload[key]);
  Object.assign(payload, replacement);
};

export function initializeExpenseForm(
  documentObject = globalThis.document,
  windowObject = globalThis.window,
  { fetchImpl = globalThis.fetch?.bind(globalThis) } = {}
) {
  const form = documentObject?.querySelector?.('#expense-form');
  if (!form) return null;

  initializeLocalizedInputs(form);

  const field = (name) => form.querySelector(`[name="${name}"]`);
  const allFields = (name) => Array.from(form.querySelectorAll(`[name="${name}"]`));
  const fields = {
    vehicleId: field('vehicleId'),
    operationalDayId: field('operationalDayId'),
    shiftId: field('shiftId'),
    categoryId: field('categoryId'),
    expenseDate: field('expenseDate'),
    competenceMonth: field('competenceMonth'),
    paidDate: field('paidDate'),
    amount: field('amount'),
    professionalPercentagePercent: field('professionalPercentagePercent'),
    professionalFixedAmount: field('professionalFixedAmount'),
    adjustmentReason: field('adjustmentReason'),
    notes: field('notes')
  };
  const classificationFields = allFields('classification');
  const allocationFields = allFields('allocationMethod');
  const paymentFields = allFields('paymentStatus');

  const allocationGroup = form.querySelector('[data-allocation-group]');
  const percentageGroup = form.querySelector('[data-percentage-group]');
  const fixedGroup = form.querySelector('[data-fixed-group]');
  const reasonGroup = form.querySelector('[data-reason-group]');
  const paidDateGroup = form.querySelector('[data-paid-date-group]');
  const allocationPreviewRegion = form.querySelector('[data-allocation-preview]');
  const previewTitle = form.querySelector('[data-allocation-preview-title]');
  const previewMessage = form.querySelector('[data-allocation-preview-message]');
  const previewWarning = form.querySelector('[data-allocation-preview-warning]');
  const referenceLabel = form.querySelector('[data-reference-label]');
  const errorSummary = form.querySelector('[data-expense-error-summary]');
  const moreOptions = form.querySelector('[data-expense-more-options]');
  const statusRegion = form.querySelector('[data-expense-status]');
  const discardButton = form.querySelector('[data-expense-discard-draft]');
  const submitButtons = Array.from(form.querySelectorAll('button[type="submit"]'));

  const summaryElements = {
    total: form.querySelector('[data-summary-total]'),
    professional: form.querySelector('[data-summary-professional]'),
    personal: form.querySelector('[data-summary-personal]'),
    professionalLabel: form.querySelector('[data-summary-professional-label]'),
    personalLabel: form.querySelector('[data-summary-personal-label]'),
    payment: form.querySelector('[data-summary-payment]'),
    paidDate: form.querySelector('[data-summary-paid-date]'),
    reference: form.querySelector('[data-summary-reference]'),
    criterion: form.querySelector('[data-summary-criterion]'),
    conclusion: form.querySelector('[data-summary-conclusion]')
  };

  const readState = ({ restored = false } = {}) => createExpenseState({
    vehicleId: fields.vehicleId?.value,
    operationalDayId: fields.operationalDayId?.value,
    shiftId: fields.shiftId?.value,
    categoryId: fields.categoryId?.value,
    expenseDate: fields.expenseDate?.value,
    competenceMonth: fields.competenceMonth?.value,
    paidDate: fields.paidDate?.value,
    paymentStatus: checkedValue(form, 'paymentStatus'),
    amount: fields.amount?.value,
    classification: checkedValue(form, 'classification'),
    allocationMethod: checkedValue(form, 'allocationMethod'),
    professionalPercentagePercent: fields.professionalPercentagePercent?.value,
    professionalFixedAmount: fields.professionalFixedAmount?.value,
    adjustmentReason: fields.adjustmentReason?.value,
    notes: fields.notes?.value,
    paidDateManuallyEdited: restored ? Boolean(fields.paidDate?.value) : undefined,
    competenceMonthManuallyEdited: restored ? Boolean(fields.competenceMonth?.value) : undefined
  });

  let state = readState();
  let allocationPreview = null;
  let previewKey = '';
  let expenseDateTouched = false;
  let submitting = false;
  let statusTimer = null;
  const previewCoordinator = new AllocationPreviewCoordinator();

  const writeSynchronizedDates = () => {
    if (fields.expenseDate) fields.expenseDate.value = state.expenseDate;
    if (fields.paidDate) fields.paidDate.value = state.paidDate;
    if (fields.competenceMonth) fields.competenceMonth.value = state.competenceMonth;
  };

  const announceSummary = (message) => {
    if (!statusRegion) return;
    windowObject.clearTimeout?.(statusTimer);
    statusTimer = windowObject.setTimeout?.(() => {
      statusRegion.textContent = message;
    }, 300);
  };

  const applyClientValidation = () => {
    const errors = validateExpenseState(state);
    synchronizeFieldValidity(fields.paidDate, errors.paidDate);
    synchronizeFieldValidity(
      fields.professionalPercentagePercent,
      errors.professionalPercentagePercent
    );
    synchronizeFieldValidity(fields.professionalFixedAmount, errors.professionalFixedAmount);
    synchronizeFieldValidity(fields.adjustmentReason, errors.adjustmentReason);
    return errors;
  };

  const renderPreview = (preview) => {
    if (!allocationPreviewRegion) return;
    allocationPreviewRegion.hidden = false;
    allocationPreviewRegion.setAttribute('aria-busy', 'false');
    const month = formatReferenceMonth(state.competenceMonth);
    const alerts = Array.isArray(preview?.alerts)
      ? preview.alerts.map((alert) => alert.message).filter(Boolean)
      : [];

    if (preview?.status === 'CONFIRMED') {
      setText(previewTitle, `Rateio confirmado de ${month}`);
      setText(
        previewMessage,
        `${formatKilometers(preview.professionalKilometers)} profissionais de ${formatKilometers(preview.totalKilometers)}. ${formatPercentage(preview.professionalPercentage)} deste gasto será atribuído à operação.`
      );
    } else if (preview?.status === 'ESTIMATED') {
      setText(previewTitle, `Estimativa de ${month}`);
      setText(
        previewMessage,
        `${formatKilometers(preview.professionalKilometers)} profissionais de ${formatKilometers(preview.totalKilometers)} registrados até agora. Parte profissional estimada: ${formatPercentage(preview.professionalPercentage)}.`
      );
    } else {
      setText(previewTitle, `Rateio de ${month} ainda indisponível`);
      setText(
        previewMessage,
        'Ainda não existem leituras suficientes para estimar a divisão. O valor definitivo será calculado no fechamento mensal.'
      );
    }
    setText(previewWarning, alerts.join(' '));
  };

  const setPreviewLoading = () => {
    if (!allocationPreviewRegion) return;
    allocationPreviewRegion.hidden = false;
    allocationPreviewRegion.setAttribute('aria-busy', 'true');
    setText(previewTitle, `Consultando a quilometragem de ${formatReferenceMonth(state.competenceMonth)}…`);
    setText(previewMessage, '');
    setText(previewWarning, '');
  };

  const setPreviewFailure = () => {
    if (!allocationPreviewRegion) return;
    allocationPreviewRegion.hidden = false;
    allocationPreviewRegion.setAttribute('aria-busy', 'false');
    setText(previewTitle, 'Não foi possível consultar a quilometragem agora');
    setText(
      previewMessage,
      'Você ainda pode salvar o gasto. O servidor calculará o rateio quando houver dados disponíveis.'
    );
    setText(previewWarning, '');
  };

  const refreshAllocationPreview = async () => {
    const shouldLoad = state.classification === 'MIXED'
      && state.allocationMethod === 'MILEAGE_RATIO'
      && state.vehicleId
      && state.competenceMonth;
    if (!shouldLoad) {
      previewCoordinator.cancel();
      previewKey = '';
      allocationPreview = null;
      if (allocationPreviewRegion) allocationPreviewRegion.hidden = true;
      renderSummary();
      return;
    }

    const nextKey = `${state.vehicleId}|${state.competenceMonth}`;
    if (nextKey === previewKey && allocationPreview) {
      renderPreview(allocationPreview);
      renderSummary();
      return;
    }

    previewKey = nextKey;
    allocationPreview = null;
    const token = previewCoordinator.begin();
    setPreviewLoading();
    renderSummary();
    try {
      const response = await fetchImpl(
        buildAllocationPreviewUrl(state.vehicleId, state.competenceMonth),
        { credentials: 'same-origin', signal: token.signal }
      );
      if (!response.ok) throw new Error('Falha na consulta de quilometragem.');
      const preview = await response.json();
      if (!previewCoordinator.isCurrent(token)) return;
      allocationPreview = preview;
      renderPreview(preview);
      renderSummary();
    } catch (error) {
      if (error?.name === 'AbortError' || !previewCoordinator.isCurrent(token)) return;
      allocationPreview = null;
      setPreviewFailure();
      renderSummary();
    }
  };

  function renderSummary() {
    const summary = expenseSummary(state, allocationPreview);
    const professional = summary.calculable
      ? formatMoneyFromCents(summary.professionalCents)
      : 'Aguardando dados';
    const personal = summary.calculable
      ? formatMoneyFromCents(summary.personalCents)
      : 'Aguardando dados';
    const provisionalSuffix = summary.provisional ? ' estimada' : '';

    setText(summaryElements.total, formatMoneyFromCents(summary.totalCents));
    setText(summaryElements.professional, professional);
    setText(summaryElements.personal, personal);
    setText(summaryElements.professionalLabel, `Parte profissional${provisionalSuffix}`);
    setText(summaryElements.personalLabel, `Parte pessoal${provisionalSuffix}`);
    setText(summaryElements.payment, state.paymentStatus === 'PENDING' ? 'Pendente' : 'Pago');
    setText(
      summaryElements.paidDate,
      state.paymentStatus === 'PENDING' ? 'Ainda não pago' : formatCivilDate(state.paidDate)
    );
    setText(summaryElements.reference, formatReferenceMonth(state.competenceMonth));

    let criterion = 'Integralmente profissional';
    let conclusion = `Este gasto reduzirá o resultado profissional em ${professional}.`;
    if (state.classification === 'PERSONAL') {
      criterion = 'Integralmente pessoal';
      conclusion = 'Este gasto não reduzirá o resultado da operação.';
    } else if (state.classification === 'MIXED') {
      if (state.allocationMethod === 'MANUAL_PERCENTAGE') criterion = 'Percentual informado manualmente';
      else if (state.allocationMethod === 'FIXED_AMOUNT') criterion = 'Valor profissional informado manualmente';
      else if (allocationPreview?.status === 'CONFIRMED') criterion = 'Quilometragem confirmada';
      else if (allocationPreview?.status === 'ESTIMATED') criterion = 'Quilometragem estimada';
      else criterion = 'Aguardando dados de quilometragem';
      conclusion = summary.calculable
        ? `A parte profissional${provisionalSuffix} será ${professional} e a parte pessoal${provisionalSuffix} será ${personal}.`
        : 'A divisão será calculada quando houver dados de quilometragem suficientes.';
    }
    setText(summaryElements.criterion, criterion);
    setText(summaryElements.conclusion, conclusion);
    announceSummary(`Resumo atualizado. ${conclusion}`);
  }

  const render = ({ refreshPreview = false } = {}) => {
    const mixed = state.classification === 'MIXED';
    const manualPercentage = mixed && state.allocationMethod === 'MANUAL_PERCENTAGE';
    const fixedAmount = mixed && state.allocationMethod === 'FIXED_AMOUNT';
    const manual = manualPercentage || fixedAmount;
    const pending = state.paymentStatus === 'PENDING';

    setHiddenAndDisabled(allocationGroup, allocationFields, !mixed, true);
    setHiddenAndDisabled(percentageGroup, [fields.professionalPercentagePercent], !manualPercentage, true);
    setHiddenAndDisabled(fixedGroup, [fields.professionalFixedAmount], !fixedAmount, true);
    setHiddenAndDisabled(reasonGroup, [fields.adjustmentReason], !manual, true);
    setHiddenAndDisabled(paidDateGroup, [fields.paidDate], pending, true);

    setText(referenceLabel, formatReferenceMonth(state.competenceMonth));
    applyClientValidation();
    renderSummary();
    if (refreshPreview) refreshAllocationPreview();
  };

  const updateFieldState = (target) => {
    if (!target?.name) return false;
    if (target.name === 'expenseDate') {
      expenseDateTouched = true;
      state = updateExpenseState(state, { type: 'EXPENSE_DATE_CHANGED', value: target.value });
      writeSynchronizedDates();
      return true;
    }
    if (target.name === 'paidDate') {
      state = updateExpenseState(state, { type: 'PAID_DATE_CHANGED', value: target.value });
      return true;
    }
    if (target.name === 'competenceMonth') {
      state = updateExpenseState(state, { type: 'COMPETENCE_MONTH_CHANGED', value: target.value });
      return true;
    }
    if (target.name === 'classification') {
      state = updateExpenseState(state, { type: 'CLASSIFICATION_CHANGED', value: target.value });
      return true;
    }
    if (target.name === 'allocationMethod') {
      state = updateExpenseState(state, { type: 'ALLOCATION_METHOD_CHANGED', value: target.value });
      return true;
    }
    if (target.name === 'paymentStatus') {
      state = updateExpenseState(state, { type: 'PAYMENT_STATUS_CHANGED', value: target.value });
      if (fields.paidDate) fields.paidDate.value = state.paidDate;
      return true;
    }
    if (Object.hasOwn(fields, target.name)) {
      state = updateExpenseState(state, {
        type: 'FIELD_CHANGED',
        field: target.name,
        value: target.value
      });
      return true;
    }
    return false;
  };

  const clearServerInvalidState = (target) => {
    if (!target?.classList?.contains('is-invalid') || !target.checkValidity?.()) return;
    target.classList.remove('is-invalid');
    synchronizeFieldValidity(target);
  };

  form.addEventListener('input', (event) => {
    clearServerInvalidState(event.target);
    if (!updateFieldState(event.target)) return;
    render({
      refreshPreview: ['vehicleId', 'expenseDate', 'competenceMonth', 'classification', 'allocationMethod']
        .includes(event.target.name)
    });
  });

  form.addEventListener('change', (event) => {
    clearServerInvalidState(event.target);
    if (!updateFieldState(event.target)) return;
    render({
      refreshPreview: ['vehicleId', 'expenseDate', 'competenceMonth', 'classification', 'allocationMethod']
        .includes(event.target.name)
    });
  });

  form.addEventListener('invalid', (event) => synchronizeFieldValidity(event.target), true);

  form.addEventListener('submit', (event) => {
    if (submitting) {
      event.preventDefault();
      return;
    }
    formatLocalizedInputs(form, { final: true });
    state = readState();
    render();
    if (!form.checkValidity()) {
      event.preventDefault();
      const invalid = form.querySelector(':invalid, .is-invalid');
      if (invalid && moreOptions?.contains(invalid)) moreOptions.open = true;
      invalid?.focus?.();
      invalid?.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
      return;
    }
    submitting = true;
    submitButtons.forEach((button) => setExpenseSubmitState(form, button, true));
  });

  documentObject.addEventListener('guided-form:before-save', (event) => {
    if (event.detail?.form !== form) return;
    state = readState();
    replaceDraftPayload(event.detail.payload, expenseDraftPayload(state));
  });

  documentObject.addEventListener('guided-form:restored', (event) => {
    if (event.detail?.form !== form) return;
    state = readState({ restored: true });
    expenseDateTouched = true;
    render({ refreshPreview: true });
  });

  documentObject.addEventListener('app:time-zone-changed', (event) => {
    if (expenseDateTouched) return;
    const timeZoneId = event.detail?.timeZoneId;
    if (!timeZoneId) return;
    const nextDate = civilDateInTimeZone(new Date(), timeZoneId);
    state = updateExpenseState(state, { type: 'EXPENSE_DATE_CHANGED', value: nextDate });
    writeSynchronizedDates();
    fields.expenseDate?.dispatchEvent?.(new Event('input', { bubbles: true }));
    render({ refreshPreview: true });
  });

  discardButton?.addEventListener('click', async () => {
    if (!windowObject.confirm?.('Descartar o rascunho salvo e limpar este formulário?')) return;
    discardButton.disabled = true;
    const controller = form.guidedFormController;
    const client = controller?.client ?? new FormDraftClient();
    try {
      await client.discard('EXPENSE', 'current', controller?.version ?? null);
      windowObject.location.assign('/expenses/new');
    } catch {
      discardButton.disabled = false;
      if (statusRegion) statusRegion.textContent = 'Não foi possível descartar o rascunho.';
    }
  });

  windowObject.addEventListener?.('pageshow', () => {
    submitting = false;
    submitButtons.forEach((button) => setExpenseSubmitState(form, button, false));
  });

  if (errorSummary) windowObject.requestAnimationFrame?.(() => errorSummary.focus());
  Array.from(form.querySelectorAll('.is-invalid')).forEach((input) => {
    input.setAttribute('aria-invalid', 'true');
    input.closest?.('.input-group')?.classList.add('is-invalid-group');
  });
  writeSynchronizedDates();
  render({ refreshPreview: true });

  return { form, getState: () => ({ ...state }), refreshAllocationPreview };
}

if (typeof document !== 'undefined') {
  initializeExpenseForm(document, window);
}
