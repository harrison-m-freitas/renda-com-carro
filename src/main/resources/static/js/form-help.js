const HELP_SELECTOR = '[data-form-help], [data-goal-help]';
const TRIGGER_SELECTOR = '[data-form-help-trigger], [data-goal-help-trigger]';
const CONTENT_SELECTOR = '[data-form-help-content], [data-goal-help-content]';

export function initializeFormHelp(root, documentObject = globalThis.document) {
  const helps = Array.from(root?.querySelectorAll?.(HELP_SELECTOR) ?? []);
  if (!helps.length) return { close() {}, destroy() {} };

  let activeHelp = null;
  const cleanups = [];

  const close = (help = activeHelp) => {
    if (!help) return;
    help.classList.remove('is-open');
    help.querySelector(TRIGGER_SELECTOR)?.setAttribute('aria-expanded', 'false');
    if (activeHelp === help) activeHelp = null;
  };

  const open = (help) => {
    if (activeHelp && activeHelp !== help) close(activeHelp);
    help.classList.add('is-open');
    help.querySelector(TRIGGER_SELECTOR)?.setAttribute('aria-expanded', 'true');
    activeHelp = help;
  };

  helps.forEach((help) => {
    const trigger = help.querySelector(TRIGGER_SELECTOR);
    const content = help.querySelector(CONTENT_SELECTOR);
    if (!trigger || !content) return;

    const onClick = (event) => {
      event.preventDefault();
      event.stopPropagation();
      if (help.classList.contains('is-open')) close(help);
      else open(help);
    };

    const onKeydown = (event) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      close(help);
      trigger.focus?.();
    };

    trigger.addEventListener('click', onClick);
    trigger.addEventListener('keydown', onKeydown);
    cleanups.push(() => {
      trigger.removeEventListener('click', onClick);
      trigger.removeEventListener('keydown', onKeydown);
    });
  });

  const onDocumentClick = (event) => {
    if (activeHelp && !activeHelp.contains(event.target)) close(activeHelp);
  };

  const onDocumentKeydown = (event) => {
    if (event.key === 'Escape') close(activeHelp);
  };

  documentObject?.addEventListener?.('click', onDocumentClick);
  documentObject?.addEventListener?.('keydown', onDocumentKeydown);

  return {
    close: () => close(activeHelp),
    destroy() {
      close(activeHelp);
      cleanups.forEach((cleanup) => cleanup());
      documentObject?.removeEventListener?.('click', onDocumentClick);
      documentObject?.removeEventListener?.('keydown', onDocumentKeydown);
    },
  };
}
