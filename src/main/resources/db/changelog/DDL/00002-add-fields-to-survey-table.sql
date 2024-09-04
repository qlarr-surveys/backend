-- liquibase formatted sql
-- preconditions onFail:HALT onError:HALT
-- changeset master:1

ALTER TABLE surveys
ADD COLUMN description TEXT,
ADD COLUMN image VARCHAR(255);
