const form = document.getElementById("expense-form");

if (form) {
  const classification = form.querySelector('[name="classification"]');
  const allocationMethod = form.querySelector('[name="allocationMethod"]');
  const amount = form.querySelector('[name="amount"]');
  const percentage = form.querySelector('[name="professionalPercentagePercent"]');
  const fixedAmount = form.querySelector('[name="professionalFixedAmount"]');
  const reason = form.querySelector('[name="adjustmentReason"]');

  const allocationGroup = form.querySelector("[data-allocation-group]");
  const percentageGroup = form.querySelector("[data-percentage-group]");
  const fixedGroup = form.querySelector("[data-fixed-group]");
  const reasonGroup = form.querySelector("[data-reason-group]");
  const totalSummary = form.querySelector("[data-summary-total]");
  const professionalSummary = form.querySelector("[data-summary-professional]");

  function setConditionalField(group, field, visible, required = false) {
    if (group) group.hidden = !visible;
    if (field) {
      field.disabled = !visible;
      field.required = visible && required;
    }
  }

  function refreshExpenseFields(clearInactive = false) {
    const mixed = classification?.value === "MIXED";
    const method = allocationMethod?.value || "";
    const percentageSelected = mixed && method === "MANUAL_PERCENTAGE";
    const fixedSelected = mixed && method === "FIXED_AMOUNT";
    const manual = percentageSelected || fixedSelected;

    setConditionalField(allocationGroup, allocationMethod, mixed, true);
    setConditionalField(percentageGroup, percentage, percentageSelected, true);
    setConditionalField(fixedGroup, fixedAmount, fixedSelected, true);
    setConditionalField(reasonGroup, reason, manual, true);

    if (clearInactive) {
      if (!mixed && allocationMethod) allocationMethod.value = "";
      if (!percentageSelected && percentage) percentage.value = "";
      if (!fixedSelected && fixedAmount) fixedAmount.value = "";
      if (!manual && reason) reason.value = "";
    }
    refreshSummary();
  }

  function refreshSummary() {
    const total = parseLocalizedDecimal(amount?.value) ?? 0;
    let professional = 0;
    let calculatedByServer = false;

    if (classification?.value === "PROFESSIONAL") {
      professional = total;
    } else if (classification?.value === "MIXED") {
      if (allocationMethod?.value === "MANUAL_PERCENTAGE") {
        const percentValue = parseLocalizedDecimal(percentage?.value);
        const ratio = percentValue === null ? null : percentValue / 100;
        professional = ratio === null ? 0 : total * ratio;
      } else if (allocationMethod?.value === "FIXED_AMOUNT") {
        professional = parseLocalizedDecimal(fixedAmount?.value) ?? 0;
      } else if (allocationMethod?.value === "MILEAGE_RATIO") {
        calculatedByServer = true;
      }
    }

    if (totalSummary) totalSummary.textContent = formatMoney(total);
    if (professionalSummary) {
      professionalSummary.textContent = calculatedByServer
        ? "Calculado ao salvar"
        : formatMoney(Math.min(Math.max(professional, 0), total));
    }
  }

  classification?.addEventListener("change", () => refreshExpenseFields(true));
  allocationMethod?.addEventListener("change", () => refreshExpenseFields(true));
  [amount, percentage, fixedAmount].forEach((field) => {
    field?.addEventListener("input", refreshSummary);
  });
  form.addEventListener("submit", () => refreshExpenseFields(true));

  document.addEventListener("guided-form:before-save", (event) => {
    if (event.detail.form !== form) return;
    refreshExpenseFields(false);
    const payload = event.detail.payload;
    if (classification?.value !== "MIXED") {
      delete payload.allocationMethod;
      delete payload.professionalPercentagePercent;
      delete payload.professionalFixedAmount;
      delete payload.adjustmentReason;
    } else if (allocationMethod?.value === "MILEAGE_RATIO") {
      delete payload.professionalPercentagePercent;
      delete payload.professionalFixedAmount;
      delete payload.adjustmentReason;
    } else if (allocationMethod?.value === "MANUAL_PERCENTAGE") {
      delete payload.professionalFixedAmount;
    } else if (allocationMethod?.value === "FIXED_AMOUNT") {
      delete payload.professionalPercentagePercent;
    }
  });

  document.addEventListener("guided-form:restored", (event) => {
    if (event.detail.form === form) refreshExpenseFields(false);
  });

  refreshExpenseFields(false);
}

function parseLocalizedDecimal(raw) {
  if (raw === null || raw === undefined || String(raw).trim() === "") return null;
  let normalized = String(raw).trim().replaceAll(" ", "");
  if (normalized.includes(",") && normalized.includes(".")) {
    normalized = normalized.replaceAll(".", "").replace(",", ".");
  } else {
    normalized = normalized.replace(",", ".");
  }
  const value = Number(normalized);
  return Number.isFinite(value) ? value : null;
}

function formatMoney(value) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Number(value) || 0);
}
