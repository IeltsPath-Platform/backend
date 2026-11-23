# Phase 03: Trình xem PDF slide cho học viên

Plan: [plan.md](plan.md) · Trạng thái: done · Phụ thuộc: phase 01

## Context

- `frontend/src/features/learning-path/components/resources/LessonSlides.tsx` (viết lại)
- `frontend/src/features/learning-path/pages/LessonPage.tsx` dòng ~124: `useApiResource('lesson-resources:…')`, nơi gọi `LessonSlides`
- `frontend/src/types/lesson.ts` (`LessonSlideItem.url` giờ là URL ký 15 phút)
- Style hiện có: `lp-res-*` trong `learning-path.css`
- `frontend/AGENTS.md` §3, §6: dependency phải ghim phiên bản; `package.json` và lockfile đổi nhất quán. **Thêm `react-pdf` đã được người dùng duyệt ngày 2026-11-23** (nêu trong mô tả PR).

## Bước 1: kiểm tra khả thi (làm trước, ~30 phút)

1. Lấy URL ký thật của một PDF trong `media-premium` (admin upload thử qua phase 01) và mở từ origin web đang chạy: `fetch(url)` có bị CORS chặn không? Có hỗ trợ `Range` không? Kết quả ghi vào cuối file này.
2. `npm view react-pdf versions` và `npm view react-pdf peerDependencies` để chọn phiên bản chạy với React 19.3. Ghim đúng phiên bản (không `^`, không `latest`), theo kiểu các dependency hiện có trong `package.json`.
3. Nếu CORS chặn: dừng, báo người dùng. Hai lối ra là cấu hình CORS bucket Supabase (thao tác ngoài code, cần người dùng) hoặc để Content proxy tệp (đổi kiến trúc, cần duyệt riêng).

## Yêu cầu

- Mỗi slide là một PDF; nhiều slide giữ tab chuyển như hiện nay (`lp-res-tabs`).
- Mỗi lần chỉ render **một trang** (canvas); nút Trước/Sau, ô "trang n / tổng", phím ←/→ khi khung được focus.
- Vừa khít chiều rộng khung, phóng to/thu nhỏ, nút "Mở rộng" (toàn màn hình, giữ cách `requestFullscreen` hiện tại), nút "Tải về" (`href` là URL ký, `target=_blank`, `rel=noopener noreferrer`).
- Trạng thái chờ, lỗi (nút "Tải lại"), rỗng ("Bài học này chưa có slide.") theo `AGENTS.md`; a11y: nút có `aria-label`, trang hiện có mô tả.
- Gỡ `getGoogleSlidesLinks`, `GoogleSlidesLinks`, nhánh `fallback` "Slide này không nhúng trực tiếp được" và `MonitorPlay`.

## Thiết kế

- Component mới `SlidePdfViewer.tsx` cạnh `LessonSlides.tsx` (đã đủ lớn để tách): nhận `url`, `title`; tự giữ `pageNumber`, `numPages`, `scale`; bọc `Document`/`Page` của `react-pdf`.
- Worker pdf.js: cấu hình `pdfjs.GlobalWorkerOptions.workerSrc` bằng `new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url).toString()` (cách Vite bundle worker); đặt một lần ở module viewer. Xác nhận bằng `npm run build` rằng worker được emit.
- Lazy load cả viewer (`React.lazy`) để pdf.js không vào bundle đầu trang học; chỉ tải khi bài có slide. Hiện `LessonSlides` đã được render theo điều kiện, nên chỉ bọc `Suspense` với trạng thái chờ có sẵn.
- **URL ký hết hạn:** tải trọn tệp một lần khi mở (`disableRange`, `disableStream`) để lật trang sau 15 phút vẫn chạy từ bộ nhớ; "Tải về" và "Tải lại" dùng URL mới bằng cách gọi lại `useApiResource.reload` của `LessonPage` (truyền `onExpired` xuống). Deck 20 MB tải trọn là chi phí chấp nhận được, ghi nhận ở rủi ro.
- CSS: thêm vào `learning-path.css` theo token hiện có, không mã màu cứng.

## Test

- Mock `react-pdf` ở test (jsdom không render canvas): kiểm tab chuyển slide, nút Trước/Sau cập nhật trang, nút "Mở rộng" gọi `requestFullscreen`, "Tải về" trỏ URL ký, lỗi tải hiện nút "Tải lại" và gọi `onExpired`, rỗng hiện thông báo.
- Sửa `LessonPage.summary.test.tsx`, `LessonPage.complete.test.tsx`, `LessonPage.vocabulary.test.tsx` nếu dữ liệu mẫu slide dùng link Google.
- Chạy `npx vitest run src/features/learning-path`, `npm run lint`, `npm run typecheck`, `npm run build`.
- Thử tay trên trình duyệt thật: một PDF nhỏ, một PDF ~20 MB, iPhone Safari nếu có máy; để mở 16 phút rồi lật trang.

## Rủi ro

- CORS (bước 1) có thể chặn cả phase.
- pdf.js làm nặng bundle: lazy load để chỉ trả giá khi có slide.
- Tải trọn tệp trên mạng yếu chậm: hiện tiến trình tải; không tối ưu thêm (YAGNI).
- Rollback: gỡ `react-pdf` khỏi `package.json` và lockfile, khôi phục `LessonSlides.tsx` từ nhánh cũ; vẫn phải xử lý slide đã là PDF nên rollback thật sự cần cả phase 01.

## Kết quả kiểm tra khả thi

Chạy 2026-11-23 bằng `curl` thẳng vào storage API của dự án, không dùng khóa, không ghi gì vào bucket. Origin thử: `http://localhost:5173` và một domain lạ.

| Thử | Kết quả |
|---|---|
| GET object public kèm `Origin` | 200, `Access-Control-Allow-Origin: *`, `Accept-Ranges: bytes` |
| GET kèm `Range: bytes=0-99` | 206, `Content-Range` đúng, vẫn có `Access-Control-Allow-Origin: *` |
| GET route `object/sign/...` token sai | 400 nhưng vẫn có `Access-Control-Allow-Origin: *` (CORS gắn toàn cục, kể cả lỗi) |
| Preflight `OPTIONS` route `sign` (`Access-Control-Request-Headers: range`) | 200, cho phép GET/HEAD, header `range`, cache 1 giờ |
| Origin lạ | Vẫn `*`: không cần whitelist origin |

Kết luận: **CORS không chặn** `fetch` từ web tới Supabase, kể cả với route URL ký. Không cần cấu hình gì ở bucket.

Chưa kiểm được (cần tệp PDF thật trong `media-premium`, sẽ làm khi phase 01 cho upload được):
- Header của một URL ký thật (`Content-Type: application/pdf`, không có `Content-Disposition: attachment` làm hỏng việc tải bằng `fetch`; pdf.js dùng `fetch` nên cả hai đều không ảnh hưởng, nhưng nên xác nhận).
- Phản hồi không có `Access-Control-Expose-Headers`, nên cross-origin pdf.js không đọc được `Accept-Ranges`/`Content-Range` và tự tải trọn tệp. Điều này khớp với quyết định `disableRange` + `disableStream` ở trên, nên không phải rủi ro.
- Hành vi sau 15 phút (URL ký hết hạn) với tệp đã tải trọn: lật trang phải chạy từ bộ nhớ.
