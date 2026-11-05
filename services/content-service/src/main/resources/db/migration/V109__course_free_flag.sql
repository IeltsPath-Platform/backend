-- A free course is open to everyone: the per-skill free lessons and the paywall do not apply to it. IELTS Foundation is
-- the first such course; the flag can be switched in the admin course form.
ALTER TABLE courses ADD COLUMN free BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE courses SET free = TRUE WHERE code = 'IELTS_FOUNDATION';
