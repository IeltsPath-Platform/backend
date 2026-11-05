-- An examiner takes at most 30 new jobs a day, Writing and Speaking together. The daily limit was 10 by default; examiners
-- still on that default move to 30, a limit someone chose on purpose stays.
ALTER TABLE examiner_profiles ALTER COLUMN daily_limit SET DEFAULT 30;
UPDATE examiner_profiles SET daily_limit = 30, updated_at = CURRENT_TIMESTAMP WHERE daily_limit = 10;
