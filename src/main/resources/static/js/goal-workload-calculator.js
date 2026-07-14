const VALID_PERIODICITIES = new Set(['DAILY', 'WEEKLY', 'MONTHLY']);
const FALLBACK_WEEK_DAYS = 5;

const parseIsoDate = (value) => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value ?? ''));
  if (!match) throw new Error('Use datas no formato AAAA-MM-DD.');
  const date = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])));
  if (formatIsoDate(date) !== value) throw new Error('Use datas no formato AAAA-MM-DD.');
  return date;
};

const formatIsoDate = (date) => [
  date.getUTCFullYear(),
  String(date.getUTCMonth() + 1).padStart(2, '0'),
  String(date.getUTCDate()).padStart(2, '0')
].join('-');

const addDays = (value, days) => {
  const date = typeof value === 'string' ? parseIsoDate(value) : new Date(value.getTime());
  date.setUTCDate(date.getUTCDate() + days);
  return formatIsoDate(date);
};

const weekdayNumber = (value) => {
  const day = parseIsoDate(value).getUTCDay();
  return day === 0 ? 7 : day;
};

const startOfWeek = (value) => addDays(value, -(weekdayNumber(value) - 1));

const monthBounds = (month) => {
  const match = /^(\d{4})-(\d{2})$/.exec(String(month ?? ''));
  if (!match) throw new Error('O mês da meta deve usar o formato AAAA-MM.');
  const year = Number(match[1]);
  const monthNumber = Number(match[2]);
  if (monthNumber < 1 || monthNumber > 12) {
    throw new Error('O mês da meta deve usar o formato AAAA-MM.');
  }
  const start = `${match[1]}-${match[2]}-01`;
  const endDate = new Date(Date.UTC(year, monthNumber, 0));
  return { start, end: formatIsoDate(endDate) };
};

const normalizeDates = (month, rawDates) => {
  const { start, end } = monthBounds(month);
  const normalized = [...new Set((rawDates ?? []).map((value) => String(value).trim()))]
    .filter(Boolean)
    .sort();

  for (const value of normalized) {
    parseIsoDate(value);
    if (value < start || value > end) {
      throw new Error('Todos os dias planejados devem pertencer ao mês da meta.');
    }
    if (weekdayNumber(value) === 7) {
      throw new Error('Domingos não podem ser adicionados aos dias planejados.');
    }
  }
  return normalized;
};

const groupDatesByWeek = (dates) => {
  const groups = new Map();
  for (const date of dates) {
    const weekStart = startOfWeek(date);
    const group = groups.get(weekStart) ?? [];
    group.push(date);
    groups.set(weekStart, group);
  }
  return new Map([...groups.entries()].sort(([left], [right]) => left.localeCompare(right)));
};

const isBoundaryWeek = (month, weekStart) => {
  const bounds = monthBounds(month);
  const weekEnd = addDays(weekStart, 6);
  return weekStart < bounds.start || weekEnd > bounds.end;
};

const patternFor = (dates) => [...new Set(dates.map(weekdayNumber))].sort((a, b) => a - b);
const patternKey = (pattern) => pattern.join(',');

const weekDistance = (left, right) => {
  const leftDate = parseIsoDate(left);
  const rightDate = parseIsoDate(right);
  return Math.abs((rightDate.getTime() - leftDate.getTime()) / (7 * 24 * 60 * 60 * 1000));
};

const inferPattern = (boundaryWeekStart, observations) => {
  if (observations.length === 0) return [];

  const grouped = new Map();
  for (const observation of observations) {
    const key = patternKey(observation.pattern);
    const values = grouped.get(key) ?? [];
    values.push(observation);
    grouped.set(key, values);
  }

  const highestFrequency = Math.max(...[...grouped.values()].map((values) => values.length));
  const candidates = [...grouped.values()]
    .filter((values) => values.length === highestFrequency)
    .map((values) => {
      const closest = [...values].sort((left, right) => {
        const distance = weekDistance(boundaryWeekStart, left.weekStart)
          - weekDistance(boundaryWeekStart, right.weekStart);
        return distance || left.weekStart.localeCompare(right.weekStart);
      })[0];
      return {
        pattern: closest.pattern,
        representativeWeekStart: closest.weekStart,
        distance: weekDistance(boundaryWeekStart, closest.weekStart)
      };
    })
    .sort((left, right) => left.distance - right.distance
      || left.representativeWeekStart.localeCompare(right.representativeWeekStart));

  return [...candidates[0].pattern];
};

