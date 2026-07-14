import test from 'node:test';
import assert from 'node:assert/strict';

import {
  initializeVehicleForm,
  serializeVehicleForm
} from '../../main/resources/static/js/vehicle-form.js';

const createClassList = (initial = []) => {
  const values = new Set(initial);
  return {
    add: (...items) => items.forEach((item) => values.add(item)),
    remove: (...items) => items.forEach((item) => values.delete(item)),
    contains: (item) => values.has(item),
    toggle(item, force) {
      if (force === true) values.add(item);
      else if (force === false) values.delete(item);
      else if (values.has(item)) values.delete(item);
      else values.add(item);
      return values.has(item);
    }
  };
};

const createElement = ({
  value = '',
  name = '',
  type = 'text',
  valid = true,
  classes = [],
  dataset = {},
  textContent = ''
} = {}) => {
  const listeners = new Map();
  const attributes = new Map();
  const selectorResults = new Map();

  const element = {
    value,
    name,
    type,
    valid,
    disabled: false,
    checked: false,
    hidden: false,
    focused: false,
    checkCount: 0,
    reportCount: 0,
    dataset: { ...dataset },
    textContent,
    style: {},
    classList: createClassList(classes),
    attributes,
    selectionStart: value.length,
    selectionEnd: value.length,
    step: null,
    group: null,
    ownerDocument: null,
    checkValidity() {
      this.checkCount += 1;
      return this.valid;
    },
    reportValidity() {
      this.reportCount += 1;
      return this.checkValidity();
    },
    focus() { this.focused = true; },
    scrollIntoView() { this.scrolled = true; },
    setSelectionRange(start, end) {
      this.selectionStart = start;
      this.selectionEnd = end;
    },
    setAttribute(attribute, nextValue) {
      attributes.set(attribute, String(nextValue));
    },
    getAttribute(attribute) {
      return attributes.get(attribute) ?? null;
    },
    removeAttribute(attribute) {
      attributes.delete(attribute);
    },
    addEventListener(eventType, listener) {
      const values = listeners.get(eventType) || [];
      values.push(listener);
      listeners.set(eventType, values);
    },
    fire(eventType, overrides = {}) {
      const event = {
        type: eventType,
        target: element,
        defaultPrevented: false,
        preventDefault() { this.defaultPrevented = true; },
        ...overrides
      };
      for (const listener of listeners.get(eventType) || []) listener(event);
      return event;
    },
    querySelector(selector) {
      const valueForSelector = selectorResults.get(selector);
      return Array.isArray(valueForSelector) ? valueForSelector[0] ?? null : valueForSelector ?? null;
    },
    querySelectorAll(selector) {
      const valueForSelector = selectorResults.get(selector);
      if (Array.isArray(valueForSelector)) return valueForSelector;
      return valueForSelector ? [valueForSelector] : [];
    },
    setSelector(selector, result) {
      selectorResults.set(selector, result);
    },
    closest(selector) {
      if (selector === '[data-vehicle-step]') return this.step;
      if (selector === '.input-group') return this.group;
      return null;
    },
    matches(selector) {
      return selector === 'form' && this.tagName === 'FORM';
    }
  };

  return element;
};

const createStep = (name, controls) => {
  const step = createElement({ dataset: { vehicleStep: name } });
  const heading = createElement({ textContent: name === 'identification' ? 'Identificação' : 'Dados de operação' });
  const error = createElement();
  error.hidden = true;
  step.setSelector('input, select, textarea', controls);
  step.setSelector('[data-vehicle-step-heading]', heading);
  step.setSelector('[data-vehicle-step-error]', error);
  for (const control of controls) control.step = step;
  return { step, heading, error };
};

const createMediaQuery = (initial) => {
  const listeners = [];
  return {
    matches: initial,
    addEventListener(type, listener) {
      if (type === 'change') listeners.push(listener);
    },
    setMatches(matches) {
      this.matches = matches;
      for (const listener of listeners) listener({ matches });
    }
  };
};

