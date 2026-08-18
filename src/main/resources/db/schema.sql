-- =============================================================
--  Daejeon Layover - 전체 스키마 (통합본)
-- -------------------------------------------------------------
--  이 파일 하나로 daejeon_layover 데이터베이스를 처음부터 만들 수 있습니다.
--  기존의 schema_2026_0816.sql, community.sql, notice_faq_inquiry.sql,
--  chat_report.sql, attachments_migration.sql, post_course_migration.sql,
--  place_detail_enrichment.sql 을 모두 반영했습니다.
--
--  테이블 정의만 포함합니다. 시드/더미/덤프 데이터는 없습니다.
--
--  실행:
--      mysql -u root -p < schema.sql
--
--  ⚠️ 아래 DROP 구문이 기존 테이블을 모두 삭제합니다.
--     운영 DB에서는 절대 그대로 실행하지 마세요.
--
--  [현재 서비스 범위에서 제외한 것]
--   - reviews : 코스 별점/후기 기능. 우선순위 보류 상태이며
--               착수 시 db/_deferred/reviews.sql 을 실행하세요.
--   - users.refresh_token : 리프레시 토큰은 무상태 JWT로만 처리하고
--                           DB에 저장하지 않습니다. 이메일 인증 코드는
--                           Redis(또는 메모리)를 사용합니다.
-- =============================================================

CREATE DATABASE IF NOT EXISTS `daejeon_layover`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `daejeon_layover`;

-- 외래키 의존성 때문에 자식 → 부모 순서로 삭제합니다.
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `user_reports`;
DROP TABLE IF EXISTS `chat_messages`;
DROP TABLE IF EXISTS `chat_room_participants`;
DROP TABLE IF EXISTS `chat_rooms`;
DROP TABLE IF EXISTS `bookmarks`;
DROP TABLE IF EXISTS `comments`;
DROP TABLE IF EXISTS `post_likes`;
DROP TABLE IF EXISTS `stamps`;
DROP TABLE IF EXISTS `user_characters`;
DROP TABLE IF EXISTS `course_places`;
DROP TABLE IF EXISTS `posts`;
DROP TABLE IF EXISTS `courses`;
DROP TABLE IF EXISTS `inquiries`;
DROP TABLE IF EXISTS `notices`;
DROP TABLE IF EXISTS `faq`;
DROP TABLE IF EXISTS `characters`;
DROP TABLE IF EXISTS `places`;
DROP TABLE IF EXISTS `users`;
-- 보류 기능 테이블 (db/_deferred/reviews.sql 참고)
DROP TABLE IF EXISTS `reviews`;
SET FOREIGN_KEY_CHECKS = 1;


-- =============================================================
--  1. 사용자 / 인증
-- =============================================================

