-- ============================================
-- TeamUI — Add optimistic locking columns
-- ============================================

ALTER TABLE meetings ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE meeting_actions ADD COLUMN version BIGINT DEFAULT 0;