const createFlowHarness = ({
  mobile = true,
  identificationValid = true,
  operationValid = true,
  serverInvalidNames = [],
  purchasePriceValue = ''
} = {}) => {
  const documentListeners = new Map();
  const documentObject = {
    nodeType: 9,
    defaultView: { Event: class {} },
    addEventListener(type, listener) {
      const values = documentListeners.get(type) || [];
      values.push(listener);
      documentListeners.set(type, values);
    },
    querySelector() { return null; }
  };

  const classesFor = (name) => serverInvalidNames.includes(name) ? ['is-invalid'] : [];
  const makeInput = createElement({ name: 'make', value: 'Renault', valid: identificationValid, classes: classesFor('make') });
  const modelInput = createElement({ name: 'model', value: 'Sandero', valid: identificationValid, classes: classesFor('model') });
  const yearInput = createElement({ name: 'year', value: '2013', valid: identificationValid, classes: classesFor('year') });
  const plateInput = createElement({ name: 'plate', value: 'ABC1D23', valid: identificationValid, classes: classesFor('plate') });
  const nameInput = createElement({ name: 'name', value: '', classes: classesFor('name') });
  const fuelInput = createElement({ name: 'fuelType', value: 'FLEX', valid: operationValid, classes: classesFor('fuelType') });
  const odometerInput = createElement({ name: 'initialOdometer', value: '10.000', valid: operationValid, classes: classesFor('initialOdometer') });
  const purchasePriceInput = createElement({ name: 'purchasePrice', value: purchasePriceValue, classes: classesFor('purchasePrice') });

  for (const control of [makeInput, modelInput, yearInput, plateInput, nameInput, fuelInput, odometerInput, purchasePriceInput]) {
    control.ownerDocument = documentObject;
  }

  const identification = createStep('identification', [makeInput, modelInput, yearInput, plateInput, nameInput]);
  const operation = createStep('operation', [fuelInput, odometerInput, purchasePriceInput]);

  const progress = createElement();
  progress.hidden = true;
  const progressCopy = createElement({ textContent: 'Etapa 1 de 2' });
  const progressName = createElement({ textContent: 'Identificação' });
  const progressBar = createElement();
  const progressRole = createElement();
  progress.setSelector('[role="progressbar"]', progressRole);

  const acquisitionToggle = createElement({ type: 'button', textContent: 'Informações de aquisição' });
  acquisitionToggle.hidden = true;
  acquisitionToggle.setAttribute('aria-expanded', 'true');
  const acquisitionIcon = createElement({ textContent: '▾' });
  acquisitionToggle.setSelector('[data-vehicle-acquisition-icon]', acquisitionIcon);
  const acquisitionPanel = createElement();
  const nameHelp = createElement({ textContent: 'Se ficar vazio, usaremos marca e modelo como identificação.' });

  const previousButton = createElement({ type: 'button', textContent: 'Voltar' });
  previousButton.hidden = true;
  const nextButton = createElement({ type: 'button', textContent: 'Continuar' });
  nextButton.hidden = true;
  const submitButton = createElement({ type: 'submit', textContent: 'Cadastrar veículo' });
  const submitSpinner = createElement();
  submitSpinner.hidden = true;
  const submitCopy = createElement({ textContent: 'Cadastrar veículo' });
  submitButton.setSelector('[data-vehicle-submit-spinner]', submitSpinner);
  submitButton.setSelector('[data-vehicle-submit-copy]', submitCopy);
  const backLink = createElement({ textContent: 'Veículos' });
  const cancelLink = createElement({ textContent: 'Cancelar' });

  const form = createElement();
  form.tagName = 'FORM';
  form.ownerDocument = documentObject;
  form.elements = [makeInput, modelInput, yearInput, plateInput, nameInput, fuelInput, odometerInput, purchasePriceInput];
  form.setSelector('[data-vehicle-submit]', submitButton);
  form.setSelector('[data-vehicle-plate]', [plateInput]);
  form.setSelector('[data-money-input]', []);
  form.setSelector('[data-percentage-input]', []);
  form.setSelector('[data-odometer-input]', []);
  form.setSelector('[data-normalize-spaces]', []);
  form.setSelector('[data-trim-outer-whitespace]', []);
  form.setSelector('[data-vehicle-step]', [identification.step, operation.step]);
  form.setSelector('[data-vehicle-progress]', progress);
  form.setSelector('[data-vehicle-progress-copy]', progressCopy);
  form.setSelector('[data-vehicle-progress-name]', progressName);
  form.setSelector('[data-vehicle-progress-bar]', progressBar);
  form.setSelector('[data-vehicle-previous]', previousButton);
  form.setSelector('[data-vehicle-next]', nextButton);
  form.setSelector('[data-vehicle-cancel-action]', cancelLink);
  form.setSelector('[data-vehicle-cancel]', cancelLink);
  form.setSelector('[data-vehicle-acquisition-toggle]', acquisitionToggle);
  form.setSelector('[data-vehicle-acquisition-panel]', acquisitionPanel);
  form.setSelector('[data-vehicle-name-help]', nameHelp);
  const serverInvalidControls = form.elements.filter((control) => control.classList.contains('is-invalid'));
  form.setSelector('.is-invalid', serverInvalidControls);
  form.setSelector(':invalid, .is-invalid', []);
  form.setSelector('input, select, textarea', form.elements);
  form.checkValidity = () => form.elements.every((control) => control.valid);

  documentObject.querySelector = (selector) => selector === '#vehicleForm' ? form : null;
  documentObject.querySelectorAll = (selector) => (
    selector === '[data-vehicle-cancel]' ? [backLink, cancelLink] : []
  );

  const mediaQuery = createMediaQuery(mobile);
  const windowListeners = new Map();
  const confirmResponses = [];
  const windowObject = {
    confirmCalls: [],
    matchMedia: () => mediaQuery,
    requestAnimationFrame: (callback) => callback(),
    confirm(message) {
      this.confirmCalls.push(message);
      return confirmResponses.length > 0 ? confirmResponses.shift() : true;
    },
    queueConfirmResponse(value) {
      confirmResponses.push(value);
    },
    addEventListener(type, listener) {
      const values = windowListeners.get(type) || [];
      values.push(listener);
      windowListeners.set(type, values);
    },
    fire(type, event = {}) {
      event.defaultPrevented ??= false;
      event.preventDefault ??= function preventDefault() {
        this.defaultPrevented = true;
      };
      for (const listener of windowListeners.get(type) || []) listener(event);
      return event;
    }
  };

  const bubbleFormEvent = (type, target) => form.fire(type, { target });

  return {
    documentObject,
    windowObject,
    mediaQuery,
    form,
    identificationStep: identification.step,
    identificationHeading: identification.heading,
    stepError: identification.error,
    operationStep: operation.step,
    operationHeading: operation.heading,
    makeInput,
    modelInput,
    yearInput,
    plateInput,
    nameInput,
    fuelInput,
    odometerInput,
    purchasePriceInput,
    firstIdentificationControl: makeInput,
    operationControl: fuelInput,
    progress,
    progressCopy,
    progressName,
    progressBar,
    progressRole,
    acquisitionToggle,
    acquisitionPanel,
    acquisitionIcon,
    nameHelp,
    previousButton,
    nextButton,
    submitButton,
    submitSpinner,
    submitCopy,
    backLink,
    cancelLink,
    cancelLinks: [backLink, cancelLink],
    bubbleFormEvent
  };
};

