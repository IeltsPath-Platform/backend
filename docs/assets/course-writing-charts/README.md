# Course Writing chart upload

Prepared on 2026-10-20. Each course task has its own folder in the public Supabase bucket `media`:
`writtings/leassons/{task-code}/`. Keep the filenames and folder names below unchanged.

Extract `course-writing-charts.zip`, then upload each PNG to the matching folder in the `media` bucket. For example,
`writtings/leassons/PS-W1-WR-1/ps-w1-wr-1-car-ownership.png`. The bucket must allow public reads of these teaching images.
The earlier root-level files under `writtings/lessons/` are not used after V65; upload the files again to the new paths below.

| Course task | PNG | Public URL after upload |
| --- | --- | --- |
| PS-W1-WR-1 — sở hữu ô tô | [ps-w1-wr-1-car-ownership.png](ps-w1-wr-1-car-ownership.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/leassons/PS-W1-WR-1/ps-w1-wr-1-car-ownership.png) |
| PS-W1-WR-2 — chi tiêu gia đình | [ps-w1-wr-2-family-spending.png](ps-w1-wr-2-family-spending.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/leassons/PS-W1-WR-2/ps-w1-wr-2-family-spending.png) |
| PS-W1-WR-3 — hôn nhân và tuổi kết hôn | [ps-w1-wr-3-marriage-age.png](ps-w1-wr-3-marriage-age.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/leassons/PS-W1-WR-3/ps-w1-wr-3-marriage-age.png) |
| PS-W5-WR-1 — khách tham quan công viên | [ps-w5-wr-1-park-visitors.png](ps-w5-wr-1-park-visitors.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/leassons/PS-W5-WR-1/ps-w5-wr-1-park-visitors.png) |
| PS-W5-WR-2 — giá nhiên liệu | [ps-w5-wr-2-fuel-prices.png](ps-w5-wr-2-fuel-prices.png) | [Supabase URL](https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/leassons/PS-W5-WR-2/ps-w5-wr-2-fuel-prices.png) |

[Download the current chart bundle](course-writing-charts.zip). It contains the four existing PNGs plus this README and
the URL list; the new table PNG is pending because ImageMagick was unavailable when these files were prepared. The new
chart's SVG source is in this folder. With `magick` available, run the regeneration command below to create the PNG and
rebuild the bundle. The same mapping is available in [upload-urls.csv](upload-urls.csv). Content migration V65 points
the four existing assets to their URLs; migration V67 points PS-W1-WR-3 to its URL above. Restart Content to apply V66
and V67. The owner must upload `ps-w1-wr-3-marriage-age.png` to `media/writtings/leassons/PS-W1-WR-3/` before the new
chart can load. Once all files are present at these exact paths, the charts can load.
No upload or remote availability check was performed by the agent.

All charts use the exact figures from [V63](../../../services/content-service/src/main/resources/db/migration/V63__writing_lesson_practice_essay_tasks.sql).
Each PNG is 1600 × 1000 pixels. SVG sources are included for editing, but the application loads the PNG URLs above.

To regenerate the files, run from the backend repository (Python standard library + ImageMagick):

```powershell
python scripts/generate-course-writing-charts.py
```

V64 adds initial guidance to the ten course essay tasks and links four chart assets to their question versions. V65 moves
those references into per-task folders. V66 extends the ten guides; V67 adds the W1 table essay and fifth chart asset.
Restart Content to apply V66 and V67; no Learning or Frontend deployment is needed for this content change. For a different bucket/path later, update media references
through a new migration or the existing asset management API; do not edit an already applied migration.
