# Brief Codex: phase 7, audio PREMIUM qua URL ký

Người làm: **Codex**. Claude review. Không ghi Supabase, không đọc `.env`.
**Đã chốt (2026-10-31):** D1 = (A), chuyển `listening/demo/ls1.mp3` sang `media-premium`, bài 1 cũng phát bằng URL ký. D2 = tên bucket `media-premium`. Hai quyết định dưới đây giữ lại để tham chiếu.

## Quyết định cần bạn (chặn việc này)

**D1. Bài trả phí dùng chung file với bài miễn phí.**
Kiểm trên DB tạm (89 migration):
- Quy tắc "sau 3 bài miễn phí" của Listening 5.5 chỉ có **một** bài trả phí dùng audio: bài vị trí 4, dùng `listening/demo/ls1.mp3`.
- `listening/demo/ls1.mp3` cũng là audio của **bài vị trí 1 (miễn phí)**.
- Nếu giữ file này công khai trong `media`, ai biết URL vẫn nghe được bài 4. Paywall không bảo vệ được bài 4.

Lựa chọn:
- **(A) Khuyến nghị:** chuyển `listening/demo/ls1.mp3` sang `media-premium` và đánh dấu tài sản là PREMIUM. Bài 1 (miễn phí) cũng phát bằng URL ký, vì URL ký không phụ thuộc paywall. Xóa bản công khai sau khi kiểm. Không cần thu âm mới.
- **(B)** Bạn thu một bản audio riêng cho bài 4 (khóa mới trong `media-premium`). Bài 1 giữ nguyên công khai. Cần thêm một file thu âm.

**D2. Tên bucket:** `media-premium` như plan. Đổi tên thì báo trước khi làm.

Quyết định đã chốt bằng ràng buộc kỹ thuật (không cần bạn): **TTL URL ký = 900 giây**, vì `common-storage` giới hạn `MAX_TTL` = 15 phút.

## Điều kiện bắt đầu

1. D1 và D2 đã trả lời.
2. Phase 3 đã merge (cùng sửa `ContentMediaController`) và phase 4 đã merge (V90 đã có, V92 đứng sau).
3. Claude đã kiểm kê endpoint phát `mediaUrl` (bảng dưới) và xác nhận không có đường công khai nào phát audio PREMIUM.

## Kiểm kê đã có (từ mã nguồn hiện tại)

Đường học viên nhận audio:
- `learning-service`: `GET /api/learning/lessons/{id}` (`LessonLearningController`), qua `LessonViewAssembler` và `LessonResponse`. Đây là đường **duy nhất** cần ký URL cho bài trả phí. Có cổng paywall (`PaywallGate`/`LessonAccess`).
- `content-service` internal: `/internal/learning-content/...` (`LessonContentResponse`, `PackageVersionContentResponse`) được learning gọi. Hiện trả `mediaUrl` công khai qua `GetLessonContentUseCase`, `GetPackageVersionContentUseCase`.
- `assessment-service`: `ContentPackageClient` và `AttemptStructureResponse` nhận `mediaUrl` từ content, trả cho học viên ở đề thử. Đề thử luôn PUBLIC theo quyết định, nên **không** nhận URL ký.
- Admin/author: `GetAuthorPackageUseCase`, `GetPackageVersionPreviewUseCase`, `SectionMediaUseCases`. Giữ URL công khai (chỉ tác giả thấy).

Gateway: `/api/learning/**` và `/api/assessments/**` được định tuyến công khai. `/internal/**` không đi qua gateway.

## Hợp đồng

- Cột mới `content_assets.access_tier VARCHAR(10) NOT NULL DEFAULT 'PUBLIC' CHECK (access_tier IN ('PUBLIC','PREMIUM'))`. Migration `V92__content_asset_access_tier.sql`.
- Tài sản PREMIUM (sau D1): mọi lần phát đều dùng URL ký, **kể cả** bài miễn phí dùng cùng tài sản.
- `content-service` internal nhận cờ `signedAudio=true` **chỉ** từ learning, và chỉ cho lesson. Mặc định `false`, giữ hành vi cũ.
- Khi `signedAudio=true` và tài sản PREMIUM: `ObjectStorage.signedDownloadUrl("media-premium", key, Duration.ofSeconds(900))`.
- Khi tài sản PUBLIC: giữ `MediaReferencePolicy.resolve` như cũ.
- Learning phải chạy `PaywallGate` **trước** khi gọi content để lấy audio. Bài bị chặn thì không gọi content, không tạo URL ký.
- Lỗi ký URL (bucket không có, cấu hình thiếu): trả lỗi 503 cho bài đó, không làm hỏng toàn bộ bài học.

