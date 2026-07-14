import test from 'node:test';
import assert from 'node:assert/strict';

import { initializeGoalWorkloadPlanner } from '../../main/resources/static/js/goal-workload-planner.js';

class FakeElement {
  constructor(tagName = 'div') {
    this.tagName = tagName.toUpperCase();
    this.className = '';
    this.textContent = '';
    this.children = [];
    this.listeners = new Map();
    this.value = '';
    this.checked = false;
    this.dispatched = [];
  }

  addEventListener(type, listener) {
    const listeners = this.listeners.get(type) ?? [];
    listeners.push(listener);
    this.listeners.set(type, listeners);
  }

  fire(type) {
    for (const listener of this.listeners.get(type) ?? []) {
      listener({ type, target: this });
    }
  }

  dispatchEvent(event) {
    this.dispatched.push(event);
    this.fire(event.type);
    return true;
  }

  append(...children) {
    this.children.push(...children);
  }

  replaceChildren(...children) {
    this.children = [...children];
  }
}

const allText = (element) => [
  element.textContent,
  ...element.children.flatMap((child) => allText(child))
].filter(Boolean).join(' ');

const createHarness = ({ dates = '' } = {}) => {
  const daily = new FakeElement('input');
  daily.value = 'DAILY';
  const weekly = new FakeElement('input');
  weekly.value = 'WEEKLY';
  weekly.checked = true;
  const monthly = new FakeElement('input');
  monthly.value = 'MONTHLY';

  const hours = new FakeElement('input');
  hours.value = '40';
  const minutes = new FakeElement('input');
  minutes.value = '0';
  const month = new FakeElement('input');
  month.value = '2026-07';
  const plannedDates = new FakeElement('textarea');
  plannedDates.value = dates;
  const summary = new FakeElement('div');
  const review = new FakeElement('strong');

  const fields = {
    '[name="workloadHours"]': hours,
    '[name="workloadMinutes"]': minutes,
    '[name="month"]': month,
    '[name="plannedDates"]': plannedDates,
    '[data-workload-summary]': summary,
    '[data-goal-summary-workload]': review,
    '[name="workloadPeriodicity"]': weekly
  };

  const form = {
    querySelector(selector) {
      return fields[selector] ?? null;
    },
    querySelectorAll(selector) {
      return selector === '[name="workloadPeriodicity"]'
        ? [daily, weekly, monthly]
        : [];
    }
  };

  const documentListeners = new Map();
  const documentObject = {
    createElement(tagName) {
      return new FakeElement(tagName);
    },
    addEventListener(type, listener) {
      const listeners = documentListeners.get(type) ?? [];
      listeners.push(listener);
      documentListeners.set(type, listeners);
    },
    restore() {
      for (const listener of documentListeners.get('guided-form:restored') ?? []) {
        listener({ detail: { form } });
      }
    }
  };

  return {
    form,
    documentObject,
    daily,
    weekly,
    monthly,
    hours,
    minutes,
    month,
    plannedDates,
    summary,
    review
  };
};

test('goal workload planner: shows the source and waits for planned dates', () => {
  const harness = createHarness();

  initializeGoalWorkloadPlanner(harness.form, harness.documentObject);

  assert.match(allText(harness.summary), /40 h por semana/);
  assert.match(allText(harness.summary), /Selecione os dias planejados/);
  assert.equal(harness.review.textContent, '40 h por semana');
});

test('goal workload planner: recalculates a partial week after a real date edit', () => {
  const harness = createHarness();
  initializeGoalWorkloadPlanner(harness.form, harness.documentObject);

  harness.plannedDates.value = '2026-07-01, 2026-07-02';
  harness.plannedDates.fire('input');

  const text = allText(harness.summary);
  assert.match(text, /Total planejado no mês/);
  assert.match(text, /16 h/);
  assert.match(text, /referência provisória de 5 dias/);
  assert.equal(harness.review.textContent, '16 h no mês');
  assert.equal(harness.plannedDates.dispatched.length, 0);
});

test('goal workload planner: switches mode and restores the original source', () => {
  const harness = createHarness({ dates: '2026-07-01,2026-07-02' });
  initializeGoalWorkloadPlanner(harness.form, harness.documentObject);

  harness.weekly.checked = false;
  harness.daily.checked = true;
  harness.hours.value = '8';
  harness.minutes.value = '30';
  harness.daily.fire('change');

  assert.match(allText(harness.summary), /8 h 30 min por dia/);
  assert.equal(harness.review.textContent, '17 h no mês');

  harness.daily.checked = false;
  harness.weekly.checked = true;
  harness.hours.value = '40';
  harness.minutes.value = '0';
  harness.documentObject.restore();

  assert.match(allText(harness.summary), /40 h por semana/);
  assert.equal(harness.review.textContent, '16 h no mês');
});
