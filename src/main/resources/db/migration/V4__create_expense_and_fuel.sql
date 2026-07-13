CREATE TABLE expense_category (
  id UUID PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO expense_category(id,code,name) VALUES
('10000000-0000-0000-0000-000000000001','PARKING','Estacionamento'),
('10000000-0000-0000-0000-000000000002','TOLL','Pedágio'),
('10000000-0000-0000-0000-000000000003','FOOD','Alimentação adicional'),
('10000000-0000-0000-0000-000000000004','CLEANING','Lavagem'),
('10000000-0000-0000-0000-000000000005','INSURANCE','Seguro'),
('10000000-0000-0000-0000-000000000006','DOCUMENTS','Documentação'),
('10000000-0000-0000-0000-000000000007','MAINTENANCE','Manutenção'),
('10000000-0000-0000-0000-000000000008','TIRES','Pneus'),
('10000000-0000-0000-0000-000000000009','ACCESSORIES','Acessórios'),
('10000000-0000-0000-0000-000000000010','INTERNET','Internet'),
('10000000-0000-0000-0000-000000000011','OTHER','Outros');

CREATE TABLE expense (
  id UUID PRIMARY KEY,
  vehicle_id UUID NOT NULL REFERENCES vehicle(id),
  operational_day_id UUID REFERENCES operational_day(id),
  shift_id UUID REFERENCES operation_shift(id),
  category_id UUID NOT NULL REFERENCES expense_category(id),
  expense_date DATE NOT NULL,
  competence_date DATE NOT NULL,
  paid_date DATE,
  amount NUMERIC(14,2) NOT NULL,
  classification VARCHAR(20) NOT NULL,
  allocation_method VARCHAR(30),
  professional_percentage NUMERIC(5,4),
  professional_fixed_amount NUMERIC(14,2),
  adjustment_reason TEXT,
  notes TEXT,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT ck_expense_amount CHECK(amount > 0)
);
CREATE INDEX ix_expense_vehicle_date ON expense(vehicle_id, expense_date);
CREATE INDEX ix_expense_competence ON expense(competence_date);
CREATE INDEX ix_expense_paid_date ON expense(paid_date);

CREATE TABLE monthly_odometer_closing (
  id UUID PRIMARY KEY,
  vehicle_id UUID NOT NULL REFERENCES vehicle(id),
  reference_month DATE NOT NULL,
  initial_odometer NUMERIC(12,1) NOT NULL,
  final_odometer NUMERIC(12,1) NOT NULL,
  professional_kilometers NUMERIC(12,1) NOT NULL,
  personal_kilometers NUMERIC(12,1) NOT NULL,
  professional_percentage NUMERIC(5,4) NOT NULL,
  adjustment_reason TEXT,
  UNIQUE(vehicle_id, reference_month)
);

CREATE TABLE fueling (
  id UUID PRIMARY KEY,
  vehicle_id UUID NOT NULL REFERENCES vehicle(id),
  fueled_at TIMESTAMP NOT NULL,
  odometer NUMERIC(12,1) NOT NULL,
  station VARCHAR(120),
  fuel_type VARCHAR(20) NOT NULL,
  liters NUMERIC(12,3) NOT NULL,
  price_per_liter NUMERIC(12,3) NOT NULL,
  total_amount NUMERIC(14,2) NOT NULL,
  full_tank BOOLEAN NOT NULL,
  notes TEXT,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX ix_fueling_vehicle_time ON fueling(vehicle_id, fueled_at);