CREATE TABLE `users` (
  `id`            char(36)     NOT NULL DEFAULT (uuid()),
  `username`      varchar(50)  NOT NULL COMMENT '닉네임',
  `real_name`     varchar(50)           DEFAULT NULL,
  `birth_date`    date                  DEFAULT NULL,
  `phone`         varchar(20)           DEFAULT NULL,
  `email`         varchar(100) NOT NULL,
  `password_hash` varchar(255)          DEFAULT NULL COMMENT '카카오 전용 계정은 NULL',
  `kakao_id`      varchar(100)          DEFAULT NULL,
  `profile_image` varchar(500)          DEFAULT NULL,
  `stamp_count`   int          NOT NULL DEFAULT 0,
  `role`          varchar(20)  NOT NULL DEFAULT 'USER',
  `deleted_at`    datetime              DEFAULT NULL COMMENT '탈퇴 시각 (soft delete)',
  `created_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email` (`email`),
  UNIQUE KEY `uq_users_kakao` (`kakao_id`),
  KEY `idx_users_find_id` (`real_name`, `birth_date`, `phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
--  2. 관광지
-- =============================================================

CREATE TABLE `places` (
  `id`                     char(36)      NOT NULL DEFAULT (uuid()),
  `name`                   varchar(200)  NOT NULL,
  `category`               varchar(50)   NOT NULL COMMENT 'FOOD / CAFE / NATURE / CULTURE / TOUR / STATION',
  `original_category_code` varchar(20)            DEFAULT NULL,
  `address`                varchar(300)           DEFAULT NULL,
  `latitude`               decimal(10,7)          DEFAULT NULL,
  `longitude`              decimal(10,7)          DEFAULT NULL,
  `tour_api_id`            varchar(50)            DEFAULT NULL COMMENT 'TourAPI contentid',
  `content_type_id`        varchar(10)            DEFAULT NULL COMMENT 'TourAPI contenttypeid',

  -- TourAPI detailIntro2 기반 상세정보
  `operating_hours`        varchar(500)           DEFAULT NULL,
  `rest_date`              text                   DEFAULT NULL COMMENT '휴무일',
  `info_center`            text                   DEFAULT NULL COMMENT '문의 및 안내',
  `parking`                text                   DEFAULT NULL,
  `use_fee`                text                   DEFAULT NULL COMMENT '이용요금 / 대표메뉴',
  `reservation`            text                   DEFAULT NULL COMMENT '예약 / 체험 안내',

  `description`            text                   DEFAULT NULL COMMENT 'detailCommon2 overview',

  -- Kakao Local API 보강 정보
  `kakao_place_id`         varchar(100)           DEFAULT NULL,
  `kakao_place_url`        varchar(500)           DEFAULT NULL COMMENT '카카오맵 상세 페이지',
  `kakao_phone`            varchar(100)           DEFAULT NULL,
  `road_address`           varchar(500)           DEFAULT NULL,

  -- 원문 보존
  `intro_raw`              longtext               DEFAULT NULL COMMENT 'detailIntro2 원문 JSON',
  `detail_info_raw`        longtext               DEFAULT NULL COMMENT 'detailInfo2 원문 JSON',

  `image_url`              varchar(500)           DEFAULT NULL,
  `is_active`              tinyint(1)    NOT NULL DEFAULT 1,
  `deleted_at`             datetime               DEFAULT NULL,
  `synced_at`              datetime               DEFAULT NULL COMMENT '마지막 TourAPI 동기화 시각',
  `created_at`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_places_tour_api_id` (`tour_api_id`),
  KEY `idx_places_category` (`category`),
  KEY `idx_places_is_active` (`is_active`),
  KEY `idx_places_location` (`latitude`, `longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
--  3. 캐릭터 / 스탬프
-- =============================================================

CREATE TABLE `characters` (
  `id`              char(36)     NOT NULL DEFAULT (uuid()),
  `name`            varchar(100) NOT NULL,
  `image_url`       varchar(500)          DEFAULT NULL,
  `required_stamps` int          NOT NULL COMMENT '획득에 필요한 스탬프 수',
  `description`     varchar(300)          DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_characters_required` (`required_stamps`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_characters` (
  `id`           char(36) NOT NULL DEFAULT (uuid()),
  `user_id`      char(36) NOT NULL,
  `character_id` char(36) NOT NULL,
  `obtained_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_character` (`user_id`, `character_id`),
  KEY `fk_uc_character` (`character_id`),
  CONSTRAINT `fk_uc_user`      FOREIGN KEY (`user_id`)      REFERENCES `users` (`id`)      ON DELETE CASCADE,
  CONSTRAINT `fk_uc_character` FOREIGN KEY (`character_id`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `stamps` (
  `id`         char(36)     NOT NULL DEFAULT (uuid()),
  `user_id`    char(36)     NOT NULL,
  `place_id`   char(36)     NOT NULL,
  `photo_url`  varchar(500)          DEFAULT NULL,
  `visited_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stamps_user_id` (`user_id`),
  KEY `idx_stamps_place_id` (`place_id`),
  CONSTRAINT `fk_st_user`  FOREIGN KEY (`user_id`)  REFERENCES `users` (`id`)  ON DELETE CASCADE,
  CONSTRAINT `fk_st_place` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
--  4. 코스
-- =============================================================

CREATE TABLE `courses` (
  `id`                char(36)    NOT NULL DEFAULT (uuid()),
  `user_id`           char(36)    NOT NULL,
  `departure_station` varchar(20) NOT NULL COMMENT 'DAEJEON / SEODAEJEON / SINTANJIN',
  `duration_minutes`  int         NOT NULL COMMENT '환승 잔여 시간(분)',
  `travel_mode`       varchar(10) NOT NULL COMMENT 'WALK / TAXI',
  `weather_condition` varchar(20)          DEFAULT NULL,
  `theme_tags`        json                 DEFAULT NULL,
  `is_public`         tinyint(1)  NOT NULL DEFAULT 0,
  `created_at`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_courses_user_id` (`user_id`),
  KEY `idx_courses_is_public` (`is_public`),
  CONSTRAINT `fk_courses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `course_places` (
  `id`              char(36) NOT NULL DEFAULT (uuid()),
  `course_id`       char(36) NOT NULL,
  `place_id`        char(36) NOT NULL,
  `order_index`     int      NOT NULL,
  `travel_time_min` int               DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_course_place_order` (`course_id`, `order_index`),
  KEY `idx_cp_place_id` (`place_id`),
  CONSTRAINT `fk_cp_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cp_place`  FOREIGN KEY (`place_id`)  REFERENCES `places` (`id`)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
--  5. 즐겨찾기
-- =============================================================

CREATE TABLE `bookmarks` (
  `id`         char(36) NOT NULL DEFAULT (uuid()),
  `user_id`    char(36) NOT NULL,
  `place_id`   char(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bookmark` (`user_id`, `place_id`),
  KEY `fk_bm_place` (`place_id`),
  CONSTRAINT `fk_bm_user`  FOREIGN KEY (`user_id`)  REFERENCES `users` (`id`)  ON DELETE CASCADE,
  CONSTRAINT `fk_bm_place` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
--  6. 커뮤니티
-- =============================================================

CREATE TABLE `posts` (
  `id`            char(36)     NOT NULL DEFAULT (uuid()),
  `user_id`       char(36)     NOT NULL,
  `course_id`     char(36)              DEFAULT NULL COMMENT 'SHARE 글에 연결된 코스',
  `category`      varchar(20)  NOT NULL COMMENT 'SHARE / QUESTION / TOGETHER / FREE',
  `title`         varchar(200) NOT NULL,
  `content`       text         NOT NULL,
  `thumbnail_url` varchar(500)          DEFAULT NULL,
  `view_count`    int          NOT NULL DEFAULT 0,
  `like_count`    int          NOT NULL DEFAULT 0,
  `comment_count` int          NOT NULL DEFAULT 0,
  `created_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`    datetime              DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_posts_user` (`user_id`),
  KEY `idx_posts_course_id` (`course_id`),
  CONSTRAINT `fk_posts_user`   FOREIGN KEY (`user_id`)   REFERENCES `users` (`id`),
  CONSTRAINT `fk_posts_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
  `id`         char(36) NOT NULL DEFAULT (uuid()),
  `post_id`    char(36) NOT NULL,
  `user_id`    char(36) NOT NULL,
  `content`    text     NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime          DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_comments_post` (`post_id`),
  KEY `fk_comments_user` (`user_id`),
  CONSTRAINT `fk_comments_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_likes` (
  `id`         char(36) NOT NULL DEFAULT (uuid()),
  `post_id`    char(36) NOT NULL,
  `user_id`    char(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_post_likes` (`post_id`, `user_id`),
  KEY `fk_post_likes_user` (`user_id`),
  CONSTRAINT `fk_post_likes_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `fk_post_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
--  7. 공지 / FAQ / 문의
-- =============================================================

CREATE TABLE `notices` (
  `id`         char(36)     NOT NULL DEFAULT (uuid()),
  `title`      varchar(200) NOT NULL,
  `content`    text         NOT NULL,
  `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `faq` (
  `id`         char(36)     NOT NULL DEFAULT (uuid()),
  `question`   varchar(500) NOT NULL,
  `answer`     text         NOT NULL,
  `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `inquiries` (
  `id`              char(36)     NOT NULL DEFAULT (uuid()),
  `user_id`         char(36)     NOT NULL,
  `title`           varchar(200) NOT NULL,
  `content`         text         NOT NULL,
  `status`          varchar(20)  NOT NULL DEFAULT 'PENDING',
  `answer`          text                  DEFAULT NULL,
  `attachment_urls` text                  DEFAULT NULL COMMENT '쉼표로 구분된 URL 목록',
  `created_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `answered_at`     datetime              DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_inquiries_user` (`user_id`),
  CONSTRAINT `fk_inquiries_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
--  8. 채팅 / 신고
--  본문은 AES-GCM으로 암호화해 저장합니다 (encrypted_content + iv).
-- =============================================================

CREATE TABLE `chat_rooms` (
  `id`           char(36) NOT NULL DEFAULT (uuid()),
  `user_low_id`  char(36) NOT NULL COMMENT '두 사용자 ID 중 사전순 앞',
  `user_high_id` char(36) NOT NULL COMMENT '두 사용자 ID 중 사전순 뒤',
  `created_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_chat_rooms_pair` (`user_low_id`, `user_high_id`),
  KEY `fk_chat_rooms_user_high` (`user_high_id`),
  CONSTRAINT `fk_chat_rooms_user_low`  FOREIGN KEY (`user_low_id`)  REFERENCES `users` (`id`),
  CONSTRAINT `fk_chat_rooms_user_high` FOREIGN KEY (`user_high_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_room_participants` (
  `room_id`      char(36) NOT NULL,
  `user_id`      char(36) NOT NULL,
  `joined_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_read_at` datetime          DEFAULT NULL,
  PRIMARY KEY (`room_id`, `user_id`),
  KEY `idx_chat_room_participants_user` (`user_id`),
  CONSTRAINT `fk_chat_room_participants_room` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`id`),
  CONSTRAINT `fk_chat_room_participants_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_messages` (
  `id`                char(36)    NOT NULL DEFAULT (uuid()),
  `room_id`           char(36)    NOT NULL,
  `sender_id`         char(36)    NOT NULL,
  `type`              varchar(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT / IMAGE / COURSE',
  `encrypted_content` text        NOT NULL,
  `iv`                varchar(64) NOT NULL,
  `created_at`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_messages_room_created` (`room_id`, `created_at`),
  KEY `fk_chat_messages_sender` (`sender_id`),
  CONSTRAINT `fk_chat_messages_room`   FOREIGN KEY (`room_id`)   REFERENCES `chat_rooms` (`id`),
  CONSTRAINT `fk_chat_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_reports` (
  `id`                char(36)    NOT NULL DEFAULT (uuid()),
  `reporter_id`       char(36)    NOT NULL,
  `reported_user_id`  char(36)    NOT NULL,
  `encrypted_content` text        NOT NULL,
  `iv`                varchar(64) NOT NULL,
  `status`            varchar(20) NOT NULL DEFAULT 'RECEIVED',
  `created_at`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_reports_reporter` (`reporter_id`, `created_at`),
  KEY `fk_user_reports_reported` (`reported_user_id`),
  CONSTRAINT `fk_user_reports_reporter` FOREIGN KEY (`reporter_id`)      REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_reports_reported` FOREIGN KEY (`reported_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
