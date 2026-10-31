# Phase 3 - Frontend admin + learner

## Context

- Admin form dùng drawer bên phải (`AdminDrawer`); không thêm caption/hint dưới field.
- Learner đang vẽ ảnh bằng `CourseArt` theo band; giữ làm fallback.
- Path gốc: `frontend/src/features/`

## Files

Admin:
- `admin/courses/types.ts` - `courseSchema` thêm `coverImageUrl: z.string().nullable()`, `description: z.string().nullable()`; `topicSchema` thêm `description`; `CourseCreate`/`CourseUpdate`/`TopicCreate` thêm trường.
- `admin/courses/components/CourseForm.tsx` - input URL ảnh + textarea mô tả; thumbnail xem trước nhỏ nếu có URL (không caption).
- `admin/courses/components/TopicForm.tsx` - textarea mô tả.
- `admin/courses/api/courseApi.ts` - payload create/update.
- Cập nhật `CoursesPage.test.tsx`, `TopicForm.test.tsx`, fixture liên quan.

Learner:
- `types/learningPath.ts` (`frontend/src/types/`) - `CourseRef` thêm `coverImageUrl?: string | null`, `description?: string | null`.
- `learning-path/api/learningAdapters.ts` - `CourseSummaryDto`, dto course trong topic, `mapCourseSummary`, `mapTopicSummary` (đọc `description` thật thay vì `''`; giữ "Nội dung Premium." khi topic premium mà không có mô tả).
- `learning-path/components/LearnHero.tsx` - nhận `coverImageUrl`; có ảnh thì dùng `<img>`, không thì `CourseArt`.
- `learning-path/components/CourseHero.tsx` - truyền `coverImageUrl` và `lead={course.description}`.
- `learning-path/pages/CourseListPage.tsx` - `CourseTile` hiện ảnh bìa hoặc `CourseArt`, thêm mô tả ngắn (clamp 2 dòng).
- `learning-path/placement/PlacementResultView.tsx` - thẻ khóa hiện mô tả ngắn.
- `learning-path/pages/TopicPage.tsx` - đã có `lead={topic.description}`, chỉ cần dữ liệu thật.
- CSS: `learning-path/learning-path.css` cho `<img>` cover (object-fit, bo góc như art hiện tại, chữ vẫn đọc được trên ảnh ở banner).
- Mock API `learning-path/api/mock/*` thêm trường nếu test dùng.

## Steps

1. Cập nhật type + adapter (optional field) trước, chạy typecheck.
2. Admin form + test.
3. Learner components: ảnh lỗi tải (`onError`) → quay về `CourseArt`.
4. `alt=""` cho ảnh trang trí (tên khóa đã có ở heading).
5. Kiểm tra bằng trình duyệt: khóa có ảnh, khóa không ảnh, topic có/không mô tả, placement result.

## Tests

- `npm run test` các file đã sửa (`CoursesPage`, `TopicForm`, `CourseListPage`, `learningAdapters*`, `TopicLessonsPanel`).
- `npm run lint` + `tsc` sạch.

## Risks / rollback

- Ảnh ngoài có thể chậm/chết link: đã có fallback `onError`.
- Banner chữ chồng lên ảnh có thể khó đọc: thêm lớp phủ gradient nhẹ ở phía chữ.
- Rollback: field optional nên bỏ code hiển thị là về trạng thái cũ.
