-- V24-V29 give meanings to words that existed only as INACTIVE pronunciation rows. Readers of the catalogue need both the
-- item and the sense to be ACTIVE, so those words have to be activated too, as V16 did for the lesson words.
UPDATE vocabulary_items item
SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE item.status = 'INACTIVE'
  AND EXISTS (SELECT 1 FROM vocabulary_senses sense WHERE sense.vocabulary_item_id = item.id AND sense.status = 'ACTIVE');
