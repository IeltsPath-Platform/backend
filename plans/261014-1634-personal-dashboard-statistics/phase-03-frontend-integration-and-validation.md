---
phase: 3
title: "Frontend integration and validation"
status: pending
effort: ""
dependencies: [1, 2]
---

# Phase 3: Ghép FE, bỏ Mentor và nghiệm thu

## Overview

Nguồn chuẩn: [thiết kế §5–6](./dashboard-statistics-design.md). Chỉ tab dữ liệu chi tiết và những luồng ghi cần thiết để có dữ liệu thật. Chưa đồng bộ toàn bộ dashboard tổng quan, shell navigation hoặc lớp học.

## Requirements

- Bốn nhóm UI có dữ liệu thật/empty/unavailable rõ ràng; thẻ từ vựng hiển thị chưa khả dụng; không fallback mock production.
- Bỏ Mentor theo quyết định người dùng, không thay bằng tính năng mentor AI.
- FE render/chart/filter, BE quyết định số liệu. Tái dùng HTTP auth và các dependency hiện có.

## Related Code Files

Base FE: `D:/Fall2026/New folder/FE/frontend`.

- Sửa `src/features/overview/pages/OverviewPage.tsx`, `src/features/overview/components/OverviewDetailedStats.tsx`, `src/features/overview/overview.css`, `src/types/overview.ts`.
- Tạo `src/features/overview/api/overviewApi.ts`, `src/features/overview/hooks/useDetailedStatistics.ts`.
- Tách card `StudyTimeCard.tsx`, `SkillCompletionCard.tsx`, `BandHistoryCard.tsx`, `ActiveCoursesCard.tsx` dưới components nếu phù hợp độ dài thực; không tạo abstraction chart framework.
- Bổ sung invalidation sau mutation thật ở `src/features/learning-path/pages/`; tránh thay toán chấm/khóa bài.
- Sửa README mục Dashboard khi behavior thực sự thay đổi; plan này không sửa README hôm nay.
- `src/mocks/overviewData.ts` vẫn có thể được tab tổng quan sử dụng; chỉ bỏ import của tab chi tiết, không xóa cả file làm hỏng phần khác.

## Architecture

Hook gửi 3 request độc lập, lưu state theo resource/query key. Dùng Promise.allSettled hoặc các resource riêng để partial failure không làm trống cả màn hình. Không kết hợp tổng dữ liệu của 2 user/range khi response về muộn. Không cần query library mới; tái sử dụng convention `useApiResource` nếu phù hợp và không làm hỏng các caller cũ.

## Implementation Steps

1. Khóa TypeScript DTO từ contract BE, nullable và availability là tường minh; không `any`, không ép null thành 0.
2. Nối API/time filters/selected course/skill tabs; date range sử dụng quy ước ngày server. Course filter không đưa full daily target vào chart course.
3. Thay SVG tọa độ cứng bằng dữ liệu, có trục/đơn vị/tooltip/table; nếu skill null, render marker chưa có nội dung hoặc bảng thay radar chứ không fake điểm 0.
4. Xóa Mentor card/CSS riêng không còn dùng, chỉnh layout vocabulary; xóa AI nhận xét và dự báo tốc độ không có cơ sở ở tab chi tiết. Đổi caption skill thành “Bài học hoàn thành”.
5. Course dùng đơn vị bài, counts thật, resumeTarget; bộ chọn và view-all có pagination. Band theo series, placement có nhãn ước lượng, pending/empty hữu ích.
6. Thẻ từ vựng: hiển thị chưa khả dụng, không dữ liệu mẫu, không điều hướng sang mock. Luồng ôn từ vựng làm ở plan riêng.
7. Nối refetch sau mutation/return-to-dashboard, abort khi unmount/range change, xóa state logout. Hook time không bật trong mock-learning hoặc mock standalone practice.
8. Deep link query cho tab/filter nếu áp dụng; kiểm back/forward, keyboard, reduced motion, screen reader. Đồng bộ menu shell đầy đủ là việc khác, không tự kéo vào scope.
9. Chạy BE checks đúng module, FE lint/typecheck/build và kịch bản end-to-end ở §6.2; ghi rõ Docker skip hoặc môi trường không phù hợp.
10. Cập nhật contract/documentation tại vị trí team thống nhất; hiện BE docs/ thiếu nên phải chọn/khôi phục đường dẫn hợp lệ khi triển khai, không tạo link giả đến tài liệu không tồn tại.

## Success Criteria

- [ ] Time week/month/course thực sự đổi dataset; 0/unknown/future khác nhau.
- [ ] Radar và thanh skill cùng counts từ BE; không còn caption mastery/AI sai nghĩa.
- [ ] Không còn Mentor hoặc AI Mentor trong tab chi tiết.
- [ ] Band delta/nguồn/range đúng; target là mục tiêu, không lời hứa dự báo.
- [ ] Thẻ từ vựng không còn số mẫu, hiển thị chưa khả dụng.
- [ ] Course đang học lesson đầu hiển thị dù chưa pass topic.
- [ ] Một service lỗi chỉ lỗi card của service đó; retry không kéo theo 2 nhóm còn lại.
- [ ] Session/logout/filter race không rò dữ liệu account/range trước.
- [ ] Bàn phím, mobile, tooltip và bảng số accessible.

## Validation

Tại FE: `npm run lint`, `npm run typecheck`, `npm run build` với Node/npm theo manifest. Không có `npm test` hiện tại; không ghi đã chạy. Nếu muốn automated browser/fake-clock suite, thống nhất dependency test riêng. Manual evidence phải nêu kịch bản, expected/actual, môi trường và lỗi còn lại, không gọi manual là unit test.

Tại BE: mở rộng `mvn -B -ntp verify` khi public contract across services đã được tích hợp và môi trường Docker đáp ứng. Không chạy build/CI trong lần lập tài liệu này.

## Risks and rollback

BE chưa phát hành API: unavailable thay vì mock. Có thể ship telemetry trước rồi bật card theo khả năng API, nhưng chỉ mark phase done khi có dữ liệu/luồng đầy đủ. Rollback FE không xóa telemetry đã thu; giữ compatibility API cũ và quyền truy cập như trước.
