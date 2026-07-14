const asText = (value) => String(value ?? '');

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
