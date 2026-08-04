-- Connect community SHARE posts to saved courses for popular course sharing.
ALTER TABLE posts ADD COLUMN course_id CHAR(36) NULL AFTER user_id;
ALTER TABLE posts ADD INDEX idx_posts_course_id (course_id);
ALTER TABLE posts ADD CONSTRAINT fk_posts_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE SET NULL;
