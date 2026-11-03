-- The wide image behind the header of the course page. The cover image stays the picture on the course card; a course
-- without a banner uses its cover image there, as it did before.
ALTER TABLE courses ADD COLUMN banner_image_url VARCHAR(500);
