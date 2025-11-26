-- liquibase formatted sql
-- preconditions onFail:HALT onError:HALT
-- changeset master:3

ALTER TABLE surveys
ADD COLUMN response_review_required BOOLEAN;