test('vehicle form: invalid submit keeps button enabled and focuses first invalid field', () => {
  const harness = createFlowHarness({ identificationValid: false });
  harness.form.setSelector(':invalid, .is-invalid', [harness.makeInput]);
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  const event = harness.form.fire('submit');

  assert.equal(event.defaultPrevented, true);
  assert.equal(harness.submitButton.disabled, false);
  assert.equal(harness.makeInput.focused, true);
  assert.equal(harness.makeInput.attributes.get('aria-invalid'), 'true');
});

test('vehicle form: pageshow restores submit button after a valid submission', () => {
  const harness = createFlowHarness();
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.form.fire('submit');
  assert.equal(harness.submitButton.disabled, true);
  assert.equal(harness.submitCopy.textContent, 'Salvando…');
  assert.equal(harness.submitSpinner.hidden, false);

  harness.windowObject.fire('pageshow');
  assert.equal(harness.submitButton.disabled, false);
  assert.equal(harness.submitCopy.textContent, 'Cadastrar veículo');
  assert.equal(harness.submitSpinner.hidden, true);
});

test('vehicle form: corrected server error clears invalid state on input', () => {
  const harness = createFlowHarness();
  harness.makeInput.classList.add('is-invalid');
  harness.form.setSelector('.is-invalid', [harness.makeInput]);
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.bubbleFormEvent('input', harness.makeInput);

  assert.equal(harness.makeInput.classList.contains('is-invalid'), false);
  assert.equal(harness.makeInput.attributes.has('aria-invalid'), false);
});

