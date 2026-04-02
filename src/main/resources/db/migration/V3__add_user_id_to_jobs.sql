ALTER TABLE jobs ADD COLUMN user_id UUID;

ALTER TABLE jobs
    ADD CONSTRAINT fk_jobs_user
    FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_jobs_user_id ON jobs (user_id);
