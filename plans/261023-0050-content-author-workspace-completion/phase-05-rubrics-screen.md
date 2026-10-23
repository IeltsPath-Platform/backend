# Phase 05: Màn "Rubric chấm Writing"

Người làm: **Codex** (đổi từ Claude Code ngày 2026-10-23). Nhánh: `feat/content-author-screens` (frontend, worktree `../frontend-claude`).
Trạng thái: In progress — client/lịch sử phiên bản đã triển khai sau cờ false; rollout còn Blocked đến khi backend/contract merge main và kiểm live, toàn bộ suite còn 2 lỗi flashcard ngoài phạm vi.

Repo: `frontend`. API xem/sửa/kích hoạt đã có; bật lịch sử đầy đủ sau khi xác minh backend/contract merge main, rồi chạy lại focused tests/các cổng kiểm và checklist live.

## Mục tiêu

Tác giả xem rubric đang dùng cho Writing Task 1 và Task 2, soạn phiên bản nháp mới (tiêu chí, mô tả từng band, bài mẫu), rồi kích hoạt. Rubric là căn cứ chấm Writing nên thao tác kích hoạt phải rõ ràng và có xác nhận.

## API (đã có, `RubricController`)

- `GET /api/content/rubrics?task=TASK_1|TASK_2` → rubric đang ACTIVE (mảng).
- `GET /api/content/admin/rubrics?task=…` → các bản nháp.
- `POST /api/content/admin/rubrics` body `RubricRequest`:
  ```json
  {
    "code": "IELTS_WRITING_T2", "task": "TASK_2", "version": 3, "sourceNote": "…",
    "criteria": [
      { "code": "TR", "name": "Task Response", "description": "…", "sortOrder": 1,
        "bandDescriptors": [ { "band": 9.0, "descriptor": "…" } ] }
    ],
    "examples": [ { "band": 6.5, "prompt": "…", "essay": "…", "commentary": "…" } ]
  }
  ```
  `criteria` không rỗng, mỗi tiêu chí có ít nhất một `bandDescriptors`; band 0.0..9.0.
- `PUT /api/content/admin/rubrics/{code}/{version}` cùng body (code/version trong body phải khớp path, nếu không 400). Chỉ sửa bản nháp.
- `POST /api/content/admin/rubrics/{code}/{version}/activate`.
- Response: `{ code, task, version, status, sourceNote, criteria: [{ … }], examples: [...] }`. Đọc `CriterionResponse` trong `RubricController.java` để viết schema đúng.

### API lịch sử — triển khai local, chỉ bật sau khi xác minh merge

Theo thông tin người dùng cung cấp 2026-10-23:

- `GET /api/content/admin/rubrics/versions?code=<code>` → `[{ code, task, version, status }]`, gồm cả `RETIRED`, sắp theo `version`.
- Sau khi merge: lấy danh sách phiên bản theo mã rubric và tính bản tiếp theo bằng `max(version trong danh sách) + 1`; parse response bằng Zod, giữ trạng thái tải/lỗi và không suy đoán số phiên bản khi chưa có dữ liệu.
- Nguồn contract đã đọc trên nhánh backend: `docs/contracts/learning-content-internal-v1.md`, dòng GET `/api/content/admin/rubrics/versions`. Được triển khai local theo contract; xác minh merge main trước khi bật cờ, sau đó kiểm lại và chạy checklist live.
- Trước khi merge: giữ phép tính hiện có theo các phiên bản ACTIVE/DRAFT đã tải và nhãn có giới hạn; vẫn báo chặn vì chưa biết bản RETIRED. Không gọi endpoint mới, không thử các số phiên bản dự đoán.

Kiểm trong backend (đọc `ManageRubricsUseCase`) trước khi code: kích hoạt có hạ bản đang ACTIVE xuống không, `version` có phải lớn hơn bản hiện có không, mã lỗi trả về. Ghi kết luận vào đầu file `types.ts` bằng một comment ngắn nếu hành vi không hiển nhiên.

## Tạo

```text
src/features/admin/rubrics/
  types.ts, api/rubricApi.ts, lib/rubricDraft.ts   # chuyển đổi rubric → bản nháp mới (version + 1)
  pages/RubricsPage.tsx
  components/RubricView.tsx, components/RubricEditor.tsx, components/CriterionEditor.tsx
  pages/RubricsPage.test.tsx, lib/rubricDraft.test.ts
```

