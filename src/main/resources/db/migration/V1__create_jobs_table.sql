CREATE TABLE jobs (
    id         UUID         PRIMARY KEY,
    type       VARCHAR(50)  NOT NULL,
    payload    TEXT         NOT NULL,
    result     TEXT,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retries    INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_type   ON jobs (type);
