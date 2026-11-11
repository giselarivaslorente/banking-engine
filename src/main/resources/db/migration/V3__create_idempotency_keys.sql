
CREATE TABLE idempotency_keys (
        idempotency_key VARCHAR(255) PRIMARY KEY,
        source_account_id UUID NOT NULL,
        destination_account_id UUID NOT NULL,
        amount NUMERIC(19, 2) NOT NULL,
        created_at TIMESTAMP(6) WITH TIME ZONE
            NOT NULL DEFAULT CURRENT_TIMESTAMP
);
