-- =============================================================
--  캐릭터 도감 랜덤 뽑기 마이그레이션 — TiDB(운영)용
--
--  character_collection.sql 과 결과 스키마는 같고, TiDB에서 실행할 수
--  없는 부분만 바꿨다. 로컬 MySQL은 기존 파일을 그대로 쓴다.
--
--  기존 파일과 다른 점
--    - stamps.visited_on 을 STORED 대신 VIRTUAL 생성 컬럼으로 추가한다.
--      TiDB는 ALTER TABLE 로 STORED 생성 컬럼을 추가할 수 없다.
--      (https://docs.pingcap.com/tidb/stable/generated-columns/ Limitations)
--      애플리케이션은 visited_on 을 직접 읽지 않고 유니크 제약 위반으로만
--      하루 1회를 판정하므로 VIRTUAL 이어도 동작은 같다.
--    - ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS /
--      DROP INDEX IF EXISTS 를 써서, 일부만 적용된 상태에서 다시 실행해도
--      실패하지 않게 했다. (MySQL은 ADD COLUMN IF NOT EXISTS 를 지원하지
--      않으므로 이 파일은 TiDB 전용이다.)
--    - USE 문을 뺐다. 운영 DB 이름이 로컬(daejeon_layover)과 다를 수 있으니
--      SQL 편집기에서 운영 DB를 먼저 선택하고 실행한다.
--    - user_characters 의 기존 유니크 인덱스는 새 인덱스를 만든 뒤에 지운다.
--      user_id 외래 키가 쓸 인덱스가 하나도 없는 순간이 생기지 않게 하기 위해서다.
--
--  실행 순서
--    0) 아래 [사전 점검] 쿼리로 현재 상태 확인
--    1) 이 파일
--    2) character_seed.sql  (147종 INSERT, 수정 없이 그대로)
--    3) 맨 아래 [적용 확인] 쿼리
-- =============================================================

SET NAMES utf8mb4;


-- -------------------------------------------------------------
--  [사전 점검] — 실행 전에 한 줄씩 직접 실행해 결과를 확인한다.
--
--  SHOW COLUMNS FROM `characters` LIKE 'code';
--      → 비어 있으면 마이그레이션이 적용되지 않은 상태 (도감 API 500의 원인)
--  SELECT COUNT(*) FROM `user_characters`;
--      → 0이 아니면 아래 1번 블록이 도감 기록을 지운다. 먼저 백업한다.
--  SELECT s.user_id, s.place_id, s.d, COUNT(*) c
--    FROM (SELECT user_id, place_id, DATE(visited_at) AS d FROM `stamps`) s
--    GROUP BY s.user_id, s.place_id, s.d HAVING COUNT(*) > 1;
--      → 결과가 있으면 4번 블록의 유니크 인덱스 생성이 실패한다. 중복부터 정리한다.
--        (TiDB는 ONLY_FULL_GROUP_BY 라서 DATE() 를 하위 쿼리에서 먼저 계산한다.)
--
--  가상 생성 컬럼에 유니크 인덱스를 만들 수 있는지 빈 임시 테이블로 확인한다.
--  운영 테이블에는 영향이 없다. 실패하면 이 파일을 실행하지 말고 알려 주세요.
--
--  CREATE TABLE `_layover_vgc_check` (
--    `visited_at` datetime NOT NULL,
--    `visited_on` date GENERATED ALWAYS AS (DATE(`visited_at`)) VIRTUAL,
--    UNIQUE KEY `uq_check` (`visited_on`)
--  );
--  DROP TABLE `_layover_vgc_check`;
-- -------------------------------------------------------------


-- -------------------------------------------------------------
--  1. 기존 캐릭터 데이터 정리
--
--  기존 characters 행은 required_stamps 기반 해금용이라 새 체계와 호환되지
--  않는다. 147종 시드로 완전히 교체하므로 비우고 시작한다.
--  이 블록 때문에 이 파일을 다시 실행하면 character_seed.sql 도 다시 실행해야 한다.
-- -------------------------------------------------------------

