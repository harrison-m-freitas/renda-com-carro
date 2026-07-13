(() => {
  const form = document.querySelector('#vehicle-form');
  const plate = document.querySelector('[data-vehicle-plate]');

  if (!form || !plate) return;

  const normalizePlate = (value) => value
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 7);

  const formatPlate = (value) => {
    const normalized = normalizePlate(value);
    return /^[A-Z]{3}[0-9]{4}$/.test(normalized)
      ? `${normalized.slice(0, 3)}-${normalized.slice(3)}`
      : normalized;
  };

  plate.value = formatPlate(plate.value);

  plate.addEventListener('input', () => {
    const cursorAtEnd = plate.selectionStart === plate.value.length;
    plate.value = formatPlate(plate.value);
    if (cursorAtEnd) plate.setSelectionRange(plate.value.length, plate.value.length);
  });

  form.addEventListener('submit', () => {
    plate.value = normalizePlate(plate.value);
    const submit = form.querySelector('button[type="submit"]');
    if (submit) {
      submit.disabled = true;
      submit.dataset.originalText = submit.textContent;
      submit.textContent = 'Salvando…';
    }
  });
})();