const divideHalfUp = (value, numerator, denominator) =>
  Math.floor((value * numerator) / denominator + 0.5);

const distribute = (totalMinutes, dates) => {
  if (dates.length === 0) return new Map();
  const sorted = [...dates].sort();
  const base = Math.floor(totalMinutes / sorted.length);
  const remainder = totalMinutes % sorted.length;
  return new Map(sorted.map((date, index) => [date, base + (index < remainder ? 1 : 0)]));
};

const buildResult = (allocatedByDate, weekMetadata = new Map()) => {
  const days = [...allocatedByDate.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([date, allocatedMinutes]) => ({ date, allocatedMinutes }));
  const grouped = groupDatesByWeek(days.map((day) => day.date));
  const allocationByDate = new Map(days.map((day) => [day.date, day.allocatedMinutes]));
  const weeks = [...grouped.entries()].map(([weekStart, dates]) => {
    const metadata = weekMetadata.get(weekStart) ?? {};
    return {
      weekStart,
      weekEnd: addDays(weekStart, 6),
      selectedDays: dates.length,
      allocatedMinutes: dates.reduce((total, date) => total + allocationByDate.get(date), 0),
      expectedDays: metadata.expectedDays ?? null,
      inferredPattern: metadata.inferredPattern ?? []
    };
  });
  const totalMinutes = days.reduce((total, day) => total + day.allocatedMinutes, 0);
  const averageDailyMinutes = divideHalfUp(totalMinutes, 1, days.length);
  const averageWeeklyMinutes = divideHalfUp(totalMinutes, 1, weeks.length);
  return {
    status: 'ready',
    totalMinutes,
    averageDailyMinutes,
    averageWeeklyMinutes,
    weeks,
    days
  };
};

export const calculateGoalWorkload = ({
  month,
  periodicity,
  enteredMinutes,
  plannedDates
}) => {
  monthBounds(month);
  const normalizedPeriodicity = String(periodicity ?? '').toUpperCase();
  if (!VALID_PERIODICITIES.has(normalizedPeriodicity)) {
    throw new Error('Selecione uma periodicidade válida para a jornada.');
  }
  if (!Number.isSafeInteger(enteredMinutes) || enteredMinutes <= 0) {
    throw new Error('A duração informada deve ser maior que zero.');
  }

  const dates = normalizeDates(month, plannedDates);
  if (dates.length === 0) {
    return {
      status: 'pending',
      totalMinutes: null,
      averageDailyMinutes: null,
      averageWeeklyMinutes: null,
      weeks: [],
      days: []
    };
  }

  if (normalizedPeriodicity === 'DAILY') {
    return buildResult(new Map(dates.map((date) => [date, enteredMinutes])));
  }

  if (normalizedPeriodicity === 'MONTHLY') {
    return buildResult(distribute(enteredMinutes, dates));
  }

  const grouped = groupDatesByWeek(dates);
  const observations = [...grouped.entries()]
    .filter(([weekStart]) => !isBoundaryWeek(month, weekStart))
    .map(([weekStart, weekDates]) => ({
      weekStart,
      pattern: patternFor(weekDates)
    }));
  const allocatedByDate = new Map();
  const metadata = new Map();

  for (const [weekStart, weekDates] of grouped) {
    let weekMinutes = enteredMinutes;
    if (isBoundaryWeek(month, weekStart)) {
      const inferredPattern = inferPattern(weekStart, observations);
      const expectedDays = inferredPattern.length || FALLBACK_WEEK_DAYS;
      const selectedDays = Math.min(weekDates.length, expectedDays);
      weekMinutes = divideHalfUp(enteredMinutes, selectedDays, expectedDays);
      metadata.set(weekStart, { expectedDays, inferredPattern });
    }
    for (const [date, minutes] of distribute(weekMinutes, weekDates)) {
      allocatedByDate.set(date, minutes);
    }
  }

  return buildResult(allocatedByDate, metadata);
};

export const formatWorkloadDuration = (rawMinutes) => {
  const minutes = Number(rawMinutes);
  if (!Number.isFinite(minutes) || minutes <= 0) return '0 min';
  const rounded = Math.round(minutes);
  const hours = Math.floor(rounded / 60);
  const remainder = rounded % 60;
  if (hours === 0) return `${remainder} min`;
  if (remainder === 0) return `${hours} h`;
  return `${hours} h ${remainder} min`;
};
