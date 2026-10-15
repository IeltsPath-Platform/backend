---
phase: 3
title: "Leaderboard tab on the course page"
status: pending
dependencies: [2]
---

# Pha 3: Tab bảng xếp hạng (frontend)

Frontend root: `../../../frontend/`. Đường dẫn dưới tính từ `frontend/src/`.

## Dữ kiện đã xác minh

- `features/learning-path/pages/TopicListPage.tsx` (347 dòng): `LearnTrail` → `CourseHero` → `TopicCatalog` (+ `CourseFinalTestStop`); đọc `?topic=` qua `useSearchParams` để mở chặng.
- API qua `features/learning-path/api/{learningApi,httpLearningApi,learningAdapters}.ts`; mock ở `api/mock/`. Tải dữ liệu bằng `lib/useApiResource.ts`; trạng thái bằng `components/PageState.tsx`.
- Segmented control có sẵn: class `lp-segment` (dùng ở `CourseListPage.tsx`) → tái dùng cho chọn bảng.
- Test: `tests/learning-path.test.mjs` (node --test + Vite ssrLoadModule).

## Thay đổi

**Sửa**
- `types/learningPath.ts`: `LeaderboardBoard = 'LEARNING' | 'TOPIC_TEST' | 'FINAL_TEST'`, `Leaderboard`, `LeaderboardEntry`, `LeaderboardMe`.
- `api/learningApi.ts` + `httpLearningApi.ts` + `learningAdapters.ts`: `getCourseLeaderboard(courseId, board, topicId?)`; adapter giữ `null` (không đổi thành 0).
- `api/mock/…`: trả bảng rỗng + `me` từ dữ liệu mock thật (không tạo người giả).
- `pages/TopicListPage.tsx`: tab `Lộ trình | Bảng xếp hạng` dưới `CourseHero`. Query `?view=ranking&board=topic-test&rankingTopic=<id>` — cập nhật bằng `setSearchParams` giữ nguyên `topic=`. Lộ trình giữ mounted (`hidden`) để không mất chặng đang mở.

**Tạo**
- `features/learning-path/components/CourseLeaderboard.tsx`: `lp-segment` chọn Học tập / Thi chặng / Thi cuối khóa; select topic khi Thi chặng (chỉ topic có bài thi); bảng top 10 + "Vị trí của bạn" khi tôi ngoài top; badge "Bạn" (không chỉ dùng màu).
- Style trong `learning-path.css` theo token hiện có.

## Hiển thị

| Trường hợp | Hiển thị |
| --- | --- |
| Học tập | `180 điểm · 2 bài`; chú thích "Tổng điểm các bài đã hoàn thành, mỗi bài tối đa 100" |
| Thi | `85/100` |
| Loading / lỗi | `CourseCatalogSkeleton`-style skeleton / `ApiErrorState` + thử lại |
| Rỗng | "Chưa có ai trên bảng này" + gợi ý hoàn thành practice / làm bài thi |
| Tôi chưa có điểm | "Bạn chưa có điểm ở bảng này", không hiện hạng |
| Không có bài thi cuối | "Khóa này chưa có bài thi cuối khóa" |
| Tên ẩn danh | Avatar mặc định + "Học viên ẩn danh" |

Mở tab mỗi lần thì tải lại (dữ liệu luôn mới sau khi hoàn thành lesson). Đổi board/topic nhanh: `useApiResource` key gồm `courseId+board+topicId`, bỏ response cũ.

A11y: `role="tablist"/tab/tabpanel`, `<table>` có `caption` + `th scope`, select có label, layout 360px không cuộn ngang.

## Test

`tests/learning-path.test.mjs`: adapter map leaderboard (null giữ null, decimal score); helper format điểm; tab đổi query giữ `topic=`.
Lệnh (frontend root): `node --test tests/*.mjs`, `npm run typecheck`, `npm run lint`. Kiểm tra tay trên trình duyệt với backend thật: 2 tài khoản, đồng điểm, tôi ngoài top.
