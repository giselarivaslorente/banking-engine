CREATE TABLE bank_transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES bank_accounts(id),
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_bank_transactions_account
    ON bank_transactions(account_id, created_at DESC);