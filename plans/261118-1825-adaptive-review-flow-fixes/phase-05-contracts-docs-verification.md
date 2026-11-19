---
phase: 5
title: "Contract, tài liệu, kiểm chứng"
status: completed
items: [12, docs, rollback]
---

# Phase 5: Contract, tài liệu, kiểm chứng

Contract sửa trong cùng PR với code (`AGENTS.md` §3.9). Phase này gom việc kiểm chứng cuối, script rollback và các tài liệu
chưa sửa ở phase trước.

<!-- Updated: Red Team Session 1 - thêm trường contract mới, script rollback, ba migration -->

## Mục 12: GET bài ôn có tác dụng phụ

**Trước:** `GET /api/learning/reviews/{reviewId}` giao set, chuyển `SKIPPED` khi hết gói, mở chẩn đoán; contract không nói.

**Sau:** giữ GET, không đổi API. `docs/contracts/lesson-learning-v1.md` ghi rõ các tác dụng phụ, gồm cả hai việc mới: tự hoàn
tất bước lý thuyết khi không còn lý thuyết, và mở chẩn đoán ở GET. Thêm test: hai GET liên tiếp không có nộp bài ở giữa trả
cùng `setId`, cùng `state`, chỉ mở một phiên chẩn đoán.

## Tài liệu cần sửa

| File | Trước | Sau |
|------|-------|-----|
| `AGENTS.md` §3.8 | "Bài ôn lặp set mới đến khi đạt (chỉ lần trượt đầu quay lại theory), chỉ `SKIPPED` khi hết package" | Lý thuyết ở lần trượt 1 và 3; chẩn đoán ở lần 2; lần N thôi chặn (vẫn `PENDING`, vào giỏ ôn); nâng lại thì đếm từ mốc mới; `SKIPPED` khi hết package |
| `AGENTS.md` §3.8 | Chẩn đoán dò ngược: "bài ôn chặn trượt set thứ hai hoặc hết gói thì thăm dò…" | Thêm: mở ở lần mở bài ôn kế tiếp; bỏ qua được sau ≥ 1 probe; probe không set ghi content gap; kết luận có loại |
| `docs/contracts/lesson-learning-v1.md` | `maxFailedSets` luôn null (dòng 294); luật trượt cũ; basket start ghi `CONSOLIDATE`/`MAINTAIN` (dòng 273) | `maxFailedSets = N` cho bài ôn chặn; `blocking` trong phản hồi nộp; luật trượt mới; tác dụng phụ của GET; `theoryScope = NO_LESSON`; `409` khi gói bị gỡ; lý do giỏ ôn `PRIORITY`, `DECAYED`, `UNBLOCKED`; start nhận `PRIORITY`/`DECAYED` với reserve và paywall; `trigger_kind` mới |
| `docs/contracts/diagnostics-v1.md` | Không có skip; probe `OPEN`/`ANSWERED`/`NO_SET` | Endpoint `skip` (cần ≥ 1 probe, `409 DIAGNOSIS_PROBE_REQUIRED`); probe `SKIPPED`; `conclusionKind`; replay nộp probe trả phản hồi đã lưu; thời điểm mở phiên; `diagnosticSessionId = null` khi phiên không có probe |
| `docs/contracts/free-practice-v1.md` | (đúng sẵn: không tạo bài ôn) | Kiểm lại; không đổi nếu đúng |
| `docs/system-architecture.md` | Mô tả luồng bài ôn và chẩn đoán | Cập nhật theo luật mới; ghi phạm vi đã sửa "HTTP ngoài khóa" |
| `infra/config-server/config-repo/learning-service.yaml` | Không có nhóm `learning.review` | `learning.review.unblock-after-failed-sets: 4` (có comment) |
| Figma `adaptive-learning-follow` frame 00, 07, 08, 10 | Luồng cũ | Đã vẽ lại theo bảng trước/sau trong `plan.md` (frame 07 thêm lane D cho lần trượt 3 và lần N) |

## Script rollback

`plans/261118-1825-adaptive-review-flow-fixes/reports/rollback-data-fixup.sql` (chỉ dùng khi phải quay về image cũ; Flyway
không rollback):

- `review_items.theory_reason`: `REPEATED_FAIL` → `SECOND_FAIL`
- `review_items.trigger_kind`: `PRIORITY`, `DECAYED` → `CONSOLIDATE`
- `diagnostic_probes.status`: `SKIPPED` → `NO_SET`

