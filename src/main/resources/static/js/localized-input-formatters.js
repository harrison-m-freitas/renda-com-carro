const DEFAULT_FIXED_SCALE_DIGITS = 14;
const DEFAULT_MONEY_DIGITS = 14;
const DEFAULT_PERCENTAGE_DIGITS = 5;
const DEFAULT_ODOMETER_INTEGER_DIGITS = 11;

const asText = (value) => String(value ?? '');
const digitsOnly = (value) => asText(value).replace(/\D/g, '');

const trimLeadingZeros = (digits) => {
  const normalized = digits.replace(/^0+(?=\d)/, '');
  return normalized || '0';
};

const groupThousands = (digits) => trimLeadingZeros(digits)
  .replace(/\B(?=(\d{3})+(?!\d))/g, '.');

export const applyFixedScaleEdit = (
  currentDigits,
  {
    inputType = '',
    data = '',
    replaceAll = false,
    selectionStart = null,
    selectionEnd = null,
    maxDigits = DEFAULT_FIXED_SCALE_DIGITS
  } = {}
) => {
  const current = digitsOnly(currentDigits).slice(0, maxDigits);
  const hasSelection = Number.isInteger(selectionStart)
    && Number.isInteger(selectionEnd)
    && selectionEnd > selectionStart;
  const start = hasSelection ? Math.max(0, Math.min(selectionStart, current.length)) : current.length;
  const end = hasSelection ? Math.max(start, Math.min(selectionEnd, current.length)) : current.length;
  const baseBefore = replaceAll ? '' : current.slice(0, start);
  const baseAfter = replaceAll ? '' : current.slice(end);

  if (inputType.startsWith('delete')) {
    if (replaceAll) return '';
    if (hasSelection) return `${baseBefore}${baseAfter}`;
    return current.slice(0, -1);
  }

  const inserted = digitsOnly(data);
  if (!inserted) return current;

  if (hasSelection) {
    return `${baseBefore}${inserted}${baseAfter}`.slice(0, maxDigits);
  }
  const base = replaceAll ? '' : current;
  return `${base}${inserted}`.slice(0, maxDigits);
};

export const formatFixedScaleInput = (
  raw,
  {
    scale = 2,
    maxDigits = DEFAULT_FIXED_SCALE_DIGITS
  } = {}
) => {
  const digits = digitsOnly(raw).slice(0, maxDigits);
  if (!digits) return '';
  if (scale === 0) return groupThousands(digits);

  const padded = digits.padStart(scale + 1, '0');
  const integerDigits = padded.slice(0, -scale);
  const decimals = padded.slice(-scale);
  return `${groupThousands(integerDigits)},${decimals}`;
};

export const formatMoneyInput = (raw, maxDigits = DEFAULT_MONEY_DIGITS) =>
  formatFixedScaleInput(raw, { scale: 2, maxDigits });

export const formatPercentageInput = (raw, maxDigits = DEFAULT_PERCENTAGE_DIGITS) =>
  formatFixedScaleInput(raw, { scale: 2, maxDigits });

const splitOdometer = (raw) => {
  const source = asText(raw)
    .replace(/\s/g, '')
    .replace(/[^\d,.]/g, '');

  if (!source) {
    return { integerSource: '', decimalSource: '', hasDecimalSeparator: false };
  }

  const commaIndex = source.lastIndexOf(',');
  if (commaIndex >= 0) {
    return {
      integerSource: source.slice(0, commaIndex),
      decimalSource: source.slice(commaIndex + 1),
      hasDecimalSeparator: true
    };
  }

  const dotCount = (source.match(/\./g) || []).length;
  const dotIndex = source.lastIndexOf('.');
  const digitsAfterDot = dotIndex >= 0 ? source.length - dotIndex - 1 : -1;
  const groupedInteger = /^\d{1,3}(\.\d{3})+$/.test(source);
  const dotActsAsDecimal = dotCount === 1
    && dotIndex > 0
    && digitsAfterDot <= 1
    && !groupedInteger;

  if (dotActsAsDecimal) {
    return {
      integerSource: source.slice(0, dotIndex),
      decimalSource: source.slice(dotIndex + 1),
      hasDecimalSeparator: true
    };
  }

  return {
    integerSource: source,
    decimalSource: '',
    hasDecimalSeparator: false
  };
};

export const formatOdometerInput = (
  raw,
  {
    maxIntegerDigits = DEFAULT_ODOMETER_INTEGER_DIGITS,
    trimZeroFraction = false
  } = {}
) => {
  const { integerSource, decimalSource, hasDecimalSeparator } = splitOdometer(raw);
  const integerDigits = digitsOnly(integerSource).slice(0, maxIntegerDigits);

  if (!integerDigits && !hasDecimalSeparator) return '';

  const groupedInteger = groupThousands(integerDigits || '0');
  if (!hasDecimalSeparator) return groupedInteger;

  const decimalDigit = digitsOnly(decimalSource).slice(0, 1);
  if (trimZeroFraction && (!decimalDigit || decimalDigit === '0')) {
    return groupedInteger;
  }

  return `${groupedInteger},${decimalDigit}`;
};

export const parseLocalizedDecimal = (raw) => {
  if (raw === null || raw === undefined || asText(raw).trim() === '') return null;

  let normalized = asText(raw)
    .replace(/\u00a0/g, '')
    .replace(/\s/g, '')
    .replace(/[^\d,.-]/g, '');

  if (!normalized || normalized === '-' || normalized === ',' || normalized === '.') {
    return null;
  }

  if (normalized.includes(',') && normalized.includes('.')) {
    normalized = normalized.replace(/\./g, '').replace(',', '.');
  } else if (normalized.includes(',')) {
    normalized = normalized.replace(',', '.');
  } else if (/^-?\d{1,3}(\.\d{3})+$/.test(normalized)) {
    normalized = normalized.replace(/\./g, '');
  }

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : null;
};

export const normalizeSpaces = (raw) => asText(raw)
  .trim()
  .replace(/\s+/g, ' ');

export const trimOuterWhitespace = (raw) => asText(raw).trim();
