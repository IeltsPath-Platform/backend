---
phase: 4
title: "Contract test and docs"
status: pending
priority: P2
effort: "2h"
dependencies: [3]
---

# Phase 4: Contract test và tài liệu

## Overview

Smoke ở phase 1 đã chốt các câu hỏi bằng tay; phase 3 viết client theo kết quả đó, nhưng test của phase 3 là **mock** nên chỉ
chứng minh code khớp với hiểu biết của ta. Phase này thêm **contract test tự động** chạy với Supabase thật (mặc định skip) để giữ
cho hiểu biết ấy đúng theo thời gian, rồi cập nhật tài liệu. **Một lần chạy contract xanh là cổng** để các plan service dùng module.

<!-- Updated: Validation Session 2 - chạy trên project hiện tại, bỏ guard so URL, giữ allow-list bucket -->

## Requirements

- Chức năng: một contract test chạy trong bucket `contract-test-*` của project Supabase hiện tại (quyết định 2026-10-19), mặc định bị skip; tài liệu nêu module, biến môi trường, quy trình xoay khóa, cách chạy.
- Phi chức năng: contract test không để lại object rác, không in khóa, không chạy trong CI mặc định, và **không thể** vô tình chạm bucket thật (allow-list tiền tố bucket); mọi tài liệu chỉ ghi **tên** biến, không ghi giá trị.

## Architecture

### Contract test

`shared/common-storage/src/test/java/com/ieltspath/commonstorage/contract/SupabaseStorageContractTest.java`

- **Biến riêng**, khác tên biến production (tránh bật nhầm khi môi trường đã có `SUPABASE_*`): `STORAGE_CONTRACT_URL`, `STORAGE_CONTRACT_KEY_FILE` (đường dẫn tới **file** chứa khóa, nằm ngoài repo), `STORAGE_CONTRACT_BUCKET`. Thiếu biến hoặc không đọc được file thì JUnit báo **skipped** kèm lý do, không phải pass.
- **Allow-list, không phải deny-list:** chỉ chạy khi tên bucket **bắt đầu bằng `contract-test-`**; ngược lại báo skipped. Mọi thao tác, kể cả dọn dẹp, chỉ nhận bucket đã qua allow-list; không có đường nào tới bucket khác. (Không còn so `STORAGE_CONTRACT_URL` với `SUPABASE_URL`: hai giá trị này cố ý có thể trùng vì dùng chung project.)
- Mọi key nằm dưới `contract-test/{uuid}/` và bị xóa trong `finally`/`@AfterEach`.
- Dựng `SupabaseObjectStorage` **thật** qua cùng đường code với production; phần HTTP thẳng tới URL signed dùng `java.net.http.HttpClient` (mô phỏng trình duyệt).
- Khóa đọc từ file vào bộ nhớ, **không** gán vào biến môi trường hay in ra; `toString()` của mọi đối tượng chứa khóa che nó.

Kịch bản (mỗi cái trả lời một điều chưa chắc):

| # | Kịch bản | Chốt điều gì |
| --- | --- | --- |
| 1 | `put` PNG 1×1 hợp lệ (tạo trong test); `put` lại **cùng key** → `ALREADY_EXISTS`; `exists` → `true` | Không ghi đè; phân loại lỗi đúng với Supabase thật |
| 2 | `exists` cho key chưa từng có → `false` | Endpoint `exists` và mã 404 |
| 3 | `signedDownloadUrl(ttl=2s)` → GET bằng `HttpClient` trả đúng từng byte; chờ 3 giây → GET trả 4xx | URL ghép đúng (`/storage/v1` + `signedURL`) và TTL có hiệu lực |
| 4 | `signedUploadUrl` → `PUT` byte tới URL (header theo smoke) → 2xx; `exists` → `true`; tải lại bằng signed download | Luồng upload trực tiếp của trình duyệt chạy được |
| 5 | `delete` → `exists` đổi từ `true` sang `false`; `delete` lần hai không ném | Xóa **thật** (không phải no-op) và idempotent; kịch bản cũ chỉ kiểm GET lỗi nên không phân biệt được |
| 6 | Khóa sai (đổi một ký tự trong bộ nhớ) → `UNAUTHORIZED`/`FORBIDDEN`; message không chứa khóa hay key | Ánh xạ lỗi thật |
| 7 | Ghi vào báo cáo **loại khóa** (`sb_secret` hoặc `jwt`, chỉ tiền tố, không in khóa) | Nhánh header nào đang chạy thật |

### Cổng cho plan service

