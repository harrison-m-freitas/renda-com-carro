CREATE TABLE vehicle (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  make VARCHAR(100) NOT NULL,
  model VARCHAR(100) NOT NULL,
  vehicle_year INTEGER NOT NULL,
  plate VARCHAR(10) NOT NULL UNIQUE,
  fuel_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  primary_vehicle BOOLEAN NOT NULL DEFAULT FALSE,
  initial_odometer NUMERIC(12,1) NOT NULL,
  current_odometer NUMERIC(12,1) NOT NULL,
  purchase_price NUMERIC(14,2) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ux_vehicle_single_primary
ON vehicle ((primary_vehicle))
WHERE primary_vehicle = TRUE AND status = 'ACTIVE';

CREATE TABLE platform (
  id UUID PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO platform (id, code, name, active) VALUES
  ('00000000-0000-0000-0000-000000000001', 'UBER', 'Uber', TRUE),
  ('00000000-0000-0000-0000-000000000002', '99', '99', TRUE);
