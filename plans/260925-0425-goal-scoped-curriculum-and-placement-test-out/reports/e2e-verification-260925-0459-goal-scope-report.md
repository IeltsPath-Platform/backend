---
date: 2026-09-25
kind: verification-report
phase: 5
scope: goal-scoped curriculum, placement test-out and path refresh through the Gateway
---

# Kiểm chứng E2E: path theo goal và test-out từ placement

Chạy trong container Linux (cloud session). Stack dựng lại từ đầu: `user_db`, `content_db`, `assessment_db` tạo
mới; compose `down -v` rồi `up`. Token, mật khẩu và email đều không được ghi.

## Môi trường

| Thành phần | Trạng thái |
| --- | --- |
| Content Flyway | `Successfully applied 5 migrations … now at version v5` (V5 = band ranges) |
| AI Learning Flyway | `Successfully applied 5 migrations … now at version v4` (V4 = band snapshot theo path) |
| Service Java | config → eureka → gateway → user → content → assessment, `java -jar` trên host |
| AI Learning | image build bằng bản sao Dockerfile có CA của sandbox (không commit), như report trước |

## Dữ liệu (bước 4)

| Topic | Band | KP (PROCEDURE) |
| --- | --- | --- |
| BASIC | 4.0–5.0 | KP-BASIC, band hiệu lực 4.0–5.0 (kế thừa topic) |
| COMPLEX | 6.0–7.0 | KP-COMPLEX, 6.0–7.0 |
| ACADEMIC | 7.0–8.0 | KP-ACADEMIC, 7.0–8.0 |

Topic demo do Content V4 seed (`DEMO_READING`) không có band, nên nằm trong path của mọi learner. Vì `sortOrder` là
900, nó luôn đứng cuối path. Trong bảng dưới, KP của nó hiện là `10000000-…0002`.

## Kết quả

| Bước | Kiểm tra | Kết quả |
| --- | --- | --- |
| 5 | Learner A (mục tiêu 5.5): `POST /paths` tạo path gồm BASIC và demo, `addedKnowledgePointCount = 2` | ✅ |
| 5 | Placement A band 4.5, BASIC đúng: consumer `applied`; BASIC `masterySource = placement`; `/status` = `probe` KP demo | ✅ |
| 6 | Learner B (mục tiêu 7.5): path đủ BASIC, COMPLEX, ACADEMIC và demo | ✅ |
| 6 | Placement B band 6.5, COMPLEX sai: BASIC được miễn theo luật (a); `/status` = `practice` COMPLEX | ✅ |
| 7 | Chấm lại placement B lên v2, band 7.0: COMPLEX được miễn theo luật (a); `/status` = `practice` ACADEMIC | ✅ |
| 8 | Thêm KP-COMPLEX-2 (kế thừa 6.0–7.0): GET trước khi làm mới chưa có KP mới | ✅ |
| 8 | `POST /paths`: `addedKnowledgePointCount = 1`, revision +1; gọi lần hai: thêm 0, revision không đổi | ✅ |
| 8 | Sau khi làm mới: mastery và test-out cũ giữ nguyên; `/status` = `probe` KP-COMPLEX-2; 5 dòng band snapshot | ✅ |
| 9 | `/map` của B: BASIC và COMPLEX `placement`, các KP khác rỗng | ✅ |

### Khác với ví dụ trong plan

Plan dự đoán learner A (mục tiêu 5.5) có cả BASIC và COMPLEX trong path. Thực tế path của A **không có COMPLEX**,
vì COMPLEX bắt đầu từ band 6.0 > 5.5. Đây mới là hành vi đúng theo luật D2 đã chốt (`effectiveBandMin ≤ targetBand`);
ví dụ trong plan đã tự mâu thuẫn với luật đó. Item COMPLEX trong placement của A vì vậy được consumer bỏ qua (log
`unknown_knowledge_points`), đúng như phần rủi ro của pha 2.

### Chưa kiểm chứng qua HTTP

- **Retire KP qua HTTP:** Content chưa có API sửa hay tắt KP, nên phần này chỉ được kiểm bằng test
  (`tests/test_path_refresh.py`: KP rời curriculum → `retired`, KP quay lại → được gỡ, band tăng quá mục tiêu → `retired`).
- **`masterySource = system`:** cần đủ bằng chứng để vượt cổng 0.9. Đã có test từ plan trước; bước này không lặp lại.

## Regression Gate

- `mvn -o -pl services/content-service,services/assessment-service,services/user-service -am test`:
  common-security 8, user 93, content 49, assessment 57; **0 fail, 0 skip**. Có Testcontainers `BandRangeMigrationTest`.
- AI Learning pytest với PostgreSQL và RabbitMQ của compose: **128 pass, 0 skip**, gồm 2 test race PostgreSQL
  (lưu hộp chờ / tạo path, và làm mới / consumer).
- `git diff --check` sạch; `.env` không bị commit.
- `graphify update .`: chưa chạy vì môi trường không có graphify.

## Quyết định đổi khi cook

1. **KP có band riêng thay trọn khoảng của topic**, thay vì kế thừa từng đầu (pha 1). Kế thừa từng đầu có thể ra
   khoảng ngược.
2. **Bản chụp band theo path** (`mastery_path_knowledge_point_bands`, V4) được làm ở pha 2 thay vì pha 4.
   `KnowledgePoint` của DeepTutor không có metadata, và consumer không có token để gọi Content.
3. **Không sửa submodule DeepTutor** (pha 4). Submodule trỏ thẳng upstream `HKUDS/DeepTutor`, nhóm không có fork,
   nên commit trong submodule không push được. Provenance nằm trong `note` của override, và IELTSPath đổi nhãn
   `masterySource`.
4. **KP retired** dùng cùng cơ chế override (`retired:content`), vì DeepTutor không có cách ẩn KP (pha 5).
