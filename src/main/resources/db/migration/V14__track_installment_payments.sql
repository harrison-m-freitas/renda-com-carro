ALTER TABLE obligation_installment
    ADD CONSTRAINT ck_installment_paid_amount_non_negative CHECK (paid_amount >= 0),
    ADD CONSTRAINT ck_installment_paid_not_above_expected CHECK (paid_amount <= expected_amount);