test('vehicle form: plate input preserves the caret while formatting', () => {
  const harness = createFlowHarness();
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.plateInput.value = 'abc1234';
  harness.plateInput.selectionStart = 4;
  harness.plateInput.selectionEnd = 4;
  harness.plateInput.fire('input');
  harness.bubbleFormEvent('input', harness.plateInput);

  assert.equal(harness.plateInput.value, 'ABC-1234');
  assert.equal(harness.plateInput.selectionStart, 5);
  assert.equal(harness.plateInput.selectionEnd, 5);
});

test('vehicle flow: mobile starts at identification and exposes only its controls', () => {
  const harness = createFlowHarness({ mobile: true });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);

  assert.equal(controller.getCurrentStep(), 'identification');
  assert.equal(harness.identificationStep.hidden, false);
  assert.equal(harness.operationStep.hidden, true);
  assert.equal(harness.nextButton.hidden, false);
  assert.equal(harness.previousButton.hidden, true);
  assert.equal(harness.submitButton.hidden, true);
  assert.equal(harness.progress.hidden, false);
  assert.equal(harness.progressCopy.textContent, 'Etapa 1 de 2');
  assert.equal(harness.progressName.textContent, 'Identificação');
});

test('vehicle flow: desktop keeps both steps visible and final submit available', () => {
  const harness = createFlowHarness({ mobile: false });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  assert.equal(harness.identificationStep.hidden, false);
  assert.equal(harness.operationStep.hidden, false);
  assert.equal(harness.progress.hidden, true);
  assert.equal(harness.nextButton.hidden, true);
  assert.equal(harness.previousButton.hidden, true);
  assert.equal(harness.submitButton.hidden, false);
});

test('vehicle flow: continue validates identification only and blocks on its first error', () => {
  const harness = createFlowHarness({ mobile: true, identificationValid: false, operationValid: false });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.nextButton.fire('click');

  assert.equal(harness.operationControl.checkCount, 0);
  assert.equal(harness.identificationStep.hidden, false);
  assert.equal(harness.stepError.hidden, false);
  assert.equal(harness.firstIdentificationControl.focused, true);
});

test('vehicle flow: valid identification advances and back returns without changing values', () => {
  const harness = createFlowHarness({ mobile: true });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);
  harness.makeInput.value = 'Renault';

  harness.nextButton.fire('click');
  assert.equal(controller.getCurrentStep(), 'operation');
  assert.equal(harness.identificationStep.hidden, true);
  assert.equal(harness.operationStep.hidden, false);
  assert.equal(harness.previousButton.hidden, false);
  assert.equal(harness.cancelLink.hidden, true);
  assert.equal(harness.submitButton.hidden, false);
  assert.equal(harness.progressCopy.textContent, 'Etapa 2 de 2');
  assert.equal(harness.operationHeading.focused, true);

  harness.previousButton.fire('click');
  assert.equal(controller.getCurrentStep(), 'identification');
  assert.equal(harness.makeInput.value, 'Renault');
  assert.equal(harness.identificationHeading.focused, true);
});

test('vehicle flow: resize preserves current mobile step', () => {
  const harness = createFlowHarness({ mobile: true });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);
  harness.nextButton.fire('click');
  assert.equal(controller.getCurrentStep(), 'operation');

  harness.mediaQuery.setMatches(false);
  assert.equal(harness.identificationStep.hidden, false);
  assert.equal(harness.operationStep.hidden, false);

  harness.mediaQuery.setMatches(true);
  assert.equal(controller.getCurrentStep(), 'operation');
  assert.equal(harness.identificationStep.hidden, true);
  assert.equal(harness.operationStep.hidden, false);
});