- Menu: `{ path: '/content-author/rubrics', label: 'Rubric chấm Writing', icon: Scale, roles: ['CONTENT_AUTHOR'] }`. Route trong nhánh `isAuthorArea`.
- `RubricsPage`: tab "Task 1" / "Task 2" (lưu tab trong query `?task=`). Mỗi tab: khối "Đang dùng" (`RubricView` chỉ đọc: tiêu chí, bảng band → mô tả, bài mẫu trong `<details>`), khối "Bản nháp" (danh sách bản nháp, mở để sửa), nút "Tạo bản nháp từ bản đang dùng" (`rubricDraft.ts` sao chép rubric ACTIVE, `version = max(version đã có) + 1`).
- `RubricEditor`: `sourceNote`; danh sách tiêu chí (thêm/xóa/lên/xuống, mỗi tiêu chí: code, tên, mô tả, danh sách band descriptor có band select 0..9 bước 0.5 và textarea); danh sách bài mẫu (band, đề, bài, nhận xét). Kiểm client trùng điều kiện server (tiêu chí không rỗng, mỗi tiêu chí ≥ 1 band, band không trùng trong một tiêu chí). "Lưu bản nháp" (POST lần đầu, PUT các lần sau). "Kích hoạt" mở hộp xác nhận ghi rõ: "Rubric này sẽ được dùng để chấm mọi bài Writing Task X từ bây giờ."
- Textarea dài: dùng `adm-textarea`, tự cao theo nội dung không cần (giữ đơn giản), `maxLength` hợp lý.

## Test

- `rubricDraft.test.ts`: sao chép đủ tiêu chí/bài mẫu, tăng version đúng, không giữ `status`.
- `RubricsPage.test.tsx`: tab đổi task gọi API với `task` đúng; hiện rubric ACTIVE; tạo bản nháp gửi POST đúng body; lưu lần hai gửi PUT tới đúng `{code}/{version}`; tiêu chí không có band bị chặn; kích hoạt cần xác nhận; menu chỉ cho CONTENT_AUTHOR.

## Kiểm thủ công

Task 2: tạo bản nháp từ bản đang dùng, sửa mô tả band 7 của một tiêu chí, lưu, tải lại thấy giữ nguyên; kích hoạt; nộp một bài Writing Task 2 ở web học (nếu môi trường có LLM key) và xác nhận không lỗi. Nếu không chạy được chấm thật, ghi rõ.

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh.

## Kết quả

- 2026-10-23, nhánh frontend `feat/content-author-screens`: màn chọn Task theo query, xem rubric đang dùng/tiêu chí/band/bài mẫu, tạo bản nháp version lớn nhất đang đọc + 1, sao chép sâu không gửi status/knowledgePointId, POST lần đầu/PUT lần sau, kiểm band trùng và tiêu chí không rỗng. Kích hoạt cần lưu mọi thay đổi và xác nhận tác động chấm bài. Menu/route chỉ CONTENT_AUTHOR.
- Đọc RubricController, ManageRubricsUseCase, Rubric và JdbcRubricRepository: kích hoạt chuyển rubric ACTIVE cùng task thành RETIRED; version chỉ cần dương và duy nhất, không bắt tăng; mã tiêu chí được ánh xạ KP lúc đọc, không thuộc request. Chỉ sửa DRAFT (409 RUBRIC_NOT_EDITABLE). Kích hoạt cần đúng TA/TR, CC, LR, GRA và mỗi tiêu chí có đủ band 4.0–9.0 bước 0.5; UI giải thích và kiểm trước xác nhận. Schema CriterionResponse có knowledgePointId nullable, không dùng SubKnowledgePoint nội bộ.
- 14 test rubric/menu qua; typecheck, lint qua. Toàn bộ suite: 93 file qua / 1 lỗi flashcard tired, 564 test qua / 1 lỗi; mã flashcard không thay đổi. Build đã chạy (xem cập nhật kiểm chứng cuối lượt).
- Chưa kiểm lưu/tải lại/kích hoạt/chấm Writing thật: gateway localhost:8080 và localhost:18080 đều không kết nối được khi kiểm ngoài sandbox; chưa có phiên content@gmail.com trên backend thật. Không tuyên bố đã gọi LLM hoặc hoàn thành checklist.
- Kiểm tra sau tích hợp: typecheck, lint, build qua; npm test 95 file / 573 test qua. Lỗi flashcard ngẫu nhiên ở lượt trước được giữ nguyên để báo rủi ro, không sửa hoặc bỏ test. Commit riêng: `2f41879 feat(rubrics): edit and activate writing rubrics`. Chờ cập nhật link PR sau review.
- Rà soát bổ sung theo migration V50 thật: mã tiêu chí chỉ TA/TR/CC/LR/GRA, tên tối đa 120 ký tự; schema/form đã khớp giới hạn lưu trữ.
- Đã push; [draft PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29), commit bổ sung `dad8530`. Rà soát code không phát hiện lỗi mới ngoài giới hạn version history đã ghi ở Việc cho bên kia.
- Lượt kiểm cuối: typecheck/lint/build qua; npm test 577 qua / 1 lỗi flashcard tired ngẫu nhiên ngoài phạm vi (`owl-reactions.test.ts:109`). Không tuyên bố tất cả cổng xanh; không sửa/bỏ test ngoài phạm vi. Chưa chạy checklist thật, chưa tự merge PR.
- Lượt mới nhất 2026-10-23: hiển thị "Bản tiếp theo: N (theo các phiên bản đã tải)" bằng chính `rubricDraft` hiện có, dùng max ACTIVE/DRAFT đã tải + 1. Test danh sách nháp không theo thứ tự (v7, v3) xác nhận hiển thị và POST v8. Không đổi API hoặc cơ chế chọn phiên bản, không đoán version RETIRED, không tự thử lại POST với số lớn hơn. **Giới hạn còn chặn:** dữ liệu hiện tại không bao gồm RETIRED nên N chưa được bảo đảm là số lớn nhất của toàn bộ lịch sử + 1; chờ backend bổ sung dữ liệu này.
- Kiểm chứng lượt mới nhất: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua; 78 test author/menu/route qua. Toàn bộ `npm.cmd test`: 580 qua / 1 lỗi flashcard confused tại `owl-reactions.test.ts:87`, ngoài phạm vi (94/95 file qua), không sửa/bỏ test.
- Checklist chưa chạy: sửa mô tả band 7 rồi lưu/tải lại, kích hoạt, nộp và chấm Writing Task 2 thật, kiểm 390px. Gateway 8080/18080 timeout khi kiểm ngoài sandbox; không có xác nhận chấm LLM thật. Không tự merge PR.
- Đã push `1fe94f9 feat(rubrics): show next version from loaded revisions` vào [draft PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29). Reviewer xác nhận nhãn dùng phép tính hiện có và giới hạn dữ liệu đã tải, không thêm suy đoán phiên bản hoặc API mới.
- Thông báo API mới 2026-10-23: đã kiểm tra trực tiếp GitHub backend main `c600b3f399dddcbef060cdea6e7438bbf9a5f42d` và contract/controller tại đúng commit đó. Chưa có `/api/content/admin/rubrics/versions` trong `learning-content-internal-v1.md` hoặc controller; chưa xác nhận merge. Giữ nguyên source và hành vi frontend, chưa gọi API mới.
- Lượt này chỉ cập nhật file phase và mô tả PR #29; không có commit/frontend source mới, không chạy lại test/build hoặc checklist backend thật, không tuyên bố đã tích hợp API phiên bản đầy đủ.

