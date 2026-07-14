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
    month: '2026-07',
    periodicity: 'WEEKLY',
    enteredMinutes: 2400,
    plannedDates: [
      '2026-07-06', '2026-07-07', '2026-07-08', '2026-07-09',
      '2026-07-13', '2026-07-14', '2026-07-15', '2026-07-16', '2026-07-17',
      '2026-07-20', '2026-07-21', '2026-07-22', '2026-07-23', '2026-07-24', '2026-07-25'
    ]
  });

  assert.equal(result.totalMinutes, 7200);
  assert.equal(result.averageDailyMinutes, 480);
  assert.equal(result.averageWeeklyMinutes, 2400);
  assert.deepEqual(result.weeks.map((week) => week.selectedDays), [4, 5, 6]);
  assert.deepEqual(result.weeks.map((week) => week.allocatedMinutes), [2400, 2400, 2400]);
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
