const MAX_FLEXIBLE_MONTHS = 1_200;
const RATE_ITERATIONS = 180;

export function inferMonthlyRate(principal, installmentAmount, termMonths) {
  validatePositive(principal, 'principalAmount');
  validatePositive(installmentAmount, 'installmentAmount');
  validateTerm(termMonths);

  const interestFreePayment = principal / termMonths;
  if (installmentAmount + 0.005 < interestFreePayment) {
    throw calculationError(
      'installmentAmount',
      'A parcela informada não quita o valor financiado dentro desse prazo.',
    );
  }
  if (Math.abs(installmentAmount - interestFreePayment) < 0.005) return 0;

  let low = 0;
  let high = 1;
  while (paymentForRate(principal, high, termMonths) < installmentAmount && high < 1_024) {
    high *= 2;
  }
  if (paymentForRate(principal, high, termMonths) < installmentAmount) {
    throw calculationError(
      'installmentAmount',
      'Não foi possível encontrar uma taxa compatível com a parcela informada.',
    );
  }

  for (let index = 0; index < RATE_ITERATIONS; index += 1) {
    const middle = (low + high) / 2;
    if (paymentForRate(principal, middle, termMonths) < installmentAmount) {
      low = middle;
    } else {
      high = middle;
    }
  }
  return (low + high) / 2;
}

export function paymentForRate(principal, monthlyRate, termMonths) {
  if (monthlyRate === 0) return principal / termMonths;
  const factor = (1 + monthlyRate) ** termMonths;
  return principal * monthlyRate * factor / (factor - 1);
}

export function buildObligationPreview(input = {}) {
  try {
    const principal = numeric(input.principalAmount);
    validatePositive(principal, 'principalAmount');

    if (input.mode === 'FIXED_INSTALLMENTS') {
      return fixedPreview(input, principal);
    }
    if (input.mode === 'FLEXIBLE_PAYMENTS') {
      return flexiblePreview(input, principal);
    }
    if (input.mode === 'SINGLE_PAYMENT') {
      return singlePreview(input, principal);
    }
    return invalid('mode', 'Escolha como o pagamento será feito.');
  } catch (error) {
    return invalid(error.field ?? 'form', error.message ?? 'Não foi possível calcular a prévia.');
  }
}

function fixedPreview(input, principal) {
  const termMonths = Number(input.termMonths);
  validateTerm(termMonths);
  if (!input.firstDueDate) {
    throw calculationError('firstDueDate', 'Informe o primeiro vencimento.');
  }

  let monthlyRate;
  let installmentAmount;
  if (input.calculationMethod === 'INSTALLMENT_KNOWN') {
    installmentAmount = numeric(input.installmentAmount);
    monthlyRate = inferMonthlyRate(principal, installmentAmount, termMonths);
  } else if (input.calculationMethod === 'RATE_KNOWN') {
    const ratePercent = numeric(input.interestRatePercent);
    if (ratePercent == null || ratePercent < 0) {
      throw calculationError('interestRatePercent', 'Informe uma taxa válida.');
    }
    monthlyRate = normalizeMonthlyRate(ratePercent / 100, input.interestRatePeriod);
    installmentAmount = roundMoney(paymentForRate(principal, monthlyRate, termMonths));
  } else if (input.calculationMethod === 'INTEREST_FREE') {
    monthlyRate = 0;
    installmentAmount = roundMoney(principal / termMonths);
  } else {
    throw calculationError('calculationMethod', 'Informe quais dados do contrato você conhece.');
  }

  const schedule = buildSchedule(principal, installmentAmount, monthlyRate, termMonths);
  return {
    valid: true,
    mode: input.mode,
    calculationMethod: input.calculationMethod,
    installmentAmount,
    totalAmount: schedule.totalAmount,
    totalInterest: roundMoney(schedule.totalAmount - principal),
    monthlyRatePercent: monthlyRate * 100,
    annualRatePercent: ((1 + monthlyRate) ** 12 - 1) * 100,
    firstDueDate: input.firstDueDate,
    lastDueDate: addMonths(input.firstDueDate, termMonths - 1),
    amortizes: true,
    warning: null,
  };
}

