const form = document.getElementById("obligation-form");

if (form) {
  const mode = form.querySelector('[name="mode"]');
  const creditor = form.querySelector('[name="creditor"]');
  const principal = form.querySelector('[name="principal"]');
  const annualRate = form.querySelector('[name="annualRatePercent"]');
  const firstDueDate = form.querySelector('[name="firstDueDate"]');
  const termMonths = form.querySelector('[name="termMonths"]');
  const plannedInstallment = form.querySelector('[name="plannedInstallment"]');
  const monthlyTarget = form.querySelector('[name="monthlyTarget"]');
  const structuredGroup = form.querySelector("[data-structured-group]");
  const flexibleGroup = form.querySelector("[data-flexible-group]");

  function setGroup(group, visible, requiredFields) {
    if (group) group.hidden = !visible;
    group?.querySelectorAll("input, select, textarea").forEach((field) => {
      field.disabled = !visible;
      field.required = visible && requiredFields.includes(field.name);
    });
  }

  function refreshMode(clearInactive = false) {
    const structured = mode?.value === "STRUCTURED";
    setGroup(structuredGroup, structured, ["firstDueDate", "termMonths"]);
    setGroup(flexibleGroup, !structured, ["monthlyTarget"]);

    if (clearInactive) {
      if (structured) {
        if (monthlyTarget) monthlyTarget.value = "";
      } else {
        [firstDueDate, termMonths, plannedInstallment].forEach((field) => {
          if (field) field.value = "";
        });
      }
    }
    refreshSummary();
  }

  function refreshSummary() {
    setText("[data-obligation-summary-creditor]", creditor?.value.trim() || "—");
    setText(
      "[data-obligation-summary-principal]",
      formatMoney(parseDecimal(principal?.value) ?? 0),
    );
    setText(
      "[data-obligation-summary-rate]",
      `${parseDecimal(annualRate?.value) ?? 0}% ao ano`,
    );
    const structured = mode?.value === "STRUCTURED";
    const plan = structured
      ? `${termMonths?.value || 0} parcelas a partir de ${formatDate(firstDueDate?.value)}`
      : `${formatMoney(parseDecimal(monthlyTarget?.value) ?? 0)} por mês`;
    setText("[data-obligation-summary-plan]", plan);
  }

  function setText(selector, value) {
    const element = form.querySelector(selector);
    if (element) element.textContent = value;
  }

  mode?.addEventListener("change", () => refreshMode(true));
  [creditor, principal, annualRate, firstDueDate, termMonths, monthlyTarget]
    .forEach((field) => field?.addEventListener("input", refreshSummary));

  document.addEventListener("guided-form:before-save", (event) => {
    if (event.detail.form !== form) return;
    const payload = event.detail.payload;
    if (mode?.value === "STRUCTURED") {
      delete payload.monthlyTarget;
    } else {
      delete payload.firstDueDate;
      delete payload.termMonths;
      delete payload.plannedInstallment;
    }
  });

  document.addEventListener("guided-form:restored", (event) => {
    if (event.detail.form === form) refreshMode(false);
  });

  refreshMode(false);
}

function parseDecimal(raw) {
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

function formatDate(value) {
  if (!value) return "data não informada";
  const date = new Date(`${value}T12:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("pt-BR").format(date);
}