test('vehicle flow: first server error selects its mobile step', () => {
  const harness = createFlowHarness({
    mobile: true,
    serverInvalidNames: ['fuelType']
  });

  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);

  assert.equal(controller.getCurrentStep(), 'operation');
  assert.equal(harness.identificationStep.hidden, true);
  assert.equal(harness.operationStep.hidden, false);
  assert.equal(harness.fuelInput.focused, true);
});

test('vehicle flow: purchase price error opens operation and acquisition', () => {
  const harness = createFlowHarness({
    mobile: true,
    serverInvalidNames: ['purchasePrice']
  });

  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);

  assert.equal(controller.getCurrentStep(), 'operation');
  assert.equal(harness.acquisitionToggle.hidden, false);
  assert.equal(harness.acquisitionPanel.hidden, false);
  assert.equal(harness.acquisitionToggle.getAttribute('aria-expanded'), 'true');
  assert.equal(harness.acquisitionIcon.textContent, '▾');
});

test('vehicle flow: clean mobile acquisition starts collapsed', () => {
  const harness = createFlowHarness({ mobile: true });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  assert.equal(harness.acquisitionToggle.hidden, false);
  assert.equal(harness.acquisitionPanel.hidden, true);
  assert.equal(harness.acquisitionToggle.getAttribute('aria-expanded'), 'false');
  assert.equal(harness.acquisitionIcon.textContent, '▸');
});

test('vehicle flow: desktop keeps acquisition visible without disclosure', () => {
  const harness = createFlowHarness({ mobile: false });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  assert.equal(harness.acquisitionToggle.hidden, true);
  assert.equal(harness.acquisitionPanel.hidden, false);
});

test('vehicle flow: existing price opens acquisition and user choice survives step changes', () => {
  const harness = createFlowHarness({ mobile: true, purchasePriceValue: '23.990,00' });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);

  assert.equal(harness.acquisitionPanel.hidden, false);
  harness.acquisitionToggle.fire('click');
  assert.equal(harness.acquisitionPanel.hidden, true);

  controller.showStep('operation');
  controller.showStep('identification');
  controller.showStep('operation');
  assert.equal(harness.acquisitionPanel.hidden, true);
});

test('vehicle flow: make and model update contextual name help without changing nickname', () => {
  const harness = createFlowHarness({ mobile: true });
  initializeVehicleForm(harness.documentObject, harness.windowObject);
  harness.nameInput.value = 'Meu carro';
  harness.makeInput.value = '  Renault  ';
  harness.modelInput.value = '  Sandero  ';

  harness.bubbleFormEvent('input', harness.makeInput);

  assert.equal(
    harness.nameHelp.textContent,
    'Se ficar vazio, o veículo será exibido como Renault Sandero.'
  );
  assert.equal(harness.nameInput.value, 'Meu carro');
});

test('vehicle form: serialization is stable and ignores action controls', () => {
  const harness = createFlowHarness();

  const initial = serializeVehicleForm(harness.form);
  harness.nextButton.textContent = 'Próximo';

  assert.equal(serializeVehicleForm(harness.form), initial);
  assert.match(initial, /"make","Renault"/);
});

test('vehicle form: changing and restoring a value clears real dirty state', () => {
  const harness = createFlowHarness();
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.makeInput.value = 'Toyota';
  harness.bubbleFormEvent('input', harness.makeInput);
  assert.equal(controller.isDirty(), true);

  harness.makeInput.value = 'Renault';
  harness.bubbleFormEvent('input', harness.makeInput);
  assert.equal(controller.isDirty(), false);
});

test('vehicle form: step and acquisition changes do not mark values dirty', () => {
  const harness = createFlowHarness();
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.nextButton.fire('click');
  harness.acquisitionToggle.fire('click');
  harness.previousButton.fire('click');

  assert.equal(controller.isDirty(), false);
});

test('vehicle form: unchanged cancel navigates without confirmation', () => {
  const harness = createFlowHarness();
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  const event = harness.cancelLink.fire('click');

  assert.equal(event.defaultPrevented, false);
  assert.deepEqual(harness.windowObject.confirmCalls, []);
});