function flexiblePreview(input, principal) {
  const target = numeric(input.monthlyTarget);
  validatePositive(target, 'monthlyTarget');

  let monthlyRate = null;
  if (input.calculationMethod === 'INTEREST_FREE') {
    monthlyRate = 0;
  } else if (input.calculationMethod === 'RATE_KNOWN') {
    const ratePercent = numeric(input.interestRatePercent);
    if (ratePercent == null || ratePercent < 0) {
      throw calculationError('interestRatePercent', 'Informe uma taxa válida.');
    }
    monthlyRate = normalizeMonthlyRate(ratePercent / 100, input.interestRatePeriod);
  }

  if (monthlyRate == null) {
    return {
      valid: true,
      mode: input.mode,
      monthlyTarget: target,
      monthlyRatePercent: null,
      annualRatePercent: null,
      estimatedMonths: null,
      amortizes: null,
      warning: 'A duração será estimada quando a taxa for informada.',
    };
  }

  const firstMonthInterest = roundMoney(principal * monthlyRate);
  if (target <= firstMonthInterest && monthlyRate > 0) {
    return {
      valid: true,
      mode: input.mode,
      monthlyTarget: target,
      monthlyRatePercent: monthlyRate * 100,
      annualRatePercent: ((1 + monthlyRate) ** 12 - 1) * 100,
      firstMonthInterest,
      estimatedMonths: null,
      amortizes: false,
      warning: 'O valor mensal não é suficiente para reduzir a dívida com essa taxa.',
    };
  }

  let balance = principal;
  let months = 0;
  while (balance > 0.005 && months < MAX_FLEXIBLE_MONTHS) {
    const interest = roundMoney(balance * monthlyRate);
    balance = roundMoney(balance + interest - target);
    months += 1;
  }

  return {
    valid: true,
    mode: input.mode,
    monthlyTarget: target,
    monthlyRatePercent: monthlyRate * 100,
    annualRatePercent: ((1 + monthlyRate) ** 12 - 1) * 100,
    firstMonthInterest,
    estimatedMonths: months < MAX_FLEXIBLE_MONTHS ? months : null,
    amortizes: months < MAX_FLEXIBLE_MONTHS,
    warning: months < MAX_FLEXIBLE_MONTHS
      ? null
      : 'A estimativa ultrapassa o limite de 100 anos.',
  };
}

function singlePreview(input, principal) {
  if (!input.firstDueDate) {
    throw calculationError('firstDueDate', 'Informe a data do pagamento.');
  }
  const total = input.calculationMethod === 'INTEREST_FREE'
    ? principal
    : numeric(input.singlePaymentAmount);
  validatePositive(total, 'singlePaymentAmount');
  if (total + 0.005 < principal) {
    throw calculationError(
      'singlePaymentAmount',
      'O valor a pagar não pode ser menor que o valor recebido.',
    );
  }
  return {
    valid: true,
    mode: input.mode,
    installmentAmount: roundMoney(total),
    totalAmount: roundMoney(total),
    totalInterest: roundMoney(total - principal),
    firstDueDate: input.firstDueDate,
    lastDueDate: input.firstDueDate,
    monthlyRatePercent: null,
    annualRatePercent: null,
    amortizes: true,
    warning: null,
  };
}

function buildSchedule(principal, installmentAmount, monthlyRate, termMonths) {
  let balance = roundMoney(principal);
  let totalAmount = 0;

  for (let sequence = 1; sequence <= termMonths; sequence += 1) {
    const interest = roundMoney(balance * monthlyRate);
    let principalPart;
    let total;
    if (sequence === termMonths) {
      principalPart = balance;
      total = roundMoney(principalPart + interest);
    } else {
      principalPart = roundMoney(installmentAmount - interest);
      if (principalPart <= 0) {
        throw calculationError(
          'installmentAmount',
          'A parcela informada não é suficiente para amortizar a dívida.',
        );
      }
      if (principalPart > balance) principalPart = balance;
      total = roundMoney(principalPart + interest);
    }
    balance = roundMoney(balance - principalPart);
    totalAmount = roundMoney(totalAmount + total);
  }

  return { totalAmount };
}

function normalizeMonthlyRate(rateRatio, period) {
  if (period === 'MONTHLY') return rateRatio;
  if (period === 'ANNUAL') return (1 + rateRatio) ** (1 / 12) - 1;
  throw calculationError('interestRatePeriod', 'Informe se a taxa é mensal ou anual.');
}

export function addMonths(isoDate, amount) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate ?? '');
  if (!match) return null;
  const year = Number(match[1]);
  const monthIndex = Number(match[2]) - 1;
  const day = Number(match[3]);
  const targetMonthIndex = monthIndex + amount;
  const targetYear = year + Math.floor(targetMonthIndex / 12);
  const normalizedMonth = ((targetMonthIndex % 12) + 12) % 12;
  const lastDay = new Date(Date.UTC(targetYear, normalizedMonth + 1, 0)).getUTCDate();
  const targetDay = Math.min(day, lastDay);
  return [
    String(targetYear).padStart(4, '0'),
    String(normalizedMonth + 1).padStart(2, '0'),
    String(targetDay).padStart(2, '0'),
  ].join('-');
}

export function suggestFirstDueDate(startDate) {
  return addMonths(startDate, 1);
}

function numeric(value) {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function validatePositive(value, field) {
  if (value == null || value <= 0) {
    throw calculationError(field, 'Informe um valor maior que zero.');
  }
}

function validateTerm(termMonths) {
  if (!Number.isInteger(termMonths) || termMonths < 1 || termMonths > 600) {
    throw calculationError('termMonths', 'Informe de 1 a 600 parcelas.');
  }
}

function roundMoney(value) {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

function calculationError(field, message) {
  const error = new Error(message);
  error.field = field;
  return error;
}

function invalid(errorField, message) {
  return { valid: false, errorField, message };
}
