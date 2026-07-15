const CLASSIFICATION_DESCRIPTIONS = Object.freeze({
  PROFESSIONAL: 'Todo o valor será considerado custo da operação.',
  PERSONAL: 'O valor não reduzirá o resultado profissional.',
  MIXED: 'Uma parte será profissional e a outra pessoal.'
});

const PAYMENT_DESCRIPTIONS = Object.freeze({
  PAID: 'O pagamento já foi realizado.',
  PENDING: 'O gasto ainda será pago.'
});

const ALLOCATION_DESCRIPTIONS = Object.freeze({
  MILEAGE_RATIO: 'Usa o fechamento confirmado ou a estimativa disponível para o mês.',
  MANUAL_PERCENTAGE: 'Informe quanto do gasto pertence à operação em percentual.',
  FIXED_AMOUNT: 'Informe diretamente o valor profissional do gasto.'
});

export function classificationDescription(value) {
  return CLASSIFICATION_DESCRIPTIONS[value] ?? '';
}

export function paymentDescription(value) {
  return PAYMENT_DESCRIPTIONS[value] ?? '';
}

export function allocationMethodDescription(value) {
  return ALLOCATION_DESCRIPTIONS[value] ?? '';
}

const checkedValue = (form, name) => form
  .querySelector(`[name="${name}"]:checked`)?.value ?? '';

const setText = (element, value) => {
  if (element) element.textContent = value;
};

export function initializeExpenseChoiceDescriptions(documentObject = globalThis.document) {
  const form = documentObject?.querySelector?.('#expense-form');
  if (!form) return null;

  const classificationHelp = form.querySelector('[data-classification-description]');
  const allocationHelp = form.querySelector('[data-allocation-method-description]');
  const paymentHelp = form.querySelector('[data-payment-description]');

  const render = () => {
    setText(classificationHelp, classificationDescription(checkedValue(form, 'classification')));
    setText(allocationHelp, allocationMethodDescription(checkedValue(form, 'allocationMethod')));
    setText(paymentHelp, paymentDescription(checkedValue(form, 'paymentStatus')));
  };

  form.addEventListener('change', (event) => {
    if (!['classification', 'allocationMethod', 'paymentStatus'].includes(event.target?.name)) return;
    render();
  });

  documentObject.addEventListener?.('guided-form:restored', (event) => {
    if (event.detail?.form === form) render();
  });

  render();
  return { form, render };
}

if (typeof document !== 'undefined') {
  initializeExpenseChoiceDescriptions(document);
}