CREATE TABLE users (
    id            UUID         NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE tasks (
    id          UUID          NOT NULL,
    owner_id    UUID          NOT NULL,
    title       VARCHAR(120)  NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(20)   NOT NULL,
    due_date    DATE,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version     BIGINT        NOT NULL,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_tasks_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0)
);

-- Listing by owner filtered by status, and by owner filtered/sorted by due date.
CREATE INDEX idx_tasks_owner_status   ON tasks (owner_id, status);
CREATE INDEX idx_tasks_owner_due_date ON tasks (owner_id, due_date);
