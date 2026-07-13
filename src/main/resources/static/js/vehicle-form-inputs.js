const DEFAULT_MONEY_DIGITS = 14;
const DEFAULT_ODOMETER_INTEGER_DIGITS = 11;

const asText = (value) => String(value ?? '');
const digitsOnly = (value) => asText(value).replace(/\D/g, '');

const trimLeadingZeros = (digits) => {
  const normalized = digits.replace(/^0+(?=\d)/, '');
  return normalized || '0';
};

const groupThousands = (digits) => trimLeadingZeros(digits)
  .replace(/\B(?=(\d{3})+(?!\d))/g, '.');

export const applyMoneyEdit = (
  currentDigits,
  {
    inputType = '',
    data = '',
    replaceAll = false,
    maxDigits = DEFAULT_MONEY_DIGITS
  } = {}
) => {
  const current = digitsOnly(currentDigits).slice(0, maxDigits);
  if (inputType.startsWith('delete')) {
    return replaceAll ? '' : current.slice(0, -1);
  }

  const inserted = digitsOnly(data);
  if (!inserted) return current;

  const base = replaceAll ? '' : current;
  return `${base}${inserted}`.slice(0, maxDigits);
};

export const formatMoneyInput = (raw, maxDigits = DEFAULT_MONEY_DIGITS) => {
  const digits = digitsOnly(raw).slice(0, maxDigits);
  if (!digits) return '';

  const padded = digits.padStart(3, '0');
  const integerDigits = padded.slice(0, -2);
  const cents = padded.slice(-2);
  return `${groupThousands(integerDigits)},${cents}`;
};

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

export const formatVehiclePlate = (raw) => {
  const normalized = asText(raw)
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 7);

  if (/^[A-Z]{3}\d{1,4}$/.test(normalized) && normalized.length > 3) {
    return `${normalized.slice(0, 3)}-${normalized.slice(3)}`;
  }

  return normalized;
};

export const normalizeVehicleText = (raw) => asText(raw)
  .trim()
  .replace(/\s+/g, ' ');
