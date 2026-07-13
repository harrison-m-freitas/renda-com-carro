CREATE TABLE operational_day (
  id UUID PRIMARY KEY,
  operation_date DATE NOT NULL,
  vehicle_id UUID NOT NULL REFERENCES vehicle(id),
  status VARCHAR(20) NOT NULL,
  planned_goal NUMERIC(14,2) NOT NULL,
  initial_odometer NUMERIC(12,1) NOT NULL,
  final_odometer NUMERIC(12,1),
  opened_at TIMESTAMP NOT NULL,
  closed_at TIMESTAMP,
  notes TEXT,
  CONSTRAINT ck_operational_day_odometer CHECK (initial_odometer >= 0 AND (final_odometer IS NULL OR final_odometer >= initial_odometer))
);
CREATE UNIQUE INDEX ux_operational_day_vehicle_date_active ON operational_day(vehicle_id, operation_date) WHERE status <> 'CANCELLED';
CREATE INDEX ix_operational_day_date ON operational_day(operation_date);
CREATE INDEX ix_operational_day_vehicle ON operational_day(vehicle_id);
CREATE INDEX ix_operational_day_status ON operational_day(status);

CREATE TABLE operation_shift (
  id UUID PRIMARY KEY,
  operational_day_id UUID NOT NULL REFERENCES operational_day(id),
  status VARCHAR(20) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  ended_at TIMESTAMP,
  initial_odometer NUMERIC(12,1) NOT NULL,
  final_odometer NUMERIC(12,1),
  start_region VARCHAR(120) NOT NULL,
  end_region VARCHAR(120),
  notes TEXT,
  CONSTRAINT ck_shift_time CHECK (ended_at IS NULL OR ended_at >= started_at),
  CONSTRAINT ck_shift_odometer CHECK (initial_odometer >= 0 AND (final_odometer IS NULL OR final_odometer >= initial_odometer))
);
CREATE UNIQUE INDEX ux_shift_one_open_per_day ON operation_shift(operational_day_id) WHERE status = 'OPEN';
CREATE INDEX ix_shift_day ON operation_shift(operational_day_id);
CREATE INDEX ix_shift_status ON operation_shift(status);

CREATE TABLE shift_platform (
  shift_id UUID NOT NULL REFERENCES operation_shift(id) ON DELETE CASCADE,
  platform_id UUID NOT NULL REFERENCES platform(id),
  PRIMARY KEY (shift_id, platform_id)
);
CREATE TABLE shift_neighborhood (
  shift_id UUID NOT NULL REFERENCES operation_shift(id) ON DELETE CASCADE,
  neighborhood VARCHAR(120) NOT NULL,
  PRIMARY KEY (shift_id, neighborhood)
);
CREATE TABLE trip (
  id UUID PRIMARY KEY,
  shift_id UUID NOT NULL REFERENCES operation_shift(id),
  platform_id UUID REFERENCES platform(id),
  started_at TIMESTAMP,
  ended_at TIMESTAMP,
  gross_amount NUMERIC(14,2),
  net_amount NUMERIC(14,2),
  distance NUMERIC(12,1),
  source VARCHAR(20) NOT NULL,
  external_reference VARCHAR(120)
);
CREATE INDEX ix_trip_shift ON trip(shift_id);
CREATE TABLE revenue (
  id UUID PRIMARY KEY,
  shift_id UUID NOT NULL REFERENCES operation_shift(id),
  trip_id UUID REFERENCES trip(id),
  platform_id UUID REFERENCES platform(id),
  type VARCHAR(30) NOT NULL,
  competence_date DATE NOT NULL,
  received_date DATE,
  gross_amount NUMERIC(14,2),
  platform_fee NUMERIC(14,2),
  net_amount NUMERIC(14,2) NOT NULL,
  tip_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
  bonus_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
  source VARCHAR(20) NOT NULL,
  notes TEXT
);
CREATE INDEX ix_revenue_shift ON revenue(shift_id);
CREATE INDEX ix_revenue_competence_date ON revenue(competence_date);
