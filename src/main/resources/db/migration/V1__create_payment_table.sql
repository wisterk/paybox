CREATE TABLE payment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        VARCHAR(255),
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'RUB',
    status          VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    method          VARCHAR(32) NOT NULL,
    provider_name   VARCHAR(64) NOT NULL,
    external_id     VARCHAR(255),
    payment_url     TEXT,
    metadata        JSONB,
    version         INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    paid_at         TIMESTAMP WITH TIME ZONE,
    expires_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_external_id ON payment(external_id);
CREATE INDEX idx_payment_order_id ON payment(order_id);
