-- 게시글 썸네일 컬럼 추가
ALTER TABLE posts ADD COLUMN thumbnail_url VARCHAR(500) NULL;

-- 문의사항 첨부파일 URL 컬럼 추가 (쉼표로 구분된 URL 목록)
ALTER TABLE inquiries ADD COLUMN attachment_urls TEXT NULL;
