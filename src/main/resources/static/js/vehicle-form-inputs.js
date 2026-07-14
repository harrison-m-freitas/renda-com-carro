const asText = (value) => String(value ?? '');

const usefulPlateCharacters = (value) => asText(value)
  .toUpperCase()
  .replace(/[^A-Z0-9]/g, '')
  .slice(0, 7);

const formatNormalizedPlate = (normalized) => {
  if (/^[A-Z]{3}\d{1,4}$/.test(normalized) && normalized.length > 3) {
    return `${normalized.slice(0, 3)}-${normalized.slice(3)}`;
  }
  return normalized;
};

const usefulOffset = (raw, offset) => asText(raw)
  .slice(0, Math.max(0, Number(offset) || 0))
  .replace(/[^A-Z0-9]/gi, '')
  .slice(0, 7)
  .length;

const renderedOffset = (formatted, usefulCount) => {
  if (usefulCount <= 0) return 0;

  let seen = 0;
  for (let index = 0; index < formatted.length; index += 1) {
    if (/[A-Z0-9]/.test(formatted[index])) seen += 1;
    if (seen >= usefulCount) return index + 1;
  }
  return formatted.length;
};

export const formatVehiclePlate = (raw) => formatNormalizedPlate(
  usefulPlateCharacters(raw)
);

export const formatVehiclePlateEdit = (raw, selectionStart, selectionEnd) => {
  const value = formatVehiclePlate(raw);
  const startCount = usefulOffset(raw, selectionStart);
  const endCount = usefulOffset(raw, selectionEnd);

  return {
    value,
    selectionStart: renderedOffset(value, startCount),
    selectionEnd: renderedOffset(value, endCount)
  };
};
