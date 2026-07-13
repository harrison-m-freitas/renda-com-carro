CREATE TABLE monthly_goal (
  id UUID PRIMARY KEY,
  reference_month DATE NOT NULL UNIQUE,
  personal_net_goal NUMERIC(14,2) NOT NULL,
  operational_goal NUMERIC(14,2) NOT NULL,
  planned_hours NUMERIC(8,2) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE TABLE planned_work_day (
  id UUID PRIMARY KEY,
  monthly_goal_id UUID NOT NULL REFERENCES monthly_goal(id) ON DELETE CASCADE,
  work_date DATE NOT NULL,
  planned_hours NUMERIC(6,2) NOT NULL,
  available BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE(monthly_goal_id, work_date)
);

CREATE TABLE financial_obligation (
  id UUID PRIMARY KEY,
  vehicle_id UUID REFERENCES vehicle(id),
  obligation_type VARCHAR(30) NOT NULL,
  mode VARCHAR(20) NOT NULL,
  creditor VARCHAR(160) NOT NULL,
  principal_amount NUMERIC(14,2) NOT NULL,
  annual_interest_rate NUMERIC(8,6) NOT NULL DEFAULT 0,
  start_date DATE NOT NULL,
  first_due_date DATE,
  term_months INTEGER,
  planned_installment NUMERIC(14,2),
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
  obligation_id UUID NOT NULL REFERENCES financial_obligation(id),
  installment_id UUID REFERENCES obligation_installment(id),
  payment_date DATE NOT NULL,
  amount NUMERIC(14,2) NOT NULL,
  principal_paid NUMERIC(14,2) NOT NULL,
  interest_paid NUMERIC(14,2) NOT NULL,
  extra_amortization NUMERIC(14,2) NOT NULL DEFAULT 0,
  external_reference VARCHAR(120),
  notes TEXT,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX ix_obligation_status ON financial_obligation(status);
CREATE INDEX ix_installment_due_status ON obligation_installment(due_date,status);
CREATE UNIQUE INDEX ux_obligation_payment_external_reference ON obligation_payment(obligation_id,external_reference) WHERE external_reference IS NOT NULL;
