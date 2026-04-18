-- ============================================================
-- E-Waste Recycling Manager — database migration patch
-- Run this against your existing ewastecs database
-- ============================================================

-- 1. Make offer_status nullable (handles old rows that have NULL)
ALTER TABLE `client_ewaste_agents`
  MODIFY COLUMN `offer_status` varchar(20) NULL DEFAULT 'ACTIVE';

-- 2. Set all legacy NULL rows to ACTIVE (they were direct admin assignments)
UPDATE `client_ewaste_agents`
  SET `offer_status` = 'ACTIVE'
  WHERE `offer_status` IS NULL OR `offer_status` = '';

-- 3. Add index for faster lookups
ALTER TABLE `client_ewaste_agents`
  ADD INDEX IF NOT EXISTS `idx_cea_pickup_status` (`client_ewaste_id`, `offer_status`);

-- offer_status lifecycle:
--   INVITED  = admin invited agent to make a price offer (pickup still PENDING)
--   OFFERED  = agent submitted a price offer (awaiting client acceptance)
--   ACTIVE   = client accepted offer OR direct admin assignment
--   REJECTED = client chose a different agent
