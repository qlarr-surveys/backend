-- liquibase formatted sql
-- preconditions onFail:HALT onError:HALT
-- changeset master:1

CREATE TABLE user_registration
(
    id          UUID            NOT NULL PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL
);

CREATE TABLE users
(
    id         UUID           NOT NULL PRIMARY KEY,
    first_name VARCHAR(255)   NOT NULL,
    last_name  VARCHAR(255)   NOT NULL,
    email      VARCHAR(255)   NOT NULL constraint uc_users_email unique,
    password VARCHAR(255)   NOT NULL,
    deleted    BOOLEAN        NOT NULL,
    roles      VARCHAR(255)[] NOT NULL,
    is_confirmed BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP
);

CREATE TABLE surveys
(
    id                      UUID         NOT NULL PRIMARY KEY,
    can_lock_survey BOOLEAN              NOT NULL,
    creation_date           TIMESTAMP,
    last_modified           TIMESTAMP,
    start_date              TIMESTAMP,
    end_date                TIMESTAMP,
    name                    VARCHAR(255) NOT NULL
        constraint uc_surveys_name
            unique,
    quota                   INTEGER      NOT NULL,
    record_gps              BOOLEAN      NOT NULL,
    save_ip                 BOOLEAN      NOT NULL,
    save_timings            BOOLEAN      NOT NULL,
    public_within_org       BOOLEAN      NOT NULL,
    status                  VARCHAR(255),
    usage                   VARCHAR(255),
    background_audio         BOOLEAN              NOT NULL
);

CREATE TABLE versions
(
    version         Int         NOT NULL,
    sub_version     Int         NOT NULL,
    survey_id       UUID        NOT NULL,
    last_modified   TIMESTAMP   NOT NULL,
    schema          TEXT        NOT NULL,
    valid           BOOLEAN     NOT NULL,
    published       BOOLEAN     NOT NULL,
    PRIMARY KEY (version, survey_id),
    CONSTRAINT pc_version_survey FOREIGN KEY (survey_id) REFERENCES surveys (id)
);

CREATE TABLE responses
(
    id              UUID            NOT NULL PRIMARY KEY,
    version         Int             NOT NULL,
    survey_id       UUID            NOT NULL,
    preview         boolean         NOT NULL,
    surveyor        UUID,
    nav_index       VARCHAR(255)    NOT NULL,
    start_date      TIMESTAMP       NOT NULL,
    submit_date     TIMESTAMP,
    lang            VARCHAR(5)      NOT NULL,
    values          jsonb,
    CONSTRAINT pc_response_survey FOREIGN KEY (survey_id) REFERENCES surveys (id),
    CONSTRAINT pc_surveyor_user FOREIGN KEY (surveyor) REFERENCES users (id),
    CONSTRAINT pc_response_version FOREIGN KEY (survey_id, version) REFERENCES versions (survey_id, version)
);

CREATE TABLE refresh_tokens
(
    id          UUID        NOT NULL    PRIMARY KEY,
    user_id     UUID        NOT NULL,
    expiration  TIMESTAMP   NOT NULL,
    session_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    CONSTRAINT pc_refresh_users FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE email_changes
(
    user_id     UUID            NOT NULL PRIMARY KEY,
    new_email   VARCHAR(255)    NOT NULL,
    pin         VARCHAR(6)      NOT NULL,
    CONSTRAINT pc_email_changes_user FOREIGN KEY (user_id) REFERENCES users (id)
);