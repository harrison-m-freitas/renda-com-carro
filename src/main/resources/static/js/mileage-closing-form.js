(() => {
  const form = document.getElementById('mileage-closing-form');
  if (!form) return;

  const trigger = document.getElementById('enable-adjustment');
  const manualInput = document.getElementById('manualAdjustment');
  const reasonGroup = document.getElementById('adjustment-reason-group');
  const reason = document.getElementById('adjustmentReason');
  const fields = Array.from(form.querySelectorAll('[data-adjustable]'));

  function enableManualAdjustment() {
    fields.forEach((field) => field.removeAttribute('readonly'));
    if (manualInput) manualInput.value = 'true';
    if (reasonGroup) reasonGroup.hidden = false;
    if (reason) reason.required = true;
    if (trigger) {
      trigger.disabled = true;
      trigger.textContent = 'Correção manual ativada';
    }
  }

  trigger?.addEventListener('click', enableManualAdjustment);
  if (form.dataset.manualAdjustment === 'true') {
    enableManualAdjustment();
  }
})();
