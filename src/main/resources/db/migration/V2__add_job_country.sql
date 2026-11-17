ALTER TABLE jobs
    ADD COLUMN country VARCHAR(100);

-- Indexes the lowercase value, because searches compare countries case-insensitively.
CREATE INDEX ix_jobs_country ON jobs (LOWER(country));