### Cập nhật hiện tại 2026-10-23 — implementation sau cờ, chưa rollout

- Đã thêm `fetchRubricVersions`, Zod response gồm `DRAFT | ACTIVE | RETIRED` và hàm thuần `nextRubricVersion` tính `max(version) + 1`, kể cả danh sách chỉ có RETIRED. Đường UI khi bật cờ có trạng thái tải/lỗi/Thử lại; chưa có lịch sử hợp lệ thì khóa tạo nháp, không suy đoán hoặc retry POST với số khác.
- `RUBRIC_VERSIONS_ENABLED = false` tại `rubrics/rubricFlags.ts`. Hiện tại không gọi API history; giữ nhãn có giới hạn **theo các phiên bản đã tải** và phép tính ACTIVE/DRAFT đang có. Không tuyên bố fallback này biết toàn bộ lịch sử RETIRED.
- Thứ tự kích hoạt: xác minh backend code/contract đã merge main → đổi cờ true → chạy lại focused tests, typecheck/lint/test/build và checklist live. Không yêu cầu chạy thành công UI history khi cờ còn false. Endpoint và contract đã có trên nhánh; việc còn lại là merge/kiểm thật, không yêu cầu bổ sung lại API phiên bản.
- Kiểm chứng cuối lượt: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0); nhóm rubric 4 file / 28 test qua. Toàn bộ `npm.cmd test` exit 1: 102/103 file qua, 670/672 test qua; 2 lỗi ngẫu nhiên đã biết ở `flashcards/lib/owl-reactions.test.ts:87` (confused) và `:109` (tired), ngoài phạm vi. Không sửa/bỏ test, không chạy lại để đổi kết quả. Build còn cảnh báo kích thước chunk sẵn có.
- Không tuyên bố tất cả cổng xanh hoặc phase hoàn thành. Commit local: `c6039b6 feat(rubrics): prepare complete version history`. Không push thêm hoặc merge; [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) vẫn nháp, không cập nhật remote trong lượt này.
- Gateway 8080/18080 timeout trong lượt này. Sửa/lưu/tải lại band 7, kích hoạt, chấm Writing Task 2 thật và kiểm 390px vẫn chưa chạy; không có xác nhận gọi LLM thật.

## Việc cho bên kia

- API danh sách version gồm RETIRED đã được thông báo là có trên nhánh backend: `GET /api/content/admin/rubrics/versions?code=<code>`. Chờ merge code và contract; không yêu cầu bổ sung lại. Hai GET đang dùng chỉ trả ACTIVE/DRAFT, trong khi `JdbcRubricRepository.activate` cho phép kích hoạt bản thấp hơn và DB giữ UNIQUE(code,rubric_version). Trường hợp UI tạo nháp v2 và v3, kích hoạt v3 rồi kích hoạt nháp v2: v3 chuyển RETIRED, hai GET không còn trả v3, frontend đề xuất v3 đã tồn tại. Frontend sẽ dùng danh sách đầy đủ sau khi xác minh main; không retry POST mù hoặc tự đoán số phiên bản.
