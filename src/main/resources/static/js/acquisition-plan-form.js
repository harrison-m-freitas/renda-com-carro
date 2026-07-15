import { initializeFinancialInputs } from './financial-inputs.js';
import { parseFinancialDecimal } from './financial-input-formatters.js';

const form = document.getElementById('acquisition-plan-form');

if (form) {
  initializeFinancialInputs(form);
  const purchase = form.elements.namedItem('purchaseAmount');
  const own = form.elements.namedItem('ownResourcesAmount');

  const refresh = () => {
    const purchaseValue = parseFinancialDecimal(purchase?.value) ?? 0;
    const ownValue = parseFinancialDecimal(own?.value) ?? 0;
    setText('[data-plan-purchase]', money(purchaseValue));
    setText('[data-plan-own]', money(ownValue));
    setText('[data-plan-remaining]', money(Math.max(0, purchaseValue - ownValue)));
    own?.setCustomValidity(
      ownValue > purchaseValue && purchaseValue > 0
        ? 'Os recursos próprios não podem ultrapassar o valor da compra.'
        : '',
    );
  };

  form.addEventListener('input', refresh);
  form.addEventListener('change', refresh);
  form.addEventListener('submit', (event) => {
    refresh();
    if (!form.checkValidity()) {
      event.preventDefault();
      const first = form.querySelector(':invalid');
      first?.reportValidity();
      first?.focus();
    }
  });
  refresh();

  function setText(selector, value) {
    const element = form.querySelector(selector);
    if (element) element.textContent = value;
  }
}

function money(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number(value) || 0);
}
