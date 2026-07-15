const DECISION_PREFIX = 'renda-com-carro:time-zone-decision:';

export function decisionKey(saved, detected) {
  return `${DECISION_PREFIX}${saved ?? ''}|${detected ?? ''}`;
}

function storageValue(storage, key) {
  if (!storage) return null;
  if (typeof storage.getItem === 'function') return storage.getItem(key);
  if (typeof storage.get === 'function') return storage.get(key) ?? null;
  return null;
}

export function shouldPromptForTimeZone(saved, detected, storage) {
  if (!saved || !detected || saved === detected) return false;
  return storageValue(storage, decisionKey(saved, detected)) !== 'keep';
}

export function civilDateInTimeZone(date = new Date(), timeZone) {
  const formatter = new Intl.DateTimeFormat('en', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
  const parts = Object.fromEntries(
    formatter.formatToParts(date)
      .filter((part) => part.type !== 'literal')
      .map((part) => [part.type, part.value])
  );
  return `${parts.year}-${parts.month}-${parts.day}`;
}

export function detectDeviceTimeZone() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || null;
  } catch {
    return null;
  }
}

function csrfHeaders(documentRef) {
  const token = documentRef.querySelector('meta[name="_csrf"]')?.content;
  const header = documentRef.querySelector('meta[name="_csrf_header"]')?.content;
  return token && header ? { [header]: token } : {};
}

function dispatchTimeZoneChanged(documentRef, timeZoneId) {
  documentRef.documentElement.dataset.activeTimeZone = timeZoneId;
  documentRef.dispatchEvent(new CustomEvent('app:time-zone-changed', {
    detail: { timeZoneId }
  }));
}

async function saveTimeZone(documentRef, fetchImpl, timeZoneId) {
  const response = await fetchImpl('/api/user-preferences/time-zone', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders(documentRef)
    },
    body: JSON.stringify({ timeZoneId })
  });
  if (!response.ok) {
    throw new Error('Não foi possível salvar o fuso horário.');
  }
  return response.json();
}

export async function initializeUserTimeZone({
  documentRef = document,
  storage = sessionStorage,
  fetchImpl = fetch,
  detectedTimeZone = detectDeviceTimeZone()
} = {}) {
  const root = documentRef.documentElement;
  const saved = root.dataset.savedTimeZone || '';
  const fallback = root.dataset.defaultTimeZone || 'America/Sao_Paulo';
  const banner = documentRef.querySelector('[data-time-zone-banner]');
  const detectedText = documentRef.querySelector('[data-detected-time-zone]');
  const savedText = documentRef.querySelector('[data-saved-time-zone]');
  const useDeviceButton = documentRef.querySelector('[data-use-device-time-zone]');
  const keepSavedButton = documentRef.querySelector('[data-keep-saved-time-zone]');
  const status = documentRef.querySelector('[data-time-zone-status]');

  if (!detectedTimeZone) {
    root.dataset.activeTimeZone = saved || fallback;
    return;
  }

  if (!saved) {
    dispatchTimeZoneChanged(documentRef, detectedTimeZone);
    try {
      await saveTimeZone(documentRef, fetchImpl, detectedTimeZone);
      root.dataset.savedTimeZone = detectedTimeZone;
    } catch {
      if (status) status.textContent = 'O fuso do dispositivo está em uso, mas não foi possível salvá-lo.';
    }
    return;
  }

  root.dataset.activeTimeZone = saved;
  if (!shouldPromptForTimeZone(saved, detectedTimeZone, storage) || !banner) return;

  if (detectedText) detectedText.textContent = detectedTimeZone;
  if (savedText) savedText.textContent = saved;
  banner.hidden = false;

  useDeviceButton?.addEventListener('click', async () => {
    useDeviceButton.disabled = true;
    keepSavedButton && (keepSavedButton.disabled = true);
    if (status) status.textContent = 'Atualizando fuso horário…';
    try {
      await saveTimeZone(documentRef, fetchImpl, detectedTimeZone);
      root.dataset.savedTimeZone = detectedTimeZone;
      dispatchTimeZoneChanged(documentRef, detectedTimeZone);
      banner.hidden = true;
      if (status) status.textContent = 'Fuso horário atualizado.';
    } catch (error) {
      useDeviceButton.disabled = false;
      keepSavedButton && (keepSavedButton.disabled = false);
      if (status) status.textContent = error.message;
    }
  });

  keepSavedButton?.addEventListener('click', () => {
    storage.setItem(decisionKey(saved, detectedTimeZone), 'keep');
    banner.hidden = true;
    if (status) status.textContent = `Fuso configurado mantido: ${saved}.`;
  });
}

export function scheduleUserTimeZoneInitialization(
  initializer = () => initializeUserTimeZone().catch(() => {}),
  scheduler = globalThis.queueMicrotask ?? ((callback) => Promise.resolve().then(callback))
) {
  scheduler(initializer);
}

if (typeof document !== 'undefined') {
  scheduleUserTimeZoneInitialization();
}
