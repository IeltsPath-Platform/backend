-- Cover image (thumbnail on the course tile) and banner image (wide picture behind the course page header) of the five
-- whole-band courses. The values are storage keys in the shared media bucket, not full URLs: the API resolves them
-- against CONTENT_MEDIA_BASE_URL. A course code that does not exist is skipped.
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
