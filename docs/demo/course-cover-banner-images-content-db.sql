-- Demo data only: sets the cover image (thumbnail on the course tile) and the banner image (wide picture behind the course page
-- header) of the five whole-band courses, using the pictures already uploaded to the shared Supabase media bucket
-- (CONTENT_MEDIA_BASE_URL in backend/.env). Other developers' databases start with both columns empty, so the courses show the
-- placeholder art until this script is run.
--
-- The values are storage keys (content-image/<uploader id>/<file>.png), not full URLs: the API resolves them against
-- CONTENT_MEDIA_BASE_URL. They only show a picture when your .env points at the same Supabase project as the one that holds
-- these files; otherwise upload your own pictures in the admin course drawer instead.
--
--   psql -h localhost -U postgres -d content_db -f backend/docs/demo/course-cover-banner-images-content-db.sql
--   (or, with the Compose database: docker cp the file into ieltspath-postgres-1 and run psql -d content_db -f on it)
--
-- Safe to run again: it overwrites the same two columns and touches nothing else. Run it after Flyway has applied V112 (the
-- banner_image_url column) and V103 (the course codes). A code that does not exist in your database is skipped.
-- It is a script and not a Flyway migration on purpose: a migration runs in every environment, these pictures are for one.
UPDATE courses AS c
   SET cover_image_url  = v.cover_image_url,
       banner_image_url = v.banner_image_url,
       updated_at       = CURRENT_TIMESTAMP
  FROM (VALUES
        ('IELTS_FOUNDATION',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/8ad33ffc-d6be-4918-9e54-28affd62c75d.png',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/b277ffec-e4bb-4163-92c5-ef88cd559886.png'),
        ('IELTS_5_0',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/c904c4a3-46a6-4976-a160-9d521ccc9ac3.png',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/1e3c98fa-bd6b-46a4-b3a6-b1893278b5e4.png'),
        ('IELTS_6_0',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/895e5992-3a7c-4d35-b92f-d662cba1e5f4.png',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/f0d22bbb-1f14-407c-82b5-1944d7143559.png'),
        ('IELTS_7_0',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/1bd0b821-435f-4428-af76-613abcc6acd3.png',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/07a97808-542c-400f-b428-a58b127b1196.png'),
        ('IELTS_8_0',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/d1941fd9-d02e-4624-8f2f-bff2b3e3f66b.png',
         'content-image/32b6bd77-3526-4319-8bbf-2ad7e23f057e/843f126f-0f00-44af-9910-3af94ef22a37.png')
       ) AS v (code, cover_image_url, banner_image_url)
 WHERE c.code = v.code;
