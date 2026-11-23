---
title: "Slide bài học: upload PDF lên Supabase, bỏ link Google Slides"
description: "Slide của bài chỉ còn là tệp PDF tải lên bucket riêng, học viên xem bằng pdf.js (react-pdf); xóa mọi slide dạng link"
status: done
priority: P2
branch: "feat/lesson-slide-pdf-upload (backend + frontend)"
tags: [content-service, lesson-resources, storage, frontend, pdf]
blockedBy: []
blocks: []
created: "2026-11-23T21:34:00.000+07:00"
createdBy: "Claude Code"
source: "hội thoại 2026-11-23: admin dán link Google Slides dễ lỗi quyền chia sẻ; tài liệu đã upload PDF được nên làm tương tự cho slide"
---

# Slide bài học = PDF tải lên

Repo: `backend` (content-service) và `frontend`. Không đụng `learning-service`, `mobile`.

## Quyết định đã chốt (2026-11-23)

| # | Quyết định | Hệ quả |
|---|---|---|
| 1 | Slide **chỉ** là PDF, không còn link | Form admin bỏ ô URL của slide; domain và DB chặn slide dạng link |
| 2 | Xóa mọi slide link bằng migration | Mất 7 dòng seed demo (V57, V60) và slide link admin đã tạo; không khôi phục được. Người dùng đã duyệt |
| 3 | Học viên xem bằng `react-pdf` (pdf.js) | Thêm dependency, đã duyệt; ghim phiên bản chính xác |
| 4 | Giữ giới hạn 20 MB | Dùng nguyên endpoint upload tài liệu, không tham số mới |

## Hiện trạng đã kiểm

- Tài liệu đã có luồng tải PDF: `POST /api/content/admin/media/document` → `fileKey` trong bucket `media-premium` → `POST .../resources` với `fileKey`. Content ký URL 15 phút ở lần đọc nội bộ mà Learning gọi sau khi kiểm quyền vào bài (`lesson-authoring-v1.md` mục "Documents uploaded as files"). Slide thừa hưởng nguyên đường này.
- Chỗ chặn slide có tệp: `LessonResource` constructor, `CreateLessonResourceUseCase.newFileResource`, và `ck_lesson_resources_target` ở V114.
- `DeleteLessonResourceUseCase`, reorder và update không phụ thuộc loại tài nguyên: dọn tệp khi xóa slide chạy sẵn.
- Học viên: `LessonSlides.tsx` chỉ nhúng được `docs.google.com`; `LessonSlideItem.url` giờ sẽ là URL ký của PDF.
- Migration cao nhất hiện là V147, nên migration mới là V148.

## Phases

| Phase | Nội dung | Trạng thái |
|---|---|---|
| [01](phase-01-backend-slide-file.md) | Domain, migration xóa slide link, contract, test backend | done |
| [02](phase-02-admin-slide-form.md) | Form admin: slide chọn tệp PDF | done |
| [03](phase-03-learner-pdf-viewer.md) | Trình xem PDF cho học viên, gỡ code Google Slides | done |

Thứ tự: 01 → 02 và 03 (song song được sau 01). Triển khai **01 và 03 cùng lúc**: nếu phase 01 lên trước 03, học viên thấy khung slide trống vì code cũ không mở được URL ký của PDF.

## Tiêu chí hoàn thành

- Admin thêm slide bằng PDF ≤ 20 MB; vượt giới hạn hoặc không phải PDF thì báo lỗi đỏ dưới ô, tiếng Việt.
- Không còn dòng `SLIDE` nào có `url`; DB từ chối chèn mới.
- Học viên mở bài: thấy slide, lật trang, phóng to, toàn màn hình, tải về; nhiều slide vẫn chuyển được bằng tab.
- Xóa slide thì tệp trong bucket cũng bị xóa (đã có sẵn, kiểm lại bằng test).
- `mvn -q -pl services/content-service -am test`, `npm run lint`, `npm run typecheck`, `npm run build` xanh (hoặc nêu rõ lệnh không chạy được).

## Rủi ro chính

1. ~~**CORS của Supabase.**~~ Đã kiểm 2026-11-23: storage trả `Access-Control-Allow-Origin: *` cả ở route `sign`, kể cả khi lỗi, và preflight `Range` được cho phép. Không chặn. Còn lại cần xác nhận bằng một PDF ký thật sau phase 01 (header `Content-Type`); chi tiết ở phase 03.
2. **URL ký hết hạn sau 15 phút.** pdf.js đọc theo đoạn (range), trang để mở lâu rồi lật tiếp có thể 403. Xem phase 03.
3. **Mất dữ liệu có chủ ý** ở quyết định 2: đã duyệt, ghi trong migration và báo trong mô tả PR.
4. **Deck lớn (100+ trang, 20 MB)** làm nặng trình duyệt: chỉ render trang hiện tại.

## Câu hỏi còn mở

- Đã chọn `react-pdf@11.1.0`: `npm view` xác nhận peer `react`/`react-dom` `^19.0.0`; build Vite 8.3.0 emit worker thành công.
- Có cần admin xem trước slide ngay trong form không? Plan này **không** làm (đã có link "Mở tệp PDF" ở hàng slide, giống tài liệu).
- Đổi tệp của slide: giữ cách tài liệu, tức xóa rồi thêm mới (giữ nguyên hành vi, chưa hỏi lại).
