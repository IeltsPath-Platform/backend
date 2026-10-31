ALTER TABLE lesson_progress
    ADD COLUMN grandfathered BOOLEAN NOT NULL DEFAULT false;

UPDATE lesson_progress
SET grandfathered = true;
