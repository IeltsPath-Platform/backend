-- A topic teaches the skills of its lessons. The single skill column stays for readers that still use it: it holds
-- the skill of a one-skill topic and NULL for a topic of several skills.
ALTER TABLE topic_progress ADD COLUMN skills VARCHAR(20)[] NOT NULL DEFAULT '{}';
UPDATE topic_progress SET skills = ARRAY[skill] WHERE skill IS NOT NULL;
