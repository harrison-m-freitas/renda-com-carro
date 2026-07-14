import test from 'node:test';
import assert from 'node:assert/strict';

import {
  calculateGoalWorkload,
  formatWorkloadDuration
} from '../../main/resources/static/js/goal-workload-calculator.js';

test('goal workload calculator: daily multiplies every selected date', () => {
  const result = calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'DAILY',
    enteredMinutes: 510,
    plannedDates: ['2026-07-01', '2026-07-02', '2026-07-03']
  });

  assert.equal(result.status, 'ready');
  assert.equal(result.totalMinutes, 1530);
  assert.equal(result.averageDailyMinutes, 510);
  assert.equal(result.averageWeeklyMinutes, 1530);
  assert.deepEqual(result.days.map((day) => day.allocatedMinutes), [510, 510, 510]);
});

test('goal workload calculator: duplicate planned dates count once', () => {
  const result = calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'DAILY',
    enteredMinutes: 60,
    plannedDates: ['2026-07-01', '2026-07-01', '2026-07-02']
  });

  assert.equal(result.totalMinutes, 120);
  assert.deepEqual(result.days.map((day) => day.date), ['2026-07-01', '2026-07-02']);
});

test('goal workload calculator: monthly remainder goes to earliest dates', () => {
  const result = calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'MONTHLY',
    enteredMinutes: 10,
    plannedDates: ['2026-07-03', '2026-07-01', '2026-07-02']
  });

  assert.equal(result.totalMinutes, 10);
  assert.equal(result.averageDailyMinutes, 3);
  assert.equal(result.averageWeeklyMinutes, 10);
  assert.deepEqual(result.days.map((day) => day.date), [
    '2026-07-01',
    '2026-07-02',
    '2026-07-03'
  ]);
  assert.deepEqual(result.days.map((day) => day.allocatedMinutes), [4, 3, 3]);
});

test('goal workload calculator: weekly gives full loads to internal weeks', () => {
  const result = calculateGoalWorkload({
    month: '2026-03',
    periodicity: 'WEEKLY',
    enteredMinutes: 2400,
    plannedDates: [
      '2026-03-02', '2026-03-03', '2026-03-04',
      '2026-03-09', '2026-03-10', '2026-03-11', '2026-03-12',
      '2026-03-16', '2026-03-17', '2026-03-18', '2026-03-19', '2026-03-20',
      '2026-03-23', '2026-03-24', '2026-03-25', '2026-03-26', '2026-03-27', '2026-03-28'
    ]
  });

  assert.equal(result.totalMinutes, 9600);
  assert.equal(result.averageDailyMinutes, 533);
  assert.equal(result.averageWeeklyMinutes, 2400);
  assert.deepEqual(result.weeks.map((week) => week.selectedDays), [3, 4, 5, 6]);
  assert.deepEqual(result.weeks.map((week) => week.allocatedMinutes), [2400, 2400, 2400, 2400]);
});

test('goal workload calculator: boundary uses five-day fallback without evidence', () => {
  const result = calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'WEEKLY',
    enteredMinutes: 2400,
    plannedDates: ['2026-07-01', '2026-07-02']
  });

  assert.equal(result.totalMinutes, 960);
  assert.equal(result.averageDailyMinutes, 480);
  assert.equal(result.averageWeeklyMinutes, 960);
  assert.equal(result.weeks[0].selectedDays, 2);
  assert.equal(result.weeks[0].expectedDays, 5);
  assert.deepEqual(result.weeks[0].inferredPattern, []);
});

test('goal workload calculator: five-day fallback prorates three and four days and caps six', () => {
  const calculate = (month, plannedDates) => calculateGoalWorkload({
    month,
    periodicity: 'WEEKLY',
    enteredMinutes: 2400,
    plannedDates
  });

  assert.equal(calculate('2026-07', [
    '2026-07-01', '2026-07-02', '2026-07-03'
  ]).totalMinutes, 1440);
  assert.equal(calculate('2026-07', [
    '2026-07-01', '2026-07-02', '2026-07-03', '2026-07-04'
  ]).totalMinutes, 1920);
  assert.equal(calculate('2026-10', [
    '2026-10-26', '2026-10-27', '2026-10-28',
    '2026-10-29', '2026-10-30', '2026-10-31'
  ]).totalMinutes, 2400);
});

test('goal workload calculator: boundary uses nearest tied internal pattern', () => {
  const result = calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'WEEKLY',
    enteredMinutes: 2400,
    plannedDates: [
      '2026-07-01', '2026-07-02',
      '2026-07-06', '2026-07-07', '2026-07-08', '2026-07-09', '2026-07-10',
      '2026-07-14', '2026-07-15', '2026-07-16', '2026-07-17', '2026-07-18',
      '2026-07-27', '2026-07-28'
    ]
  });

  assert.deepEqual(result.weeks[0].inferredPattern, [1, 2, 3, 4, 5]);
  assert.deepEqual(result.weeks.at(-1).inferredPattern, [2, 3, 4, 5, 6]);
});

test('goal workload calculator: no planned dates returns pending source', () => {
  const result = calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'WEEKLY',
    enteredMinutes: 2400,
    plannedDates: []
  });

  assert.deepEqual(result, {
    status: 'pending',
    totalMinutes: null,
    averageDailyMinutes: null,
    averageWeeklyMinutes: null,
    weeks: [],
    days: []
  });
});

test('goal workload calculator: validates source and planned dates', () => {
  assert.throws(() => calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'WEEKLY',
    enteredMinutes: 0,
    plannedDates: ['2026-07-01']
  }), /maior que zero/);

  assert.throws(() => calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'DAILY',
    enteredMinutes: 60,
    plannedDates: ['2026-07-05']
  }), /Domingos/);

  assert.throws(() => calculateGoalWorkload({
    month: '2026-07',
    periodicity: 'DAILY',
    enteredMinutes: 60,
    plannedDates: ['2026-08-01']
  }), /mês/);
});

test('goal workload calculator: formats hours and minutes for summaries', () => {
  assert.equal(formatWorkloadDuration(0), '0 min');
  assert.equal(formatWorkloadDuration(30), '30 min');
  assert.equal(formatWorkloadDuration(480), '8 h');
  assert.equal(formatWorkloadDuration(510), '8 h 30 min');
});
