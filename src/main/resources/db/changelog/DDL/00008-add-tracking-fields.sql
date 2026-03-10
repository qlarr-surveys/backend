-- liquibase formatted sql
-- changeset qlarr:00008

ALTER TABLE surveys
    ADD COLUMN record_gps        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN save_ip           BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN save_timings      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN background_audio  BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE responses
    ADD COLUMN events JSONB NOT NULL DEFAULT '[]'::JSONB,
    ADD COLUMN ip_addr VARCHAR(255);
