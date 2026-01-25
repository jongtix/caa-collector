-- WatchlistStock 테이블 컬럼 추가 및 기본값 설정

-- Step 1: 컬럼 추가 (NULL 허용)
ALTER TABLE watchlist_stock ADD COLUMN asset_type VARCHAR(20) NULL;
ALTER TABLE watchlist_stock ADD COLUMN backfill_completed BOOLEAN NULL;

-- Step 2: 기존 데이터 기본값 설정
UPDATE watchlist_stock SET asset_type = 'DOMESTIC_STOCK' WHERE asset_type IS NULL;
UPDATE watchlist_stock SET backfill_completed = false WHERE backfill_completed IS NULL;

-- Step 3: NOT NULL 제약조건 추가
ALTER TABLE watchlist_stock MODIFY COLUMN asset_type VARCHAR(20) NOT NULL;
ALTER TABLE watchlist_stock MODIFY COLUMN backfill_completed BOOLEAN NOT NULL;