Các cột mới (`failures_counted_from`, `unblocked_at`, `conclusion_kind`, `response`, `decayed_from_tier`) bản cũ không đọc,
để nguyên. Lưu ý: rollback làm mất hiệu lực "thôi chặn"; bài ôn đã hạ cấp vẫn là tùy chọn (`blocking = false`).

## Kiểm chứng cuối

```powershell
mvn -q -pl services/learning-service -am -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -pl services/learning-service -am test      # Testcontainers cần Docker
mvn -q compile -DskipTests
docker compose config --quiet
```

Frontend (repo `frontend`): `npm run lint`, `npm run typecheck`, `npm run build`, test Vitest.

Kịch bản tay trên stack local (`docker compose up -d --build`):

1. Học viên demo làm bài học, sai KP để có bài ôn bắt buộc.
2. Trượt set 1 → lý thuyết. Trượt set 2 → mở lại bài ôn thấy probe; nút bỏ qua tắt. Làm 1 probe → nút bật → bỏ qua → kết
   luận đúng.
3. Trượt set 3 → lý thuyết lại. Trượt set 4 → thông báo chuyển vào giỏ ôn; bài học sau và Practice mở; bài ôn ở giỏ ôn
   với nhãn "Bài ôn chưa đạt".
4. Giỏ ôn có mục `PRIORITY`; bắt đầu được; học viên miễn phí không nhận set premium.

## Kết quả kiểm chứng

Chạy ngày 19/11/2026 trên stack local (gateway, user, content, access, assessment, learning) với frontend Vite.

- `mvn -o test -pl services/learning-service -am`: 1934 test, 1 lỗi `EssayKnowledgeJudgeTest` (file này nhánh không sửa; lỗi có từ trước).
- Frontend: `tsc -b` sạch; test các màn bài ôn, chẩn đoán, giỏ ôn xanh khi chạy riêng. Chạy cả bộ lúc máy đang tải nặng thì vài test
  timeout ngẫu nhiên, chạy lại khi máy rảnh.
- Migration V55–V57 áp dụng thành công trên database cũ của máy dev.
- Đã chạy thật qua gateway: trượt set 1 → lý thuyết; qua quick-check; trượt set 2 → nộp bài không mở phiên, lần GET kế tiếp mới mở;
  `skip` khi chưa có probe trả `409 DIAGNOSIS_PROBE_REQUIRED`; nộp probe hai lần cùng `requestId` trả cùng kết quả; `skip` sau 1 probe
  đóng probe còn lại thành `SKIPPED`, kết luận `GAPS_FOUND`; KP không có prerequisite ghi phiên `CONCLUDED` /
  `NO_PREREQUISITES`; bài ôn sinh từ chẩn đoán không bị chẩn đoán lại.
- Trên giao diện thật: panel chẩn đoán hiện probe, làm probe, nút bỏ qua bật sau 1 probe, hộp xác nhận, bỏ qua thành công.
- **Lỗi tìm thấy nhờ chạy thật và đã sửa:** nhánh hết set (bài ôn `SKIPPED` kèm phiên `PROBING`) không có đường vào probe trên giao
  diện; panel nay hiện cho bài ôn `SKIPPED` còn phiên mở và biến mất khi phiên đóng.
- **Chưa chạy thật:** lần trượt 3 và lần N (thôi chặn, chuyển vào giỏ ôn), giỏ ôn với `PRIORITY` / `DECAYED`, học viên miễn phí không
  nhận set premium. Các luồng này chỉ có test tích hợp. Nguyên nhân: dữ liệu demo chỉ có một gói cho mỗi KP nên bài ôn hết set sau
  lần trượt đầu.
- Vấn đề môi trường dev, không thuộc nhánh này: dữ liệu demo `lesson_exercise_submissions.response = '{}'` làm
  `FirstAttemptMistakes` ném NPE (500 khi mở bài ôn của học viên demo); database content cũ không khớp checksum migration 113/114
  nên phải dùng database mới.

## Mô tả PR

- Pattern §3.9 đã áp dụng: HTTP ngoài khóa và transaction; consumer không đổi.
- Thay đổi hành vi sản phẩm: luật trượt, giỏ ôn, công thức trong `ReviewReevaluation`, cổng Practice với bài ôn đã hạ cấp.
- Ba migration (V55, V56, V57): nới CHECK constraint và thêm cột; không xóa hay sửa dữ liệu cũ.
- Route mới `POST /api/learning/diagnostics/{id}/skip` thuộc tier rate limit `default`.
