---
phase: 6
title: "Frontend luồng nộp, kết quả và quản trị bộ tiêu chí"
status: in-progress
priority: P2
effort: "3.5d"
dependencies: [2, 3, 5]
---

# Phase 6: Frontend luồng nộp, kết quả và quản trị bộ tiêu chí

## Overview

Trang Speaking luyện tập hiện chỉ ghi âm để nghe lại ở trình duyệt. Phase này thêm: giữ ghi âm đến lúc Nộp, hộp thoại nộp (chi phí, đồng ý, upload có tiến độ), trang kết quả có polling, và mở rộng trang quản trị rubric (`RubricsPage`, hiện chỉ cho Writing) để người quản trị nhập và sửa bộ tiêu chí Speaking (D16). Repo: `frontend`, nhánh riêng cho phase này.

## Requirements

- Functional: ghi âm từng câu giữ trong trình duyệt; nút Nộp chỉ bật khi đủ ghi âm của phạm vi (cả đề: 3 part; part lẻ: part đó); nộp, theo dõi trạng thái, xem kết quả; gọi đúng hợp đồng JSON của phase 3 và 5.
- Non-functional: không hiện lỗi thô; truy cập bàn phím; cảnh báo khi rời trang lúc đang có ghi âm chưa nộp; nhãn rõ "điểm ước lượng".

## Architecture

**Điểm cần chú ý:** các part là route riêng (`/practice/speaking/tasks/:packageVersionId/:sectionId?parts=...`), nên ghi âm phải sống qua việc chuyển part. Dùng một store phía module hoặc provider bao ngoài route (blob không lưu được ở `sessionStorage`; IndexedDB là câu hỏi mở số 7). Mất khi F5, nên bắt `beforeunload` khi còn ghi âm.

Luồng nộp: (1) màn bắt đầu hiện giá điểm và số dư hiện có như cảnh báo (không chặn); (2) Nộp mở hộp thoại: giá điểm, ô đồng ý (văn bản và phiên bản từ `GET .../terms`); (3) upload tuần tự từng file với thanh tiến độ, thử lại riêng file lỗi; nếu server báo thiếu điểm ở file đầu thì dừng, ghi âm vẫn còn, hiện liên kết nạp điểm; (4) `POST .../submissions` với `requestId` sinh phía client (giữ nguyên khi thử lại); (5) chuyển sang trang kết quả.

Trang kết quả (`/practice/speaking/submissions/:id`): polling 3–5 giây có giãn dần; các trạng thái: đang chờ, đang phân tích, đang chấm, **chờ thanh toán** (CTA nạp điểm, nộp lại cùng `requestId`), **hoàn tất** (4 tiêu chí kèm band và nhận xét, transcript đánh dấu các đoạn dẫn chứng và từ phát âm yếu, danh sách tiêu chí cần luyện kèm liên kết luyện tập, band tổng nếu là cả đề), **lỗi**/chuyển giám khảo (thông báo "chưa chấm được, bạn không bị trừ điểm").

Ghi âm WAV 16 kHz mono (D11): mở rộng `useAudioRecorder` (hiện dùng `MediaRecorder` mặc định) hoặc thêm bộ ghi PCM; giữ chức năng nghe lại. Phase 1 xác nhận hướng này.

## Related Code Files

- Modify: `src/features/practice/hooks/useAudioRecorder.ts`, `src/features/practice/pages/SpeakingPage.tsx`, `src/features/practice/speaking/SpeakingRecorder.tsx`, `src/features/practice/speaking/SpeakingWorkspacePage.tsx`
- Create: `src/features/practice/speaking/speakingSubmitApi.ts` (zod schema theo hợp đồng, upload, nộp, polling), `speakingSession.ts` (store ghi âm qua các part), `SpeakingSubmitDialog.tsx`, `SpeakingResultPage.tsx`, `speaking-result.css`
- Modify: file định tuyến của app (đăng ký route kết quả; tìm nơi đăng ký `/practice/speaking/tasks`)
- Modify (quản trị rubric): `src/features/admin/rubrics/pages/RubricsPage.tsx` và các component, lớp API, kiểu dữ liệu cùng thư mục: chọn loại Writing hoặc Speaking; với Speaking hiện các tiêu chí FC, LR, GRA, PR, mô tả theo band, ô `system_prompt` có giới hạn độ dài, trạng thái nháp, ACTIVE, RETIRED, tạo phiên bản mới từ bản cũ; ghi chú chỉ đọc cho biết định dạng JSON và luật chống prompt injection do hệ thống tự nối thêm
- Modify: `src/features/admin/adminMenu.ts` (nhãn "Rubric chấm Writing" thành "Rubric chấm Writing và Speaking") và test menu liên quan
- Create (test): vitest cho `speakingSubmitApi`, store, hộp thoại (thiếu điểm, thử lại, đồng ý), trang kết quả (mọi trạng thái), và trang rubric cho Speaking (hiện đúng tiêu chí, chặn phát hành khi thiếu FC, LR hoặc GRA, bản ACTIVE chỉ đọc)

## Implementation Steps

1. Đọc `SpeakingPage`, `useAudioRecorder` và route hiện có; chốt cách giữ ghi âm qua các part.
2. Test trước cho store và `speakingSubmitApi` (schema, `requestId` giữ nguyên khi thử lại, mã lỗi 402 và 409).
3. Bộ ghi WAV và giữ ghi âm; cảnh báo rời trang.
4. Hộp thoại nộp và luồng upload; xử lý thiếu điểm.
5. Trang kết quả và polling; trạng thái từng loại; liên kết luyện tập từ tiêu chí cần luyện.
6. Đọc `RubricsPage` hiện có và API rubric, rồi mở rộng cho Speaking theo hợp đồng của phase 2; kiểm tra Writing không đổi hành vi.
7. Chạy `tsc`, `eslint`, vitest toàn `features/practice` và `features/admin/rubrics`.

## Success Criteria

- [ ] Nộp part lẻ và cả đề đều chạy với backend giả; chỉ bật Nộp khi đủ ghi âm.
- [ ] Thiếu điểm: dừng ngay ở file đầu, ghi âm còn nguyên, có đường nạp điểm, nộp lại không phải thu lại.
- [ ] `requestId` giữ nguyên khi thử lại; nhấn đúp không tạo hai bài.
- [ ] Trang kết quả hiển thị đúng từng trạng thái, có nhãn "điểm ước lượng".
- [ ] Người có quyền nhập, sửa nháp, phát hành và tạo phiên bản mới bộ tiêu chí Speaking trên giao diện; Writing vẫn dùng được như cũ.
- [ ] `tsc`, `eslint` sạch, test vitest của `features/practice` và `features/admin/rubrics` xanh.

## Risk Assessment

- Bộ ghi WAV phụ thuộc trình duyệt và độ lớn file (5 MB ≈ 2,5 phút ở 16 kHz mono 16-bit): Part 2 sát giới hạn, cần thông báo rõ khi gần chạm trần và có phương án nén hoặc nâng giới hạn ở backend.
- Mất ghi âm khi F5: chấp nhận ở v1 có cảnh báo; IndexedDB là bước sau.
- Polling lâu khi Azure chậm: hiện thông điệp "đang chấm, có thể mất tới vài phút" và cho phép rời trang quay lại sau.
