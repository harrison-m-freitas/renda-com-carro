(() => {
  const form = document.querySelector('#vehicleForm');
  if (!form) return;

  const submitButton = form.querySelector('[data-vehicle-submit]');
  const plateInput = form.querySelector('[data-vehicle-plate]');
  const localizedInputs = form.querySelectorAll('[data-localized-number]');
  const invalidInputs = form.querySelectorAll('.is-invalid');
  let dirty = false;
  let submitting = false;

  const normalizeNumericText = (value) => value
    .replace(/\u00a0/g, '')
    .replace(/\s/g, '')
    .replace(/[^\d,.-]/g, '');

  const parseLocalizedNumber = (raw) => {
    let value = normalizeNumericText(raw);
    if (!value) return null;

    if (value.includes(',') && value.includes('.')) {
      value = value.replace(/\./g, '').replace(',', '.');
    } else if (value.includes(',')) {
      value = value.replace(',', '.');
    } else if (/^-?\d{1,3}(\.\d{3})+$/.test(value)) {
      value = value.replace(/\./g, '');
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  };

  const formatLocalizedNumber = (input) => {
    const parsed = parseLocalizedNumber(input.value);
    if (parsed === null) return;
    const scale = Number(input.dataset.decimalScale || '0');
    input.value = new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: scale,
      maximumFractionDigits: scale
    }).format(parsed);
  };

  const formatPlate = () => {
    if (!plateInput) return;
    let normalized = plateInput.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);
    if (/^[A-Z]{3}\d{1,4}$/.test(normalized) && normalized.length > 3) {
      normalized = `${normalized.slice(0, 3)}-${normalized.slice(3)}`;
    }
    plateInput.value = normalized;
  };

  if (plateInput) {
    formatPlate();
    plateInput.addEventListener('input', formatPlate);
  }

  localizedInputs.forEach((input) => {
    formatLocalizedNumber(input);
    input.addEventListener('blur', () => formatLocalizedNumber(input));
  });

  invalidInputs.forEach((input) => input.setAttribute('aria-invalid', 'true'));
  if (invalidInputs.length > 0) {
    window.requestAnimationFrame(() => invalidInputs[0].focus());
  }

  form.addEventListener('input', () => {
    dirty = true;
  });

  form.addEventListener('submit', () => {
    submitting = true;
    dirty = false;
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.dataset.originalText = submitButton.textContent;
      submitButton.textContent = 'Salvando…';
    }
  });

  window.addEventListener('beforeunload', (event) => {
    if (!dirty || submitting) return;
    event.preventDefault();
    event.returnValue = '';
  });
})();
