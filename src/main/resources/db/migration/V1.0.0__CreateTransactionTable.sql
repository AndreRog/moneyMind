CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- V1__Create_Transaction_Table.sql

-- Create the transaction table
CREATE TABLE bank_transaction (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL,
    value NUMERIC NOT NULL,
    balance NUMERIC NOT NULL,
    description text,
    bank_name text,
    category text,
    date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transaction_uuid ON bank_transaction(uuid);