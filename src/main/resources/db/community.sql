CREATE TABLE posts
(
    id            CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id       CHAR(36)     NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    view_count    INT          NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME              NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE comments
(
    id         CHAR(36) NOT NULL DEFAULT (UUID()),
    post_id    CHAR(36) NOT NULL,
    user_id    CHAR(36) NOT NULL,
    content    TEXT     NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME          NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE post_likes
(
    id         CHAR(36) NOT NULL DEFAULT (UUID()),
    post_id    CHAR(36) NOT NULL,
    user_id    CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_post_likes (post_id, user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
);