- Một lần chạy xanh cả 7 kịch bản trong bucket `contract-test-*` phải được ghi vào `plans/reports/contract-run-<timestamp>-supabase-storage-report.md` (không chứa khóa, không chứa key thật).
- **Cho tới khi có báo cáo đó**: `plan.md` (frontmatter `status`) **không** chuyển `completed`, `blocks` giữ nguyên, và các plan service (avatar, Speaking, content) **không được bắt đầu** dùng module.
- Cho phép "chưa chạy" chỉ để merge module vào nhánh tích hợp; không đủ để mở khóa plan service.

### Tài liệu cần sửa (đọc từng file trước khi sửa)

| File | Sửa gì |
| --- | --- |
| `AGENTS.md` | Bảng module (dòng ~21): thêm `shared/common-storage`. Dòng ~43: **đếm lại** số module từ `<modules>` của `pom.xml` (hiện 14, thành 15). Dòng ~49 và cây thư mục (~236): thêm module. Dòng ~325: thêm lệnh `mvn -q -pl shared/common-storage test`. Mục §3.4: một dòng **trung thực**: "khóa Supabase chỉ nằm ở `.env` (gitignored); mọi service đều import `.env` gốc nên việc chỉ service dùng storage mới có khóa là quy ước + compose chỉ truyền biến cho service cần, **không** phải kiểm soát kỹ thuật; khóa bỏ qua RLS nên có quy trình xoay khóa" |
| `CLAUDE.md` | Bảng module (~25), lệnh test (~69), dòng biến môi trường (~50): thêm tên `SUPABASE_URL`, `SUPABASE_SECRET_KEY` (không giá trị) |
| `README.md` | Bảng công nghệ (~18) và cây thư mục (~35–36): thêm `common-storage`; nếu `common-events` còn thiếu ở cùng chỗ thì thêm luôn |
| `plans/261016-1030-supabase-media-storage/plan.md` | Sửa bảng "Biến cấu hình" cho khớp mục "Cập nhật 2026-10-19" (xem dưới) |

Javadoc là tài liệu chính của module (không tạo README riêng trong module, `common-security` cũng không có). Javadoc bắt buộc:
`ObjectStorage.put` ("chỉ cho file nhỏ; mp3 dài dùng `signedUploadUrl`; sau `outcomeUnknown` đừng retry cùng key"),
`ObjectKeys.requireOwnedBy` ("bắt buộc gọi, cùng `exists`, khi nhận key từ client"), `signedDownloadUrl` ("URL là bí mật, không log").

**Quy trình xoay khóa** (một đoạn ngắn trong AGENTS.md hoặc `docs/`): tạo khóa mới ở Dashboard → cập nhật `.env`/secret store → khởi động lại các service dùng storage (khóa được gắn vào `RestClient` lúc tạo bean; `refresh` của config server **không** áp lại) → thu hồi khóa cũ. Khóa legacy `service_role` đang được Supabase thay dần, nên dùng khóa `sb_secret_…`; nếu Supabase cho nhiều secret key, dùng mỗi service một khóa riêng để thu hồi từng cái.

### Plan Supabase cũ (`261016-1030-supabase-media-storage`)

Đã được cập nhật ngày 2026-10-19 khi lập plan này (mục "Cập nhật 2026-10-19" ở đầu `plan.md`, `blockedBy` trỏ về plan này,
`phase-02` bỏ adapter riêng và bỏ `CONTENT_MEDIA_UPLOAD_TTL_SECONDS`). Các sửa đó **chưa commit** lúc lập plan; phase 1 commit chúng.
Phase này chỉ làm hai việc:

1. Sau khi smoke và contract test chốt các câu hỏi mở, sửa lại mục "Cập nhật" nếu kết quả khác giả định (ví dụ có TTL thật cho signed upload, hoặc dạng xóa khác).
2. Sửa bảng "Biến cấu hình" trong `plan.md` cũ để khớp mục "Cập nhật" (bỏ dòng `CONTENT_MEDIA_UPLOAD_TTL_SECONDS` cho signed upload, thay `SUPABASE_STORAGE_BUCKET` bằng tên bucket theo từng service), vì bảng đó hiện vẫn là bản cũ.

## Related Code Files

