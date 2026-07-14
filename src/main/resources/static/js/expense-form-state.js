const hasOwn = (object, key) => Object.prototype.hasOwnProperty.call(object, key);
const asText = (value) => String(value ?? '').trim();

const monthFromDate = (dateValue) => /^\d{4}-\d{2}-\d{2}$/.test(asText(dateValue))
  ? asText(dateValue).slice(0, 7)
  : '';

const decimalParts = (raw, scale) => {
  const normalized = asText(raw)
    .replace(/\s/g, '')
    .replace(/%/g, '')
    .replace('.', ',')
    .replace(/[^\d,]/g, '');
  if (!normalized) return null;
  const [integerRaw = '0', decimalRaw = ''] = normalized.split(',', 2);
  const integer = BigInt((integerRaw.replace(/^0+(?=\d)/, '') || '0'));
  const decimals = decimalRaw.replace(/\D/g, '').slice(0, scale).padEnd(scale, '0');
  return integer * (10n ** BigInt(scale)) + BigInt(decimals || '0');
};

export const parseMoneyToCents = (raw) => {
  let source = asText(raw)
    .replace(/R\$/gi, '')
    .replace(/\s/g, '')
    .replace(/[^\d,.]/g, '');
  if (!source) return null;

  let integerSource = source;
  let decimalSource = '';
  if (source.includes(',')) {
    const index = source.lastIndexOf(',');
    integerSource = source.slice(0, index).replace(/\./g, '');
    decimalSource = source.slice(index + 1);
  } else {
    const dots = (source.match(/\./g) || []).length;
    const index = source.lastIndexOf('.');
    const after = index >= 0 ? source.length - index - 1 : -1;
    if (dots === 1 && after >= 1 && after <= 2) {
      integerSource = source.slice(0, index);
      decimalSource = source.slice(index + 1);
    } else {
      integerSource = source.replace(/\./g, '');
    }
  }

  const integerDigits = integerSource.replace(/\D/g, '') || '0';
  const decimalDigits = decimalSource.replace(/\D/g, '').slice(0, 2).padEnd(2, '0');
  return BigInt(integerDigits) * 100n + BigInt(decimalDigits || '0');
};

