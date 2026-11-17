CREATE TABLE saved_searches (
                                id               BIGSERIAL PRIMARY KEY,
                                name             VARCHAR(100) NOT NULL,
                                query            VARCHAR(200),
                                country          VARCHAR(100),
                                location         VARCHAR(100),
                                email            VARCHAR(320) NOT NULL,
                                active           BOOLEAN      NOT NULL DEFAULT TRUE,
                                last_notified_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_saved_searches_active ON saved_searches (active);