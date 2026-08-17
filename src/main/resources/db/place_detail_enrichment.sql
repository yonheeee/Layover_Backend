-- 관광지 상세정보 보강 컬럼
-- 적용 전 places 테이블 백업을 권장합니다.

ALTER TABLE places
    ADD COLUMN rest_date TEXT NULL AFTER operating_hours,
    ADD COLUMN info_center TEXT NULL AFTER rest_date,
    ADD COLUMN parking TEXT NULL AFTER info_center,
    ADD COLUMN use_fee TEXT NULL AFTER parking,
    ADD COLUMN reservation TEXT NULL AFTER use_fee,
    ADD COLUMN kakao_place_id VARCHAR(100) NULL AFTER description,
    ADD COLUMN kakao_place_url VARCHAR(500) NULL AFTER kakao_place_id,
    ADD COLUMN kakao_phone VARCHAR(100) NULL AFTER kakao_place_url,
    ADD COLUMN road_address VARCHAR(500) NULL AFTER kakao_phone,
    ADD COLUMN intro_raw LONGTEXT NULL AFTER road_address,
    ADD COLUMN detail_info_raw LONGTEXT NULL AFTER intro_raw;
