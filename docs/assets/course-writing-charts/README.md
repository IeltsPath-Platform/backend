# Course Writing chart upload

Prepared on 2026-10-20. Upload the four **PNG** files to Supabase bucket `media`, folder `writtings/lessons`.
Keep the names below unchanged. The bucket must allow public reads of these teaching images.

| Course task | PNG | Public URL after upload |
| --- | --- | --- |
| PS-W1-WR-1 — sở hữu ô tô | [ps-w1-wr-1-car-ownership.png](ps-w1-wr-1-car-ownership.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/lessons/ps-w1-wr-1-car-ownership.png) |
| PS-W1-WR-2 — chi tiêu gia đình | [ps-w1-wr-2-family-spending.png](ps-w1-wr-2-family-spending.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/lessons/ps-w1-wr-2-family-spending.png) |
| PS-W5-WR-1 — khách tham quan công viên | [ps-w5-wr-1-park-visitors.png](ps-w5-wr-1-park-visitors.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/lessons/ps-w5-wr-1-park-visitors.png) |
| PS-W5-WR-2 — giá nhiên liệu | [ps-w5-wr-2-fuel-prices.png](ps-w5-wr-2-fuel-prices.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/lessons/ps-w5-wr-2-fuel-prices.png) |

[Download all four PNGs and this URL list](course-writing-charts.zip).
The same mapping is available in [upload-urls.csv](upload-urls.csv). These URLs are configured in Content migration V64;
the images will load once the owner uploads them. No upload or remote availability check was performed by the agent.

All charts use the exact figures from [V63](../../../services/content-service/src/main/resources/db/migration/V63__writing_lesson_practice_essay_tasks.sql).
Each PNG is 1600 × 1000 pixels. SVG sources are included for editing, but the application loads the PNG URLs above.

To regenerate the files, run from the backend repository (Python standard library + ImageMagick):

```powershell
python scripts/generate-course-writing-charts.py
```

Deploy Content with V64, then Learning, then Frontend. V64 adds detailed guidance to all ten course essay tasks and links
these four chart assets to their question versions. Restarting Content applies V64 through the existing Flyway setup.
For a different bucket/path later, update media references through a new migration or the existing asset management API;
do not edit an already applied V64.