export const formatMoneyFromCents = (cents) => {
  if (cents === null || cents === undefined) return '—';
  const value = BigInt(cents);
  const sign = value < 0n ? '-' : '';
  const absolute = value < 0n ? -value : value;
  const integer = (absolute / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  const decimals = (absolute % 100n).toString().padStart(2, '0');
  return `${sign}R$ ${integer},${decimals}`;
};

const allocatePercentage = (totalCents, rawPercentage, scale) => {
  const units = decimalParts(rawPercentage, scale);
  if (units === null) return null;
  const denominator = 100n * (10n ** BigInt(scale));
  return (totalCents * units + denominator / 2n) / denominator;
};

export function createExpenseState(initial = {}) {
  const expenseDate = asText(initial.expenseDate);
  const paymentStatus = asText(initial.paymentStatus) || 'PAID';
  const explicitPaidDate = hasOwn(initial, 'paidDate') ? asText(initial.paidDate) : '';
  const paidDate = explicitPaidDate || (paymentStatus === 'PAID' ? expenseDate : '');
  const explicitMonth = hasOwn(initial, 'competenceMonth') ? asText(initial.competenceMonth) : '';
  const competenceMonth = explicitMonth || monthFromDate(expenseDate);

  return {
    vehicleId: asText(initial.vehicleId),
    categoryId: asText(initial.categoryId),
    operationalDayId: asText(initial.operationalDayId),
    shiftId: asText(initial.shiftId),
    expenseDate,
    competenceMonth,
    paidDate,
    lastPaidDate: paidDate,
    paymentStatus,
    amount: asText(initial.amount),
    classification: asText(initial.classification) || 'PROFESSIONAL',
    allocationMethod: asText(initial.allocationMethod) || 'MILEAGE_RATIO',
    professionalPercentagePercent: asText(initial.professionalPercentagePercent),
    professionalFixedAmount: asText(initial.professionalFixedAmount),
    adjustmentReason: asText(initial.adjustmentReason),
    notes: String(initial.notes ?? ''),
    paidDateManuallyEdited: initial.paidDateManuallyEdited
      ?? Boolean(explicitPaidDate && explicitPaidDate !== expenseDate),
    competenceMonthManuallyEdited: initial.competenceMonthManuallyEdited
      ?? Boolean(explicitMonth && explicitMonth !== monthFromDate(expenseDate))
  };
}

export function updateExpenseState(state, action) {
  const next = { ...state };
  switch (action.type) {
    case 'EXPENSE_DATE_CHANGED':
      next.expenseDate = asText(action.value);
      if (!next.paidDateManuallyEdited) {
        next.paidDate = next.expenseDate;
        next.lastPaidDate = next.expenseDate;
      }
      if (!next.competenceMonthManuallyEdited) {
        next.competenceMonth = monthFromDate(next.expenseDate);
      }
      return next;
    case 'PAID_DATE_CHANGED':
      next.paidDate = asText(action.value);
      next.lastPaidDate = next.paidDate;
      next.paidDateManuallyEdited = true;
      return next;
    case 'COMPETENCE_MONTH_CHANGED':
      next.competenceMonth = asText(action.value);
      next.competenceMonthManuallyEdited = true;
      return next;
    case 'PAYMENT_STATUS_CHANGED':
      next.paymentStatus = asText(action.value) || 'PAID';
      if (next.paymentStatus === 'PAID') {
        next.paidDate = next.lastPaidDate || next.expenseDate;
      } else if (next.paidDate) {
        next.lastPaidDate = next.paidDate;
      }
      return next;
    case 'CLASSIFICATION_CHANGED':
      next.classification = asText(action.value) || 'PROFESSIONAL';
      if (next.classification === 'MIXED' && !next.allocationMethod) {
        next.allocationMethod = 'MILEAGE_RATIO';
      }
      return next;
    case 'ALLOCATION_METHOD_CHANGED':
      next.allocationMethod = asText(action.value) || 'MILEAGE_RATIO';
      return next;
    case 'FIELD_CHANGED':
      next[action.field] = action.value ?? '';
      return next;
    default:
      return next;
  }
}

export function validateExpenseState(state) {
  const errors = {};
  if (state.paymentStatus === 'PAID' && !asText(state.paidDate)) {
    errors.paidDate = 'Informe a data do pagamento.';
  }

  if (state.classification !== 'MIXED') return errors;

  if (state.allocationMethod === 'MANUAL_PERCENTAGE') {
    const percentage = decimalParts(state.professionalPercentagePercent, 2);
    if (percentage === null) {
      errors.professionalPercentagePercent = 'Informe o percentual profissional.';
    } else if (percentage === 0n) {
      errors.professionalPercentagePercent = 'Para 0%, classifique o gasto como Pessoal.';
    } else if (percentage === 10000n) {
      errors.professionalPercentagePercent = 'Para 100%, classifique o gasto como Profissional.';
    } else if (percentage < 0n || percentage > 10000n) {
      errors.professionalPercentagePercent = 'Informe um percentual maior que 0% e menor que 100%.';
    }
  }

  if (state.allocationMethod === 'FIXED_AMOUNT') {
    const fixed = parseMoneyToCents(state.professionalFixedAmount);
    const total = parseMoneyToCents(state.amount);
    if (fixed === null) {
      errors.professionalFixedAmount = 'Informe o valor profissional.';
    } else if (fixed === 0n) {
      errors.professionalFixedAmount = 'Para nenhum valor profissional, classifique o gasto como Pessoal.';
    } else if (total !== null && fixed === total) {
      errors.professionalFixedAmount = 'Para atribuir todo o valor à operação, classifique o gasto como Profissional.';
    } else if (total !== null && fixed > total) {
      errors.professionalFixedAmount = 'O valor profissional deve ser menor que o valor total do gasto.';
    }
  }

  if (['MANUAL_PERCENTAGE', 'FIXED_AMOUNT'].includes(state.allocationMethod)
      && !asText(state.adjustmentReason)) {
    errors.adjustmentReason = 'Explique por que o rateio foi informado manualmente.';
  }
  return errors;
}

export function expenseDraftPayload(state) {
  const payload = {
    vehicleId: state.vehicleId || undefined,
    operationalDayId: state.operationalDayId || undefined,
    shiftId: state.shiftId || undefined,
    categoryId: state.categoryId || undefined,
    expenseDate: state.expenseDate || undefined,
    competenceMonth: state.competenceMonth || undefined,
    paymentStatus: state.paymentStatus,
    paidDate: state.paymentStatus === 'PAID' ? state.paidDate || undefined : undefined,
    amount: state.amount || undefined,
    classification: state.classification,
    notes: state.notes || undefined
  };

  if (state.classification === 'MIXED') {
    payload.allocationMethod = state.allocationMethod || 'MILEAGE_RATIO';
    if (payload.allocationMethod === 'MANUAL_PERCENTAGE') {
      payload.professionalPercentagePercent = state.professionalPercentagePercent || undefined;
      payload.adjustmentReason = state.adjustmentReason || undefined;
    } else if (payload.allocationMethod === 'FIXED_AMOUNT') {
      payload.professionalFixedAmount = state.professionalFixedAmount || undefined;
      payload.adjustmentReason = state.adjustmentReason || undefined;
    }
  }
  return Object.fromEntries(Object.entries(payload).filter(([, value]) => value !== undefined));
}

export function expenseSummary(state, allocationPreview = null) {
  const totalCents = parseMoneyToCents(state.amount) ?? 0n;
  const errors = validateExpenseState(state);
  let professionalCents = 0n;
  let calculable = true;
  let provisional = false;
  let status = 'DIRECT';

  if (state.classification === 'PROFESSIONAL') {
    professionalCents = totalCents;
  } else if (state.classification === 'PERSONAL') {
    professionalCents = 0n;
  } else if (state.allocationMethod === 'MANUAL_PERCENTAGE') {
    if (errors.professionalPercentagePercent) calculable = false;
    else professionalCents = allocatePercentage(totalCents, state.professionalPercentagePercent, 2);
  } else if (state.allocationMethod === 'FIXED_AMOUNT') {
    if (errors.professionalFixedAmount) calculable = false;
    else professionalCents = parseMoneyToCents(state.professionalFixedAmount);
  } else if (allocationPreview?.status === 'CONFIRMED' || allocationPreview?.status === 'ESTIMATED') {
    professionalCents = allocatePercentage(totalCents, allocationPreview.professionalPercentage, 4);
    provisional = Boolean(allocationPreview.provisional);
    status = allocationPreview.status;
  } else {
    calculable = false;
    professionalCents = null;
    status = allocationPreview?.status || 'AWAITING_PREVIEW';
  }

  return {
    totalCents,
    professionalCents,
    personalCents: calculable ? totalCents - professionalCents : null,
    calculable,
    provisional,
    status,
    errors
  };
}
