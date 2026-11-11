CREATE TABLE bank_accounts (
                               id UUID PRIMARY KEY,
                               owner_name VARCHAR(100) NOT NULL,
                               balance NUMERIC(19, 2) NOT NULL,
                               currency VARCHAR(3) NOT NULL
);