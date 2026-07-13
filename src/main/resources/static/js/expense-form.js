(() => {
  const form = document.getElementById('expense-form');
  if (!form) return;

  const classification = form.querySelector('[name="classification"]');
  const allocationMethod = form.querySelector('[name="allocationMethod"]');
  const percentage = form.querySelector('[name="professionalPercentage"]');
  const fixedAmount = form.querySelector('[name="professionalFixedAmount"]');
  const reason = form.querySelector('[name="adjustmentReason"]');

  const allocationGroup = form.querySelector('[data-allocation-group]');
  const percentageGroup = form.querySelector('[data-percentage-group]');
  const fixedGroup = form.querySelector('[data-fixed-group]');
  const reasonGroup = form.querySelector('[data-reason-group]');

  function setVisible(group, visible) {
    if (group) group.hidden = !visible;
  }

  function refreshExpenseFields(clearInactive = false) {
    const mixed = classification?.value === 'MIXED';
    const method = allocationMethod?.value || '';
    const percentageSelected = mixed && method === 'MANUAL_PERCENTAGE';
    const fixedSelected = mixed && method === 'FIXED_AMOUNT';

    setVisible(allocationGroup, mixed);
    setVisible(percentageGroup, percentageSelected);
    setVisible(fixedGroup, fixedSelected);
    setVisible(reasonGroup, percentageSelected || fixedSelected);

    if (allocationMethod) allocationMethod.required = mixed;
    if (percentage) percentage.required = percentageSelected;
    if (fixedAmount) fixedAmount.required = fixedSelected;
    if (reason) reason.required = percentageSelected || fixedSelected;

    if (!clearInactive) return;
    if (!mixed && allocationMethod) allocationMethod.value = '';
    if (!percentageSelected && percentage) percentage.value = '';
    if (!fixedSelected && fixedAmount) fixedAmount.value = '';
    if (!percentageSelected && !fixedSelected && reason) reason.value = '';
  }

  classification?.addEventListener('change', () => refreshExpenseFields(true));
  allocationMethod?.addEventListener('change', () => refreshExpenseFields(true));
  form.addEventListener('submit', () => refreshExpenseFields(true));
  refreshExpenseFields(false);
})();
