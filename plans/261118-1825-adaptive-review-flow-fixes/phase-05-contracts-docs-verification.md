---
phase: 5
title: "Contract, tài liệu, kiểm chứng"
status: pending
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
| Figma `adaptive-learning-follow` frame 07, 08, 10 | Luồng cũ | Vẽ lại theo bảng trước/sau trong `plan.md` (làm sau khi merge) |

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

## Mô tả PR

- Pattern §3.9 đã áp dụng: HTTP ngoài khóa và transaction; consumer không đổi.
- Thay đổi hành vi sản phẩm: luật trượt, giỏ ôn, công thức trong `ReviewReevaluation`, cổng Practice với bài ôn đã hạ cấp.
- Ba migration (V55, V56, V57): nới CHECK constraint và thêm cột; không xóa hay sửa dữ liệu cũ.
- Route mới `POST /api/learning/diagnostics/{id}/skip` thuộc tier rate limit `default`.
