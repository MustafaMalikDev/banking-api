CREATE TABLE idempotency (
     request_id VARCHAR(255) NOT NULL,
     account_number BIGINT,
     sort_code VARCHAR(6),
     merchant_name VARCHAR(255),
     merchant_location VARCHAR(50),
     merchant_id BIGINT,
     merchant_tax_id VARCHAR(255),
     executed_at TIMESTAMP,
     location VARCHAR(50),
     error_code INT,
     amount DECIMAL,
     PRIMARY KEY (request_id)
);