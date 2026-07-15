const COMMON_FIELDS = new Set([
  'draftKey',
  'acquisitionPlanId',
  'vehicleId',
  'type',
  'creditor',
  'principalAmount',
  'startDate',
  'mode',
  'calculationMethod',
  'notes',
]);

export function activeObligationFieldNames({ mode, calculationMethod } = {}) {
  const fields = new Set(COMMON_FIELDS);

  if (mode === 'FIXED_INSTALLMENTS') {
    fields.add('firstDueDate');
    fields.add('termMonths');
    if (calculationMethod === 'INSTALLMENT_KNOWN') {
      fields.add('installmentAmount');
    }
    if (calculationMethod === 'RATE_KNOWN') {
      fields.add('interestRatePercent');
      fields.add('interestRatePeriod');
    }
  } else if (mode === 'FLEXIBLE_PAYMENTS') {
    fields.add('monthlyTarget');
    if (calculationMethod === 'RATE_KNOWN') {
      fields.add('interestRatePercent');
      fields.add('interestRatePeriod');
    }
  } else if (mode === 'SINGLE_PAYMENT') {
    fields.add('firstDueDate');
    fields.add('singlePaymentAmount');
  }

  return fields;
}

export function filterObligationPayload(payload, state) {
  const allowed = activeObligationFieldNames(state);
  return Object.fromEntries(
    Object.entries(payload ?? {}).filter(([name]) => allowed.has(name)),
  );
}
