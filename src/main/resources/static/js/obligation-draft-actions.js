const STORAGE_PREFIX = "renda:draft:OBLIGATION:";

const activeDraftRoot = document.querySelector("[data-obligation-active-draft-key]");
const activeDraftKey = activeDraftRoot?.dataset.obligationActiveDraftKey?.trim() ?? "";

clearStaleObligationCopies(activeDraftKey);

document.querySelectorAll("form[data-obligation-draft-discard]").forEach((form) => {
  form.addEventListener("submit", () => {
    const contextKey = form.dataset.draftKey?.trim();
    if (contextKey) removeStorageKey(`${STORAGE_PREFIX}${contextKey}`);
  });
});

export function clearStaleObligationCopies(activeContextKey, storage = globalThis.localStorage) {
  if (!storage) return;
  const activeStorageKey = activeContextKey
    ? `${STORAGE_PREFIX}${activeContextKey}`
    : null;
  const staleKeys = [];
  try {
    for (let index = 0; index < storage.length; index += 1) {
      const key = storage.key(index);
      if (key?.startsWith(STORAGE_PREFIX) && key !== activeStorageKey) {
        staleKeys.push(key);
      }
    }
    staleKeys.forEach((key) => storage.removeItem(key));
  } catch {
    // Storage can be unavailable in restricted browser contexts.
  }
}

function removeStorageKey(key, storage = globalThis.localStorage) {
  try {
    storage?.removeItem(key);
  } catch {
    // Discard on the server remains authoritative.
  }
}
