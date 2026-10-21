-- Keep each course Writing chart under its stable task code in Supabase Storage.
UPDATE content_assets AS asset
SET media_reference = charts.public_url
FROM (VALUES
    ('PS-W1-WR-1', 'ps-w1-wr-1-car-ownership.png'),
    ('PS-W1-WR-2', 'ps-w1-wr-2-family-spending.png'),
    ('PS-W5-WR-1', 'ps-w5-wr-1-park-visitors.png'),
    ('PS-W5-WR-2', 'ps-w5-wr-2-fuel-prices.png')
) AS chart(task_code, filename)
CROSS JOIN LATERAL (
    SELECT 'https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/leassons/'
        || chart.task_code || '/' || chart.filename AS public_url
) AS charts
WHERE asset.id = md5('course-writing-chart:' || chart.task_code)::uuid;
