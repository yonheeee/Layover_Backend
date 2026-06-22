CREATE TABLE faq
(
    id         CHAR(36)     NOT NULL DEFAULT (UUID()),
    question   VARCHAR(500) NOT NULL,
    answer     TEXT         NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE inquiries
(
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)     NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    answer      TEXT                  NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at DATETIME              NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id) REFERENCES users (id)
);
