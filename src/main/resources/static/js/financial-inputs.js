import {
  formatFinancialMoney,
  formatFinancialPercentage,
  sanitizeFinancialEntry,
} from './financial-input-formatters.js';

export function initializeFinancialInputs(root = document) {
  root.querySelectorAll('[data-financial-money], [data-financial-percent]').forEach((input) => {
    if (input.dataset.financialInputReady === 'true') {
      formatInput(input);
      return;
    }
    input.dataset.financialInputReady = 'true';

    input.addEventListener('input', () => {
      const sanitized = sanitizeFinancialEntry(input.value);
      if (sanitized !== input.value) {
        const position = input.selectionStart ?? sanitized.length;
        input.value = sanitized;
        const nextPosition = Math.min(position, sanitized.length);
        input.setSelectionRange?.(nextPosition, nextPosition);
      }
    });

    input.addEventListener('blur', () => formatInput(input));
    formatInput(input);
  });
}

export function formatInput(input) {
  if (!input || input.value.trim() === '') return;
  input.value = input.matches('[data-financial-percent]')
    ? formatFinancialPercentage(input.value)
    : formatFinancialMoney(input.value);
}
