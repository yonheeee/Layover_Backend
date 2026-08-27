-- =============================================================
--  캐릭터 도감 랜덤 뽑기 마이그레이션
--
--  적용 순서
--    1) schema.sql            (기본 스키마 — 이 파일은 수정하지 않는다)
--    2) character_collection.sql  ← 이 파일
--    3) character_seed.sql    (147종 INSERT)
--
--  변경 요약
--    - characters      : 스탬프 개수 해금 → code 기반 랜덤 뽑기 풀
--    - user_characters : 보유 목록 → 뽑기 로그 (중복 허용)
--    - stamps          : 장소당 평생 1회 → 하루 1회 (DB 유니크로 보장)
-- =============================================================

USE `daejeon_layover`;

SET NAMES utf8mb4;


-- -------------------------------------------------------------
--  0. 기존 캐릭터 데이터 정리
--
--  기존 characters 행은 required_stamps 기반 해금용이라 새 체계와
--  호환되지 않는다. 147종 시드로 완전히 교체하므로 비우고 시작한다.
--  user_characters 는 FK ON DELETE CASCADE 로 함께 지워지지만,
--  의도를 드러내기 위해 명시적으로 먼저 지운다.
--
--  ⚠ 운영 데이터에 도감 기록이 쌓인 뒤라면 이 블록을 실행하기 전에
--    백업하거나, code 매핑 후 UPDATE 하는 방식으로 바꿔야 한다.
-- -------------------------------------------------------------

DELETE FROM `user_characters`;
DELETE FROM `characters`;


-- -------------------------------------------------------------
--  1. characters — 뽑기 메타데이터
--
--  code       : 이미지 파일명(확장자 제외). 프론트가 이 값으로
--               번들된 이미지를 찾는다. image_url 은 더 이상 쓰지 않는다.
--  kind       : SOLO / DUO / THEME
--  theme      : NULL / BIRTHDAY / EXPO
--               NULL 인 144종만 평상시 뽑기 풀에 들어간다.
--  base_char  : 도감 그룹 키. 'char01', duo 는 'char01+char02'
--
--  테이블을 비운 뒤 실행하므로 NOT NULL 컬럼을 바로 추가해도 안전하다.
-- -------------------------------------------------------------

ALTER TABLE `characters`
  ADD COLUMN `code`      varchar(80) NOT NULL AFTER `id`,
  ADD COLUMN `kind`      varchar(10) NOT NULL DEFAULT 'SOLO' COMMENT 'SOLO / DUO / THEME',
  ADD COLUMN `theme`     varchar(20)          DEFAULT NULL COMMENT 'NULL / BIRTHDAY / EXPO',
  ADD COLUMN `base_char` varchar(30)          DEFAULT NULL COMMENT '도감 그룹 키',
  MODIFY COLUMN `required_stamps` int          DEFAULT NULL COMMENT '미사용 (구 해금 조건)',
  MODIFY COLUMN `image_url`       varchar(500) DEFAULT NULL COMMENT '미사용 (프론트가 code로 해석)',
  ADD UNIQUE KEY `uq_characters_code` (`code`),
  ADD KEY `idx_characters_theme` (`theme`),
  ADD KEY `idx_characters_group` (`base_char`, `code`);


-- -------------------------------------------------------------
--  2. user_characters — 뽑기 로그
--
--  같은 캐릭터를 여러 번 뽑을 수 있어야 하므로 (user_id, character_id)
--  유니크 제약을 제거한다. 대신 어느 스탬프에서 나왔는지 추적한다.
--
--  ⚠ 이 제약이 사라지면 실수로 두 번 INSERT 해도 DB가 막아주지 않는다.
--    user_characters INSERT 는 반드시 StampService.saveStamp 트랜잭션
--    안에서 정확히 한 번만 일어나야 한다.
-- -------------------------------------------------------------

ALTER TABLE `user_characters`
  DROP INDEX `uq_user_character`,
  ADD COLUMN `stamp_id`  char(36)     DEFAULT NULL COMMENT '이 캐릭터가 나온 스탬프',
  ADD COLUMN `place_id`  char(36)     DEFAULT NULL COMMENT '획득 장소 (조회 편의용 비정규화)',
  ADD COLUMN `photo_url` varchar(500) DEFAULT NULL COMMENT '함께 찍은 엽서 사진',
  ADD KEY `idx_uc_user_char` (`user_id`, `character_id`),
  ADD KEY `idx_uc_user_obtained` (`user_id`, `obtained_at`);


-- -------------------------------------------------------------
--  3. stamps — 하루 1회
--
--  기존에는 (user_id, place_id) 조합이 평생 한 번이라 재방문이 영원히
--  막혔다. 하루 단위로 완화하고, 애플리케이션 검사만으로는 동시 요청을
--  막을 수 없으므로 DB 유니크 제약을 진짜 방어선으로 둔다.
--
--  visited_on 은 생성 컬럼이라 visited_at 이 바뀌면 자동으로 따라간다.
--  visited_at 은 서버가 Asia/Seoul 기준으로 넣어야 날짜 경계가 맞는다.
--
--  ⚠ 기존 데이터에 (user_id, place_id) 중복이 있으면 이 ALTER 가 실패한다.
--    구 규칙이 장소당 1회였으므로 중복은 없어야 정상이다.
--    실패하면 아래로 확인:
--      SELECT user_id, place_id, COUNT(*) c FROM stamps
--      GROUP BY user_id, place_id HAVING c > 1;
-- -------------------------------------------------------------

ALTER TABLE `stamps`
  ADD COLUMN `visited_on` date
      GENERATED ALWAYS AS (DATE(`visited_at`)) STORED
      COMMENT '하루 1회 제약용 파생 컬럼',
  ADD UNIQUE KEY `uq_stamps_user_place_day` (`user_id`, `place_id`, `visited_on`);


-- -------------------------------------------------------------
--  적용 확인
-- -------------------------------------------------------------
-- SHOW CREATE TABLE `characters`;
-- SHOW CREATE TABLE `user_characters`;
-- SHOW CREATE TABLE `stamps`;
-- SELECT COUNT(*) FROM `characters`;   -- character_seed.sql 적용 후 147