## Nhánh và PR

- `feat/storage-p7-premium-audio` từ `feat/supabase-storage-reorganization`, PR vào đó.

## Việc cần làm (TDD: test trước)

Content-service:
- `MediaUrlResolver` (application): PUBLIC → `MediaReferencePolicy`; PREMIUM → signed; URL `https://` giữ nguyên.
  - Test: PUBLIC cho URL công khai; PREMIUM cho URL dạng `/object/sign/` có token; TTL đúng 900; tham chiếu hỏng → lỗi cũ.
- Sửa `GetLessonContentUseCase`, `GetPackageVersionContentUseCase` để nhận cờ và dùng resolver.
- `ContentMediaController` (upload audio): thêm tham số `tier` (mặc định `PUBLIC`); `PREMIUM` lưu vào `media-premium`.
  - Test: học viên không phải tác giả nhận 403; tác giả upload PREMIUM vào bucket đúng.
- Cấu hình: `CONTENT_PREMIUM_MEDIA_BUCKET` (mặc định `media-premium`) và `content.media.premium-url-ttl-seconds: 900` trong `infra/config-server/config-repo/content-service.yaml`; `docker-compose.yml` khối `content-service`.

Learning-service:
- Trong luồng lấy bài (`GetLessonUseCase` hoặc tương đương, tìm bằng `grep -rn "getLessonContent\|LessonContentClient" services/learning-service/src/main`): gọi `PaywallGate` trước, rồi gọi content với `signedAudio=true`.
  - Test: bài bị chặn không gọi content (kiểm bằng mock/verify); bài mở được nhận URL ký; lỗi ký trả 503.
- Các test `PracticeGatedLessonUnlockIntegrationTest` và test paywall hiện có phải vẫn xanh.

Assessment-service:
- Không đổi. Test hồi quy: đề thử vẫn nhận URL công khai, không có URL ký.

Frontend (repo `frontend/`):
- Trình phát audio bài học: nếu URL hết hạn (lỗi tải), gọi lại `GET /api/learning/lessons/{id}` một lần rồi phát tiếp.
- Test: phát được; hết hạn thì tải lại đúng một lần.

Migration tác giả (dữ liệu, không phải code): sau khi Claude sao chép file sang `media-premium` và kiểm URL ký, đặt `access_tier = 'PREMIUM'` cho đúng tài sản đã chốt ở D1. Đây là migration `V93` (dữ liệu, idempotent), **chỉ** merge sau khi file đã có trên bucket.

## Không làm

- Không tạo bucket. Không chuyển file. (Claude làm, có xác nhận từng bước.)
- Không đổi TTL thành 3600 hay khác 900.
- Không ký URL cho đề thử, tài liệu, hay đường tác giả.
- Không cache URL ký phía server lâu hơn TTL.
- Không ghi URL ký vào log hay DB.

## Lệnh kiểm chứng

```
mvn -B -ntp -pl services/content-service -am test
mvn -B -ntp -pl services/learning-service -am test
mvn -B -ntp -pl services/assessment-service -am test
```
Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run src/features/learning-path`.

## Tiêu chí xong

- Bài 4 (trả phí) không phát được qua URL công khai; phát được qua URL ký khi đã mở khóa; bị chặn khi chưa mở khóa (kiểm bằng Claude trên dev).
- Đề thử và placement vẫn phát bằng URL công khai.
- Test ba service xanh.

## Dừng và hỏi (`NEEDS_CONTEXT`)

- D1 hoặc D2 chưa có quyết định.
- `LessonLearningController` có đường lấy bài khác ngoài `GET /api/learning/lessons/{id}` trả audio: báo danh sách.
- Test paywall cũ mâu thuẫn với thứ tự gọi mới: báo, không xóa test.