DELETE FROM `user_characters`;
DELETE FROM `characters`;


-- -------------------------------------------------------------
--  2. characters — 뽑기 메타데이터
-- -------------------------------------------------------------

ALTER TABLE `characters`
  ADD COLUMN IF NOT EXISTS `code` varchar(80) NOT NULL AFTER `id`;
ALTER TABLE `characters`
  ADD COLUMN IF NOT EXISTS `kind` varchar(10) NOT NULL DEFAULT 'SOLO' COMMENT 'SOLO / DUO / THEME';
ALTER TABLE `characters`
  ADD COLUMN IF NOT EXISTS `theme` varchar(20) DEFAULT NULL COMMENT 'NULL / BIRTHDAY / EXPO';
ALTER TABLE `characters`
  ADD COLUMN IF NOT EXISTS `base_char` varchar(30) DEFAULT NULL COMMENT '도감 그룹 키';

ALTER TABLE `characters`
  MODIFY COLUMN `required_stamps` int DEFAULT NULL COMMENT '미사용 (구 해금 조건)';
ALTER TABLE `characters`
  MODIFY COLUMN `image_url` varchar(500) DEFAULT NULL COMMENT '미사용 (프론트가 code로 해석)';

CREATE UNIQUE INDEX IF NOT EXISTS `uq_characters_code` ON `characters` (`code`);
CREATE INDEX IF NOT EXISTS `idx_characters_theme` ON `characters` (`theme`);
CREATE INDEX IF NOT EXISTS `idx_characters_group` ON `characters` (`base_char`, `code`);


-- -------------------------------------------------------------
--  3. user_characters — 뽑기 로그 (같은 캐릭터 중복 획득 허용)
--
--  ⚠ (user_id, character_id) 유니크 제약이 사라지므로 INSERT 는
--    StampService.saveStamp 트랜잭션 안에서 정확히 한 번만 일어나야 한다.
-- -------------------------------------------------------------

ALTER TABLE `user_characters`
  ADD COLUMN IF NOT EXISTS `stamp_id` char(36) DEFAULT NULL COMMENT '이 캐릭터가 나온 스탬프';
ALTER TABLE `user_characters`
  ADD COLUMN IF NOT EXISTS `place_id` char(36) DEFAULT NULL COMMENT '획득 장소 (조회 편의용 비정규화)';
ALTER TABLE `user_characters`
  ADD COLUMN IF NOT EXISTS `photo_url` varchar(500) DEFAULT NULL COMMENT '함께 찍은 엽서 사진';

CREATE INDEX IF NOT EXISTS `idx_uc_user_char` ON `user_characters` (`user_id`, `character_id`);
CREATE INDEX IF NOT EXISTS `idx_uc_user_obtained` ON `user_characters` (`user_id`, `obtained_at`);
DROP INDEX IF EXISTS `uq_user_character` ON `user_characters`;


-- -------------------------------------------------------------
--  4. stamps — 장소당 하루 1회 (DB 유니크가 실제 방어선)
--
--  visited_at 은 서버가 Asia/Seoul 기준으로 넣어야 날짜 경계가 맞는다.
-- -------------------------------------------------------------

ALTER TABLE `stamps`
  ADD COLUMN IF NOT EXISTS `visited_on` date
      GENERATED ALWAYS AS (DATE(`visited_at`)) VIRTUAL
      COMMENT '하루 1회 제약용 파생 컬럼';

CREATE UNIQUE INDEX IF NOT EXISTS `uq_stamps_user_place_day`
  ON `stamps` (`user_id`, `place_id`, `visited_on`);


-- -------------------------------------------------------------
--  [적용 확인] — character_seed.sql 까지 실행한 뒤 확인한다.
--
--  SHOW CREATE TABLE `characters`;
--  SHOW CREATE TABLE `user_characters`;
--  SHOW CREATE TABLE `stamps`;
--  SELECT COUNT(*) FROM `characters`;   -- 147
-- -------------------------------------------------------------
