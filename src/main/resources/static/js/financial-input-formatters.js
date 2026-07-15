const MONEY_FORMATTER = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const PERCENTAGE_FORMATTER = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});

export function sanitizeFinancialEntry(value) {
  const raw = String(value ?? '').trim();
  if (!raw) return '';

  const cleaned = raw.replace(/[^0-9.,-]/g, '');
  const negative = cleaned.startsWith('-');
  const unsigned = cleaned.replace(/-/g, '');
  const comma = unsigned.lastIndexOf(',');
  const dot = unsigned.lastIndexOf('.');

  let decimalIndex = -1;
  if (comma >= 0 && dot >= 0) {
    decimalIndex = Math.max(comma, dot);
  } else if (comma >= 0) {
    decimalIndex = unsigned.indexOf(',');
  } else if (dot >= 0) {
    decimalIndex = unsigned.indexOf('.');
  }

  if (decimalIndex < 0) {
    const digits = unsigned.replace(/\D/g, '');
    return `${negative ? '-' : ''}${digits}`;
  }

  const integerDigits = unsigned.slice(0, decimalIndex).replace(/\D/g, '');
  const decimalDigits = unsigned.slice(decimalIndex + 1).replace(/\D/g, '');
  return `${negative ? '-' : ''}${integerDigits || '0'},${decimalDigits}`;
}

export function parseFinancialDecimal(value) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  const raw = String(value ?? '').trim();
  if (!raw) return null;

  const negative = raw.includes('-');
  const cleaned = raw.replace(/[^0-9.,]/g, '');
  if (!cleaned) return null;

  const comma = cleaned.lastIndexOf(',');
  const dot = cleaned.lastIndexOf('.');
  let decimalIndex = -1;

  if (comma >= 0 && dot >= 0) {
    decimalIndex = Math.max(comma, dot);
  } else if (comma >= 0) {
    decimalIndex = comma;
  } else if (dot >= 0) {
    const occurrences = (cleaned.match(/\./g) ?? []).length;
    const digitsAfter = cleaned.length - dot - 1;
    decimalIndex = occurrences === 1 && digitsAfter !== 3 ? dot : -1;
  }

  let normalized;
  if (decimalIndex >= 0) {
    const integerDigits = cleaned.slice(0, decimalIndex).replace(/\D/g, '') || '0';
    const decimalDigits = cleaned.slice(decimalIndex + 1).replace(/\D/g, '');
    normalized = `${integerDigits}.${decimalDigits || '0'}`;
  } else {
    normalized = cleaned.replace(/\D/g, '');
  }

  const parsed = Number(`${negative ? '-' : ''}${normalized}`);
  return Number.isFinite(parsed) ? parsed : null;
}

export function formatFinancialMoney(value) {
  const parsed = parseFinancialDecimal(value);
  return parsed == null ? '' : MONEY_FORMATTER.format(parsed);
}

export function formatFinancialPercentage(value) {
  const parsed = parseFinancialDecimal(value);
  return parsed == null ? '' : PERCENTAGE_FORMATTER.format(parsed);
}

export function parseCanonicalFinancialDecimal(value) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  const raw = String(value ?? '').trim();
  if (!/^-?\d+(?:\.\d+)?$/.test(raw)) return null;
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
}

export function formatCanonicalMoney(value) {
  const parsed = parseCanonicalFinancialDecimal(value);
  return parsed == null ? '' : MONEY_FORMATTER.format(parsed);
}

export function formatCanonicalPercentage(value) {
  const parsed = parseCanonicalFinancialDecimal(value);
  return parsed == null ? '' : PERCENTAGE_FORMATTER.format(parsed);
}
