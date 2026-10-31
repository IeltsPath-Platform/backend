UPDATE content_assets
SET media_reference = replace(media_reference, 'writtings/leassons/', 'writing/lessons/')
WHERE media_reference LIKE '%writtings/leassons/%';
