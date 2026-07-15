export const MOBILE_BREAKPOINT = 768;

const FRAMEWORK_CONTROL_NAMES = new Set(["_csrf", "_method"]);

export function isDraftFrameworkControlName(name) {
  return FRAMEWORK_CONTROL_NAMES.has(String(name ?? ""));
}

export function clampStep(step, maxStep) {
  const parsedStep = Number.isFinite(Number(step)) ? Number(step) : 1;
  const parsedMax = Math.max(1, Number(maxStep) || 1);
  return Math.min(Math.max(1, parsedStep), parsedMax);
}

export function nextStep(step, maxStep) {
  return clampStep(Number(step) + 1, maxStep);
}

export function previousStep(step) {
  return Math.max(1, Number(step) - 1);
}

export function isMobileWizard(width) {
  return Number(width) < MOBILE_BREAKPOINT;
}

export function draftStorageKey(type, contextKey) {
  return `renda:draft:${type}:${contextKey}`;
}

export function serializeEditableFields(form) {
  const payload = {};
  for (const field of Array.from(form?.elements ?? [])) {
    if (!field?.name || field.disabled) continue;
    if (isDraftFrameworkControlName(field.name)) continue;
    if (Object.prototype.hasOwnProperty.call(field.dataset ?? {}, "draftIgnore")) continue;

    const type = String(field.type ?? "").toLowerCase();
    if (["button", "submit", "reset", "file", "image"].includes(type)) continue;
    if (type === "checkbox"
        && Object.prototype.hasOwnProperty.call(field.dataset ?? {}, "draftArray")) {
      payload[field.name] ??= [];
      if (field.checked) payload[field.name].push(String(field.value ?? ""));
      continue;
    }
    if ((type === "checkbox" || type === "radio") && !field.checked) continue;

    const includeCalculated = Object.prototype.hasOwnProperty.call(
      field.dataset ?? {},
      "draftInclude",
    );
    if ((field.readOnly || field.disabled) && !includeCalculated) continue;

    payload[field.name] = String(field.value ?? "");
  }
  return payload;
}
