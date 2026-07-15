import { buildObligationPreview, suggestFirstDueDate } from './obligation-calculator.js';
import { initializeFinancialInputs } from './financial-inputs.js';
import { parseFinancialDecimal } from './financial-input-formatters.js';
import { activeObligationFieldNames, filterObligationPayload } from './obligation-form-state.js';

const form = document.getElementById('obligation-form');

if (form) {
  initializeFinancialInputs(form);

  const fields = Object.fromEntries([
    'type', 'mode', 'calculationMethod', 'creditor', 'principalAmount',
    'interestRatePercent', 'interestRatePeriod', 'startDate', 'firstDueDate',
    'termMonths', 'installmentAmount', 'singlePaymentAmount', 'monthlyTarget',
  ].map((name) => [name, form.elements.namedItem(name)]));

  const conditionalPanels = Array.from(form.querySelectorAll('[data-obligation-condition]'));
  const summary = {
    creditor: form.querySelector('[data-summary-creditor]'),
    principal: form.querySelector('[data-summary-principal]'),
    payment: form.querySelector('[data-summary-payment]'),
    total: form.querySelector('[data-summary-total]'),
    cost: form.querySelector('[data-summary-cost]'),
    rate: form.querySelector('[data-summary-rate]'),
    dates: form.querySelector('[data-summary-dates]'),
    warning: form.querySelector('[data-summary-warning]'),
  };

  function currentState() {
    return {
      mode: selectedValue('mode'),
      calculationMethod: selectedValue('calculationMethod'),
    };
  }

  function selectedValue(name) {
    const control = form.elements.namedItem(name);
    if (!control) return '';
    if (typeof RadioNodeList !== 'undefined' && control instanceof RadioNodeList) return control.value;
    return control.value ?? '';
  }

  function matchesCondition(condition, state) {
    return condition.split(',').some((alternative) => alternative.trim().split(/\s+/).every((term) => {
      const [key, rawValues] = term.split('=');
      if (!key || rawValues == null) return false;
      return rawValues.split('|').includes(state[key]);
    }));
  }

  function refreshConditionalPanels() {
    refreshMethodChoices();
    const state = currentState();
    conditionalPanels.forEach((panel) => {
      const visible = matchesCondition(panel.dataset.obligationCondition ?? '', state);
      panel.hidden = !visible;
      panel.querySelectorAll('input, select, textarea').forEach((control) => {
        control.disabled = !visible;
      });
    });
    refreshRequiredFields();
    refreshSelectedCards();
  }

  function refreshMethodChoices() {
    const mode = selectedValue('mode');
    const choices = Array.from(form.querySelectorAll('[data-method-modes]'));
    choices.forEach((choice) => {
      const radio = choice.querySelector('input[type="radio"]');
      const allowed = (choice.dataset.methodModes ?? '').split('|').includes(mode);
      choice.hidden = !allowed;
      if (radio) radio.disabled = !allowed;
    });

    const selected = form.querySelector('input[name="calculationMethod"]:checked');
    if (selected && !selected.disabled) return;
    const preferred = {
      FIXED_INSTALLMENTS: 'INSTALLMENT_KNOWN',
      FLEXIBLE_PAYMENTS: 'INTEREST_FREE',
      SINGLE_PAYMENT: 'INTEREST_FREE',
    }[mode];
    setRadio('calculationMethod', preferred);
  }

  function refreshRequiredFields() {
    const active = activeObligationFieldNames(currentState());
    form.querySelectorAll('[data-conditionally-required]').forEach((control) => {
      control.required = !control.disabled && active.has(control.name);
    });
  }

  function refreshSelectedCards() {
    form.querySelectorAll('.obligation-choice').forEach((label) => {
      const radio = label.querySelector('input[type="radio"]');
      label.classList.toggle('is-selected', Boolean(radio?.checked));
    });
  }

  function formInput() {
    return {
      ...currentState(),
      principalAmount: parseFinancialDecimal(fields.principalAmount?.value),
      interestRatePercent: parseFinancialDecimal(fields.interestRatePercent?.value),
      interestRatePeriod: fields.interestRatePeriod?.value,
      termMonths: fields.termMonths?.value ? Number(fields.termMonths.value) : null,
      installmentAmount: parseFinancialDecimal(fields.installmentAmount?.value),
      singlePaymentAmount: parseFinancialDecimal(fields.singlePaymentAmount?.value),
      monthlyTarget: parseFinancialDecimal(fields.monthlyTarget?.value),
      firstDueDate: fields.firstDueDate?.value,
    };
  }

  function refreshSummary() {
    const input = formInput();
    const preview = buildObligationPreview(input);
    setText(summary.creditor, fields.creditor?.value.trim() || 'Ainda não informado');
    setText(summary.principal, money(input.principalAmount));

    if (!preview.valid) {
      setText(summary.payment, 'Complete os dados do pagamento');
      setText(summary.total, '—');
      setText(summary.cost, '—');
      setText(summary.rate, '—');
      setText(summary.dates, '—');
      showWarning(preview.message, 'neutral');
      return;
    }

    if (input.mode === 'FIXED_INSTALLMENTS') {
      setText(summary.payment, `${input.termMonths} × ${money(preview.installmentAmount)}`);
      setText(summary.total, money(preview.totalAmount));
      setText(summary.cost, money(preview.totalInterest));
      setText(summary.rate, rateText(preview));
      setText(summary.dates, `${dateText(preview.firstDueDate)} a ${dateText(preview.lastDueDate)}`);
    } else if (input.mode === 'FLEXIBLE_PAYMENTS') {
      setText(summary.payment, `${money(preview.monthlyTarget)} por mês`);
      setText(summary.total, preview.estimatedMonths ? `${preview.estimatedMonths} meses estimados` : 'Prazo ainda não calculável');
      setText(summary.cost, preview.firstMonthInterest == null ? '—' : `${money(preview.firstMonthInterest)} no primeiro mês`);
      setText(summary.rate, rateText(preview));
      setText(summary.dates, 'Sem vencimentos fixos');
    } else {
      setText(summary.payment, `Pagamento único de ${money(preview.totalAmount)}`);
      setText(summary.total, money(preview.totalAmount));
      setText(summary.cost, money(preview.totalInterest));
      setText(summary.rate, 'Não calculada');
      setText(summary.dates, dateText(preview.firstDueDate));
    }
    showWarning(preview.warning, preview.amortizes === false ? 'danger' : 'info');
  }

  function applySuggestedFirstDueDate() {
    if (!fields.startDate?.value || fields.firstDueDate?.value) return;
    const suggested = suggestFirstDueDate(fields.startDate.value);
    if (!suggested) return;
    fields.firstDueDate.value = suggested;
    fields.firstDueDate.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function applyTypeDefaults() {
    const type = selectedValue('type');
    const modeWasTouched = form.dataset.modeTouched === 'true';
    const methodWasTouched = form.dataset.methodTouched === 'true';
    if (!modeWasTouched) {
      setRadio('mode', type === 'BANK_FINANCING' ? 'FIXED_INSTALLMENTS' : 'FLEXIBLE_PAYMENTS');
    }
    if (!methodWasTouched) {
      setRadio(
        'calculationMethod',
        type === 'BANK_FINANCING' ? 'INSTALLMENT_KNOWN' : 'INTEREST_FREE',
      );
    }
  }

  function setRadio(name, value) {
    const radio = form.querySelector(`input[name="${name}"][value="${value}"]`);
    if (radio) radio.checked = true;
  }

  form.addEventListener('change', (event) => {
    if (event.target?.name === 'type') applyTypeDefaults();
    if (event.target?.name === 'mode') form.dataset.modeTouched = 'true';
    if (event.target?.name === 'calculationMethod') form.dataset.methodTouched = 'true';
    refreshConditionalPanels();
    refreshSummary();
  });
  form.addEventListener('input', refreshSummary);
  form.querySelector('[data-suggest-first-due-date]')?.addEventListener('click', applySuggestedFirstDueDate);

  document.addEventListener('guided-form:before-save', (event) => {
    if (event.detail.form !== form) return;
    const filtered = filterObligationPayload(event.detail.payload, currentState());
    Object.keys(event.detail.payload).forEach((key) => delete event.detail.payload[key]);
    Object.assign(event.detail.payload, filtered);
  });

  document.addEventListener('guided-form:restored', (event) => {
    if (event.detail.form !== form) return;
    initializeFinancialInputs(form);
    refreshConditionalPanels();
    refreshSummary();
  });

  document.addEventListener('guided-form:step-changed', (event) => {
    if (event.detail.form !== form) return;
    const title = form.querySelector(`[data-form-step="${event.detail.currentStep}"] h2`);
    title?.focus?.({ preventScroll: true });
  });

  form.addEventListener('submit', (event) => {
    refreshConditionalPanels();
    if (!form.checkValidity()) {
      event.preventDefault();
      const first = form.querySelector(':invalid');
      first?.focus();
      first?.scrollIntoView({ block: 'center', behavior: 'smooth' });
      return;
    }
    const button = form.querySelector('button[type="submit"]:not([data-guided-final])');
    if (button && !button.disabled) {
      button.disabled = true;
      button.dataset.originalText = button.textContent;
      button.textContent = 'Salvando…';
    }
  });

  refreshConditionalPanels();
  refreshSummary();
}

function setText(element, value) {
  if (element) element.textContent = value;
}

function money(value) {
  if (value == null || !Number.isFinite(Number(value))) return '—';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number(value));
}

function rateText(preview) {
  if (preview.monthlyRatePercent == null) return 'Ainda não informada';
  const monthly = percent(preview.monthlyRatePercent);
  const annual = percent(preview.annualRatePercent);
  return `${monthly} a.m. · ${annual} a.a. efetivos`;
}

function percent(value) {
  return `${new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(Number(value) || 0)}%`;
}

function dateText(value) {
  if (!value) return '—';
  const [year, month, day] = value.split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
}

function showWarning(message, tone) {
  const element = form?.querySelector('[data-summary-warning]');
  if (!element) return;
  element.hidden = !message;
  element.textContent = message || '';
  element.dataset.tone = tone;
}