test('vehicle form: changed cancel stays when discard is refused', () => {
  const harness = createFlowHarness();
  initializeVehicleForm(harness.documentObject, harness.windowObject);
  harness.makeInput.value = 'Toyota';
  harness.bubbleFormEvent('input', harness.makeInput);
  harness.windowObject.queueConfirmResponse(false);

  const event = harness.backLink.fire('click');

  assert.equal(event.defaultPrevented, true);
  assert.deepEqual(
    harness.windowObject.confirmCalls,
    ['Descartar as alterações deste veículo?']
  );
});

test('vehicle form: confirmed discard disables unload protection', () => {
  const harness = createFlowHarness();
  initializeVehicleForm(harness.documentObject, harness.windowObject);
  harness.makeInput.value = 'Toyota';
  harness.bubbleFormEvent('input', harness.makeInput);
  harness.windowObject.queueConfirmResponse(true);

  const clickEvent = harness.cancelLink.fire('click');
  const unloadEvent = harness.windowObject.fire('beforeunload', {});

  assert.equal(clickEvent.defaultPrevented, false);
  assert.equal(unloadEvent.defaultPrevented, false);
});

test('vehicle form: beforeunload protects only real changes outside submission', () => {
  const harness = createFlowHarness();
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  const cleanEvent = harness.windowObject.fire('beforeunload', {});
  assert.equal(cleanEvent.defaultPrevented, false);

  harness.makeInput.value = 'Toyota';
  harness.bubbleFormEvent('input', harness.makeInput);
  const dirtyEvent = harness.windowObject.fire('beforeunload', {});
  assert.equal(dirtyEvent.defaultPrevented, true);
  assert.equal(dirtyEvent.returnValue, '');

  harness.form.fire('submit');
  const submittingEvent = harness.windowObject.fire('beforeunload', {});
  assert.equal(submittingEvent.defaultPrevented, false);
});

test('vehicle form: native invalid event reveals the first hidden invalid step', () => {
  const harness = createFlowHarness({ mobile: true });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);
  controller.showStep('operation');
  harness.makeInput.valid = false;
  harness.form.setSelector(':invalid, .is-invalid', [harness.makeInput]);

  harness.form.fire('invalid', { target: harness.makeInput });

  assert.equal(controller.getCurrentStep(), 'identification');
  assert.equal(harness.makeInput.focused, true);
});

test('vehicle form: native invalid price reveals acquisition details', () => {
  const harness = createFlowHarness({ mobile: true });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);
  harness.purchasePriceInput.valid = false;
  harness.form.setSelector(':invalid, .is-invalid', [harness.purchasePriceInput]);

  harness.form.fire('invalid', { target: harness.purchasePriceInput });

  assert.equal(controller.getCurrentStep(), 'operation');
  assert.equal(harness.acquisitionPanel.hidden, false);
  assert.equal(harness.purchasePriceInput.focused, true);
});

test('vehicle form: final invalid identification returns mobile flow to first step', () => {
  const harness = createFlowHarness({ mobile: true });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);
  controller.showStep('operation');
  harness.makeInput.valid = false;
  harness.form.setSelector(':invalid, .is-invalid', [harness.makeInput]);

  const event = harness.form.fire('submit');

  assert.equal(event.defaultPrevented, true);
  assert.equal(controller.getCurrentStep(), 'identification');
  assert.equal(harness.makeInput.focused, true);
  assert.equal(harness.submitButton.disabled, false);
});

test('vehicle form: final invalid price opens operation acquisition', () => {
  const harness = createFlowHarness({ mobile: true });
  const controller = initializeVehicleForm(harness.documentObject, harness.windowObject);
  harness.purchasePriceInput.valid = false;
  harness.form.setSelector(':invalid, .is-invalid', [harness.purchasePriceInput]);

  const event = harness.form.fire('submit');

  assert.equal(event.defaultPrevented, true);
  assert.equal(controller.getCurrentStep(), 'operation');
  assert.equal(harness.acquisitionPanel.hidden, false);
  assert.equal(harness.purchasePriceInput.focused, true);
});

test('vehicle form: valid submit uses stable accessible loading children', () => {
  const harness = createFlowHarness({ mobile: true });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.form.fire('submit');

  assert.equal(harness.submitButton.disabled, true);
  assert.equal(harness.submitCopy.textContent, 'Salvando…');
  assert.equal(harness.submitSpinner.hidden, false);
});
