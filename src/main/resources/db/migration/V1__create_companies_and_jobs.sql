CREATE TABLE companies (
                           id              BIGSERIAL    PRIMARY KEY,
                           name            VARCHAR(200) NOT NULL,
                           source          VARCHAR(20)  NOT NULL,
                           board_token     VARCHAR(100) NOT NULL,
                           active          BOOLEAN      NOT NULL DEFAULT TRUE,
                           last_synced_at  TIMESTAMPTZ,
                           created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

                           CONSTRAINT uq_companies_source_board UNIQUE (source, board_token)
);

CREATE TABLE jobs (
                      id              BIGSERIAL     PRIMARY KEY,
                      company_id      BIGINT        NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
                      external_id     VARCHAR(100)  NOT NULL,
                      title           VARCHAR(300)  NOT NULL,
                      location        VARCHAR(300),
                      department      VARCHAR(200),
                      url             VARCHAR(1000) NOT NULL,
                      description     TEXT,
                      active          BOOLEAN       NOT NULL DEFAULT TRUE,
                      first_seen_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
                      last_seen_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- Filled automatically by the database from the other columns, for full-text search.
                      search_vector   TSVECTOR GENERATED ALWAYS AS (
                          to_tsvector('simple',
                                      coalesce(title, '') || ' ' ||
                                      coalesce(location, '') || ' ' ||
                                      coalesce(department, '') || ' ' ||
                                      coalesce(description, ''))
                          ) STORED,

                      CONSTRAINT uq_jobs_company_external UNIQUE (company_id, external_id)
);

CREATE INDEX ix_jobs_search_vector ON jobs USING GIN (search_vector);
CREATE INDEX ix_jobs_active_first_seen ON jobs (active, first_seen_at DESC);