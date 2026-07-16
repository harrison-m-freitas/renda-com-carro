DELETE FROM form_draft WHERE form_type = 'OBLIGATION';

DROP TABLE IF EXISTS obligation_payment;
DROP TABLE IF EXISTS obligation_installment;
DROP TABLE IF EXISTS financial_obligation;

CREATE TABLE acquisition_plan (
  id UUID PRIMARY KEY,
  vehicle_id UUID REFERENCES vehicle(id),
  title VARCHAR(160) NOT NULL,
  purchase_amount NUMERIC(14,2) NOT NULL CHECK (purchase_amount > 0),
  own_resources_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (own_resources_amount >= 0),
  purchase_date DATE NOT NULL,
  notes TEXT,
  created_at TIMESTAMP NOT NULL,
  CHECK (own_resources_amount <= purchase_amount)
);

CREATE TABLE financial_obligation (
  id UUID PRIMARY KEY,
  vehicle_id UUID REFERENCES vehicle(id),
  acquisition_plan_id UUID REFERENCES acquisition_plan(id) ON DELETE SET NULL,
  obligation_type VARCHAR(40) NOT NULL,
  mode VARCHAR(40) NOT NULL,
  calculation_method VARCHAR(40) NOT NULL,
  creditor VARCHAR(160) NOT NULL,
  principal_amount NUMERIC(14,2) NOT NULL CHECK (principal_amount > 0),
  monthly_interest_rate NUMERIC(18,12),
  annual_effective_interest_rate NUMERIC(18,12),
  start_date DATE NOT NULL,
  first_due_date DATE,
  term_months INTEGER,
  installment_amount NUMERIC(14,2),
  planned_total_amount NUMERIC(14,2),
  planned_interest_amount NUMERIC(14,2),
  current_balance NUMERIC(14,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  monthly_target NUMERIC(14,2),
  notes TEXT,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE obligation_installment (
  id UUID PRIMARY KEY,
  obligation_id UUID NOT NULL REFERENCES financial_obligation(id) ON DELETE CASCADE,
  sequence_number INTEGER NOT NULL,
  due_date DATE NOT NULL,
  principal_amount NUMERIC(14,2) NOT NULL,
  interest_amount NUMERIC(14,2) NOT NULL,
  expected_amount NUMERIC(14,2) NOT NULL,
  paid_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  UNIQUE(obligation_id, sequence_number)
);

CREATE TABLE obligation_payment (
  id UUID PRIMARY KEY,
  obligation_id UUID NOT NULL REFERENCES financial_obligation(id) ON DELETE CASCADE,
  installment_id UUID REFERENCES obligation_installment(id) ON DELETE SET NULL,
  payment_date DATE NOT NULL,
  amount NUMERIC(14,2) NOT NULL,
  principal_paid NUMERIC(14,2) NOT NULL,
  interest_paid NUMERIC(14,2) NOT NULL,
  extra_amortization NUMERIC(14,2) NOT NULL DEFAULT 0,
  external_reference VARCHAR(120),
  notes TEXT,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX ix_acquisition_plan_vehicle ON acquisition_plan(vehicle_id);
CREATE INDEX ix_obligation_status ON financial_obligation(status);
CREATE INDEX ix_obligation_acquisition_plan ON financial_obligation(acquisition_plan_id);
CREATE INDEX ix_installment_due_status ON obligation_installment(due_date, status);
CREATE UNIQUE INDEX ux_obligation_payment_external_reference
  ON obligation_payment(obligation_id, external_reference)
  WHERE external_reference IS NOT NULL;
