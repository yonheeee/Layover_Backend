CREATE TABLE chat_rooms
(
    id           CHAR(36) NOT NULL DEFAULT (UUID()),
    user_low_id  CHAR(36) NOT NULL,
    user_high_id CHAR(36) NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_chat_rooms_pair (user_low_id, user_high_id),
    CONSTRAINT fk_chat_rooms_user_low FOREIGN KEY (user_low_id) REFERENCES users (id),
    CONSTRAINT fk_chat_rooms_user_high FOREIGN KEY (user_high_id) REFERENCES users (id)
);

CREATE TABLE chat_room_participants
(
    room_id      CHAR(36) NOT NULL,
    user_id      CHAR(36) NOT NULL,
    joined_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_read_at DATETIME NULL,
    PRIMARY KEY (room_id, user_id),
    KEY idx_chat_room_participants_user (user_id),
    CONSTRAINT fk_chat_room_participants_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id),
    CONSTRAINT fk_chat_room_participants_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE chat_messages
(
    id                CHAR(36)    NOT NULL DEFAULT (UUID()),
    room_id           CHAR(36)    NOT NULL,
    sender_id         CHAR(36)    NOT NULL,
    type              VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    encrypted_content TEXT        NOT NULL,
    iv                VARCHAR(64) NOT NULL,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_chat_messages_room_created (room_id, created_at),
    CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id),
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE TABLE user_reports
(
    id                CHAR(36)    NOT NULL DEFAULT (UUID()),
    reporter_id       CHAR(36)    NOT NULL,
    reported_user_id  CHAR(36)    NOT NULL,
    encrypted_content TEXT        NOT NULL,
    iv                VARCHAR(64) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_reports_reporter (reporter_id, created_at),
    CONSTRAINT fk_user_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_user_reports_reported FOREIGN KEY (reported_user_id) REFERENCES users (id)
);
