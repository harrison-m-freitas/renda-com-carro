const form = document.getElementById("mileage-closing-form");

if (form) {
  const trigger = document.getElementById("enable-adjustment");
  const manualInput = document.getElementById("manualAdjustment");
  const reasonGroup = document.getElementById("adjustment-reason-group");
  const reason = document.getElementById("adjustmentReason");
  const fields = Array.from(form.querySelectorAll("[data-adjustable]"));

  function enableManualAdjustment() {
    fields.forEach((field) => {
      field.readOnly = false;
      field.classList.remove("guided-calculated");
    });
    if (manualInput) manualInput.value = "true";
    if (reasonGroup) reasonGroup.hidden = false;
    if (reason) reason.required = true;
    if (trigger) {
      trigger.disabled = true;
      trigger.textContent = "Correção manual ativada";
    }
  }

  function restoreAutomaticMode() {
    fields.forEach((field) => {
      field.readOnly = true;
      field.classList.add("guided-calculated");
    });
    if (manualInput) manualInput.value = "false";
    if (reasonGroup) reasonGroup.hidden = true;
    if (reason) reason.required = false;
    if (trigger) {
      trigger.disabled = false;
      trigger.textContent = "Corrigir valores";
    }
  }

  trigger?.addEventListener("click", enableManualAdjustment);

  document.addEventListener("guided-form:before-save", (event) => {
    if (event.detail.form !== form) return;
    const payload = event.detail.payload;
    const manual = manualInput?.value === "true";
    payload.manualAdjustment = String(manual);
    if (!manual) {
      delete payload.initialOdometer;
      delete payload.finalOdometer;
      delete payload.professionalKilometers;
      delete payload.adjustmentReason;
    }
  });

  document.addEventListener("guided-form:restored", (event) => {
    if (event.detail.form !== form) return;
    if (manualInput?.value === "true") enableManualAdjustment();
    else restoreAutomaticMode();
  });

  if (form.dataset.manualAdjustment === "true") enableManualAdjustment();
  else restoreAutomaticMode();
}