- Create: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/contract/SupabaseStorageContractTest.java`
- Create: `plans/reports/contract-run-<timestamp>-supabase-storage-report.md` (kết quả lần chạy thật; chỉ khi đã chạy)
- Modify: `AGENTS.md`, `CLAUDE.md`, `README.md`
- Modify: `plans/261016-1030-supabase-media-storage/plan.md` (bảng "Biến cấu hình"; mục "Cập nhật" nếu cần)
- Modify (nếu kết quả buộc): `shared/common-storage/.../SupabaseObjectStorage.java` (cách `delete`/`exists`)

## Implementation Steps

1. Viết contract test với đủ 7 kịch bản; kiểm tra bằng đọc code rằng không có chỗ nào in khóa, đặt khóa vào biến môi trường, hay chạy khi bucket không bắt đầu bằng `contract-test-`.
2. Chạy `mvn -q -pl shared/common-storage test` **không** đặt biến môi trường: xác nhận contract test **skipped**, phần còn lại xanh. Ghi số test chạy và số test skipped.
3. Chạy trong bucket `contract-test-*` (nên dùng secret key tạm, thu hồi sau). Trên Windows PowerShell của máy này:
   `$env:STORAGE_CONTRACT_URL = '<url project>'; $env:STORAGE_CONTRACT_KEY_FILE = '<đường dẫn file khóa ngoài repo>'; $env:STORAGE_CONTRACT_BUCKET = 'contract-test-<tên>'; mvn -q -pl shared/common-storage test -Dtest=SupabaseStorageContractTest`
   (biến chỉ tồn tại trong phiên; khóa **không** nằm trên dòng lệnh vì nằm trong file). Nếu chưa có bucket test hoặc khóa test: ghi "contract test chưa chạy" ở báo cáo và PR, và **không** mở khóa plan service.
4. Ghi báo cáo chạy thật vào `plans/reports/` (không chứa khóa hay key thật).
5. Sửa code nếu contract test lộ khác biệt (kèm test phase 3 tương ứng).
6. Sửa tài liệu theo bảng trên và hai việc nhỏ ở plan Supabase cũ; chạy lại grep nhất quán (xem Success Criteria).
7. Commit tách: `test(storage): add an opt-in Supabase contract test`, rồi `docs(storage): document the common-storage module`.

## Success Criteria

- [ ] Contract test: skip sạch khi thiếu biến hoặc tên bucket sai (có số test skipped). Nếu đã chạy thật, cả 7 kịch bản xanh và báo cáo lưu ở `plans/reports/`. Nếu chưa chạy, câu "chưa chạy" xuất hiện ở báo cáo cuối và plan service chưa được mở khóa.
- [ ] Các câu hỏi mở (dạng khóa, thời hạn signed upload, dạng xóa, endpoint `exists`) đã được trả lời hoặc ghi rõ "chưa xác minh được" trong báo cáo.
- [ ] `mvn -B -ntp verify` xanh toàn reactor; `docker compose config --quiet` qua.
- [ ] Không có khóa thật trong thay đổi của nhánh: `git diff feat/common-events... | grep -nE "sb_secret_[A-Za-z0-9_-]{20,}|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}"` rỗng. Khóa giả của test (`sb_secret_test-not-real`, dưới 20 ký tự sau tiền tố) cố ý không khớp mẫu này.
- [ ] AGENTS.md/CLAUDE.md/README khớp với `pom.xml` (số module, tên module, lệnh test), và câu §3.4 mới không khẳng định một kiểm soát kỹ thuật không tồn tại.
- [ ] Bảng "Biến cấu hình" của plan Supabase cũ không còn dòng TTL cho signed upload hay biến bucket duy nhất, khớp mục "Cập nhật 2026-10-19".


## Risk Assessment

| Rủi ro | Mitigation |
| --- | --- |
| Contract test để lại object rác hoặc chạm bucket thật | Allow-list tiền tố `contract-test-`, key dưới `contract-test/{uuid}/`, xóa trong `finally`; dọn dẹp chỉ trong bucket đã qua allow-list |
| Contract test chạy nhầm vì môi trường đã có `SUPABASE_*` | Dùng bộ biến riêng `STORAGE_CONTRACT_*`; không dùng lại tên biến production |
| Khóa lọt vào lịch sử shell/CI log | Khóa đọc từ file ngoài repo; test không in khóa; CI mặc định không có biến nên skip |
| Test ngủ 3 giây làm chậm | Chỉ chạy khi opt-in, nên không ảnh hưởng build thường |
| Kết quả thật làm đổi API (ví dụ dạng xóa) | Đã khoanh vùng một chỗ sửa và test tương ứng; tài liệu cập nhật sau cùng |
| Dùng chung project với ứng dụng: khóa chính bỏ qua RLS nên một lỗi trong test có thể chạm dữ liệu thật | Allow-list bucket trong code; không gọi bucket khác; ưu tiên secret key tạm rồi thu hồi; người dùng xóa bucket test sau khi xong |
| Chưa có bucket test hoặc khóa test | Cổng giữ nguyên, plan service chưa mở khóa; phase 1–3 vẫn hợp nhất được vào nhánh tích hợp |
| Rollback | Xóa file contract test; hoàn tác commit tài liệu; module vẫn dùng được (phase 3) |
