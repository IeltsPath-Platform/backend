---
phase: 3
title: "Frontend: hiển thị 'Kiến thức cần học' và nút Luyện tập"
status: in-progress
priority: P1
effort: "1d"
dependencies: [1]
repo: frontend
---

# Phase 3: Frontend: hiển thị "Kiến thức cần học" và nút Luyện tập

Repo: `C:\Users\pduy8\OneDrive\Desktop\capstone-fall26\frontend` (React + TypeScript + Vite, test bằng Vitest và Testing Library).
Đọc `plan.md` (hợp đồng JSON, S8-S10, rủi ro 1 và 5) trước. Có thể bắt đầu ngay khi hợp đồng JSON đã chốt, không cần chờ backend chạy:
mọi test dùng dữ liệu giả.

## Mục tiêu

Trong khung kết quả của essay ở bộ luyện tập Writing (`PracticeEssays`), sau mục "Chỗ cần sửa", hiện mục **"Kiến thức cần học"** từ
`studyPoints`. Mục `CRITERION` có nút **"Luyện tập"** mở luyện tự do của KP đó. Mục `AI` chỉ hiện chữ. Không có `studyPoints` thì không hiện mục này.

Bố cục hiện tại (đã đổi sau khi lên plan): `EssayResult` nằm ở **cột phải** của workspace, tab "Kết quả chấm" (cạnh tab "Từ điển"), trong một
khung cuộn hẹp (~1/3 chiều rộng). Vì vậy mục "Kiến thức cần học" phải thiết kế một cột, không dùng lưới nhiều cột; không đặt kết quả lại vào khung bài viết ở giữa.
Test truy vấn nội dung kết quả sau khi chấm vẫn thấy ngay vì tab "Kết quả chấm" tự mở khi bài được chấm.

## Files (đường dẫn dưới `src/`)

| Hành động | File |
|---|---|
| Modify | `types/learningPath.ts` (thêm `EssayStudyPoint`, `EssayGrade.studyPoints`) |
| Modify | `features/learning-path/api/learningAdapters.ts` (`EssayGradeDto`, `mapEssayGrade`) |
| Create | `features/learning-path/components/essayCriteria.ts` (`CRITERION_NAME` tách ra khỏi `PracticeEssays.tsx`) |
| Create | `features/learning-path/components/EssayStudyPoints.tsx` |
| Create | `features/learning-path/lib/studyPointPractice.ts` |
| Modify | `features/learning-path/components/PracticeEssays.tsx` (prop `onPractise`, dùng `essayCriteria`, render `EssayStudyPoints`) |
| Modify | `features/learning-path/components/LessonPractice.tsx` (truyền `onPractise`) |
| Modify | `features/learning-path/practice-essay.css` (style mục mới, cạnh `.lp-essay-corrections`, khoảng dòng 530) |
| Create (test) | `features/learning-path/components/EssayStudyPoints.test.tsx` |
| Create (test) | `features/learning-path/lib/studyPointPractice.test.ts` |
| Create (test) | `features/learning-path/api/learningAdapters.studyPoints.test.ts` |
| Modify (test) | `features/learning-path/components/PracticeEssays.test.tsx` (fixture `grade` + test mới) |
| Modify (test) | `features/learning-path/components/PracticeEssaysSample.test.tsx` (fixture `grade`) |

Không đổi: `mockLearningApi.ts` (mock essay đang trả 404), `KnowledgeTreePage.tsx`, `FreePracticePage.tsx`, `routes.ts`, `apiError.ts`
(`userMessage` đã có chữ tiếng Việt cho `FREE_PRACTICE_EXHAUSTED`, `FREE_PRACTICE_UNAVAILABLE`, `HEARTS_DEPLETED`, `COURSE_NOT_JOINED`, `REVIEW_REQUIRED`).

## Bước 1: Kiểu dữ liệu (`types/learningPath.ts`)

```ts
export interface EssayStudyPoint {
  kind: 'CRITERION' | 'AI'
  criterionCode: string | null
  band: number | null
  knowledgePointId: string | null
  code: string | null
  name: string | null
  title: string | null
  reason: string | null
}
```

Thêm `studyPoints: EssayStudyPoint[]` vào `EssayGrade` (bắt buộc, mặc định `[]` ở adapter). Chạy `npm run typecheck` để thấy mọi chỗ dựng `EssayGrade`
(hai file test ở trên) và thêm `studyPoints: []`.

## Bước 2: Adapter (`learningAdapters.ts`)

`EssayGradeDto` thêm:

```ts
studyPoints?: Array<{
  kind?: string
  criterionCode?: string | null
  band?: number | null
  knowledgePointId?: string | null
  code?: string | null
  name?: string | null
  title?: string | null
  reason?: string | null
}>
```

`mapEssayGrade` thêm `studyPoints: (dto.studyPoints ?? []).map(...)`: `kind` là `'CRITERION'` chỉ khi đúng chuỗi đó, còn lại `'AI'`; mọi trường
còn lại `?? null`. Bỏ qua phần tử không có gì để hiển thị: mục `AI` không có `title`; mục `CRITERION` không có `knowledgePointId`
và không có `criterionCode`.

Test `api/learningAdapters.studyPoints.test.ts` (hàm thuần, không cần mock):
- thiếu `studyPoints` → `[]`;
- một mục `CRITERION` đủ trường và một mục `AI` → map đúng, trường thiếu thành `null`;
- `kind` lạ thành `AI`; mục `AI` không có `title` bị bỏ; mục `CRITERION` không có `knowledgePointId` bị bỏ.

## Bước 3: `essayCriteria.ts`

Chuyển nguyên `CRITERION_NAME` (TA, TR, CC, LR, GRA) từ `PracticeEssays.tsx` sang `components/essayCriteria.ts` và `export`. `PracticeEssays.tsx` import từ đó và xóa bản cục bộ.
Mục đích: `EssayStudyPoints` dùng chung mà không import vòng.

## Bước 4: Hàm mở luyện tập (`lib/studyPointPractice.ts`)

Hàm thuần để test không cần Router:

```ts
import type { NavigateFunction } from 'react-router-dom'
import type { EssayStudyPoint } from '~types/learningPath'
import { learningApi, toApiError } from '../api'
import { reportApiError } from './reviewGate'
import { freePracticePath, type FreePracticeNavigationState } from './routes'

/** Starts free practice on the knowledge point of a study point and opens it; a refusal is reported and rethrown. */
export async function startStudyPointPractice(point: EssayStudyPoint, backTo: string, navigate: NavigateFunction) {
  if (!point.knowledgePointId) return
  try {
    const attempt = await learningApi.startFreePractice(point.knowledgePointId)
    navigate(freePracticePath(attempt.attemptId), {
      state: {
        freePractice: {
          pointName: point.name ?? point.title ?? 'Bộ luyện tự do',
          pointCode: point.code ?? '',
          backTo,
        },
      } satisfies FreePracticeNavigationState,
    })
  } catch (reason) {
    const error = toApiError(reason)
    reportApiError(error)
    throw error
  }
}
```

Làm đúng như `KnowledgeTreePage.practise()` (gọi `reportApiError` để cổng bài ôn bắt `REVIEW_REQUIRED`).

Test `lib/studyPointPractice.test.ts` (mock `../api` với `learningApi.startFreePractice`, mock `./reviewGate.reportApiError`, `navigate = vi.fn()`):
- thành công: gọi `startFreePractice('kp-1')`, rồi `navigate('/learn/practice/attempt-9', { state: { freePractice: { pointName: 'Mạch lạc và liên kết (CC)', pointCode: 'DEMO_WRITING_CRITERION_CC', backTo: '/learn/lessons/l1' } } })`;
- thiếu `name` thì dùng `title`, thiếu cả hai thì `'Bộ luyện tự do'`; thiếu `code` thì `pointCode: ''`;
- không có `knowledgePointId`: không gọi API, không `navigate`;
- API ném `ApiError(409, 'FREE_PRACTICE_EXHAUSTED', ...)`: `reportApiError` được gọi, hàm ném lại lỗi, không `navigate`.

## Bước 5: Component `EssayStudyPoints.tsx`

```tsx
export function EssayStudyPoints({ points, onPractise }: {
  points: EssayStudyPoint[]
  /** Starts practice on the point's knowledge point; a rejection is shown under that point. */
  onPractise?: (point: EssayStudyPoint) => Promise<void>
})
```

Hành vi:
- `points.length === 0` → trả `null` (không hiện gì).
- Khung: `<section className="lp-essay-study" aria-labelledby=...>` có `<h4>Kiến thức cần học</h4>` và `<ul>`.
- Mỗi mục là `<li>`. Tiêu đề hiển thị = `point.name ?? point.title ?? CRITERION_NAME[point.criterionCode] ?? point.criterionCode`.
  - Mục `CRITERION`: dòng phụ `"{CRITERION_NAME[code] ?? code} · band {band.toFixed(1)}"`; `reason` nếu có.
  - Mục `AI`: tiêu đề và `reason` nếu có; không có dòng phụ, không có nút.
- Nút "Luyện tập" chỉ khi `onPractise` có mặt VÀ `point.knowledgePointId` khác null. `type="button"`, dùng `Button` của `@/components/ui/button`
  với `className="lp-btn"`, kèm `aria-label={`Luyện tập ${tiêu đề}`}`.
- Bấm nút: chặn bấm đúp bằng `useRef`; hiện `Loader2` và chữ "Đang mở…", nút `disabled`. Gọi `onPractise(point)`.
  - Thành công: component sẽ bị gỡ vì điều hướng, không cập nhật state sau đó.
  - Lỗi: bỏ trạng thái đang mở, hiện ngay dưới mục đó `<p className="lp-error" role="alert">` với `userMessage(toApiError(reason))`
    (không tự chế chữ khác; `FREE_PRACTICE_EXHAUSTED` đã có thông báo "Đã hết bộ luyện tự do... giữ cho bài ôn").
    Lỗi của mục nào hiện ở mục đó; bấm lại thì xóa lỗi cũ.
- Mọi chuỗi từ server hiển thị như văn bản thường trong JSX (không `dangerouslySetInnerHTML`).
- `key` của mục: `${kind}-${knowledgePointId ?? title}-${index}` hoặc chỉ số, miễn không trùng.

Style (`practice-essay.css`): `.lp-essay-study` (h4 giống `.lp-essay-corrections h4`), `ul` dạng `grid` gap `.5rem`, `li` thẻ giống `.lp-essay-corrections li`
(nền `var(--lp-surface)`, viền `var(--lp-line)`, bo `10px`, padding `.55rem .75rem`), tiêu đề đậm, dòng phụ `var(--lp-muted)` cỡ nhỏ,
hàng nút căn phải. Dùng đúng biến màu đang có, không thêm màu mới.

## Bước 6: Gắn vào `PracticeEssays` và `LessonPractice`

`PracticeEssays.tsx`:
- Thêm prop tùy chọn `onPractise?: (point: EssayStudyPoint) => Promise<void>` vào `PracticeEssaysProps`; truyền qua `EssayResult` (thêm prop) rồi vào `EssayStudyPoints`.
- Trong `EssayResult`, render `<EssayStudyPoints points={grade.studyPoints} onPractise={onPractise} />` ngay sau khối `corrections` (kể cả khi không có `corrections`).
- Không dùng hook của router trong file này (test hiện có render không có Router).

`LessonPractice.tsx` (đã nằm trong Router và đã import `Link` từ `react-router-dom`):
- Thêm `useNavigate` và `useLocation` ở đầu component (trước mọi `return` sớm).
- Truyền vào `<PracticeEssays ... onPractise={(point) => startStudyPointPractice(point, `${location.pathname}${location.search}`, navigate)} />`.

## Bước 7: Test component

`EssayStudyPoints.test.tsx` (render trực tiếp, `onPractise = vi.fn()`):
- không có điểm nào → không có tiêu đề "Kiến thức cần học";
- mục `CRITERION`: thấy tên KP, dòng "Coherence & Cohesion · band 5.5", lý do, và nút `Luyện tập Mạch lạc và liên kết (CC)`;
- mục `CRITERION` thiếu `name`/`title`: tiêu đề rơi về `CRITERION_NAME[criterionCode]`;
- mục `AI`: có tiêu đề và lý do, KHÔNG có nút;
- không truyền `onPractise` hoặc `knowledgePointId` null: KHÔNG có nút;
- bấm nút gọi `onPractise` đúng một lần với đúng mục; bấm đúp nhanh vẫn một lần; nút `disabled` trong lúc chờ;
- `onPractise` ném `ApiError(409, 'FREE_PRACTICE_EXHAUSTED', ...)`: `role="alert"` hiện chữ "Đã hết bộ luyện tự do" ở đúng mục, nút dùng lại được;
- nội dung có ký tự `<b>` hiển thị đúng chữ, không thành thẻ.

`PracticeEssays.test.tsx`:
- Fixture `grade` thêm `studyPoints: []`; test mới `shows the study points of a graded essay and lets the learner practise one`:
  `submitPracticeEssay.mockResolvedValue({ ...grade, studyPoints: [criterionPoint, aiPoint] })`, truyền `onPractise = vi.fn().mockResolvedValue(undefined)`
  vào `PracticeEssays` (cần mở rộng `renderEssays` nhận prop này), nộp bài, thấy "Kiến thức cần học", bấm "Luyện tập ..." → `onPractise` được gọi với mục `CRITERION`.
- Test `does not show the study points section when the grade has none` (fixture `studyPoints: []`).
- Giữ xanh toàn bộ test hiện có (kể cả test nút "Nộp để chấm", "Hoàn thành bộ luyện tập" và việc ẩn nút "Chấm lại").

`PracticeEssaysSample.test.tsx`: chỉ thêm `studyPoints: []` vào fixture.

## Chạy (trong thư mục `frontend`)

```powershell
npm run typecheck
npx eslint src/features/learning-path/components/EssayStudyPoints.tsx src/features/learning-path/components/PracticeEssays.tsx src/features/learning-path/components/LessonPractice.tsx src/features/learning-path/lib/studyPointPractice.ts src/features/learning-path/api/learningAdapters.ts
npx vitest run src/features/learning-path
npm test
```

`npm test` chạy cả bộ; ghi số test chạy/lỗi. Nếu một test ngoài phạm vi này lỗi, chạy đúng test đó trên bản HEAD sạch (tạo `git worktree` riêng,
không dùng `git stash`) để biết lỗi có sẵn từ trước không, trước khi báo là hồi quy.

## Tiêu chí hoàn thành

- [x] Mục "Kiến thức cần học" hiện sau "Chỗ cần sửa" khi có `studyPoints`; không hiện khi rỗng.
- [x] Nút "Luyện tập" chỉ ở mục `CRITERION` có `knowledgePointId`; bấm mở trang luyện tự do với `backTo` là trang bộ luyện tập hiện tại.
- [x] Lỗi mở luyện tập (hết bộ, hết tim, chưa tham gia khóa, cần làm bài ôn) hiện đúng thông báo của `userMessage` ngay dưới mục.
- [x] `PracticeEssays` vẫn render được không cần Router; test cũ không phải sửa logic (chỉ thêm `studyPoints: []` vào fixture).
- [ ] `typecheck`, eslint các file đã sửa, `vitest` và `npm test` xanh.

## Rủi ro và hoàn tác

- Điều hướng khỏi khung viết khi bài chưa "Hoàn thành": bài đã chấm không mất (server giữ), học viên quay lại bằng `backTo` rồi mở lại bộ. Không cần xử lý thêm.
- Nút chỉ chạy một lần cho mỗi KP vì `reserve` (plan.md, rủi ro 1): thông báo đã có sẵn, không dựng thêm UI.
- Hoàn tác: bỏ dòng render `EssayStudyPoints` (hoặc backend trả `studyPoints = []`) là mục này biến mất.

## Đồng bộ trạng thái — 2026-10-21

Triển khai UI và điều hướng đã có. Chạy riêng bốn file test của tính năng ngày 2026-10-21: 41/41 test qua. Typecheck, lint và build đã qua trong lượt làm việc trước cùng cuộc hội thoại; không chạy lại trong lần cập nhật tài liệu này. Gate `npm test` chưa xanh: lần toàn bộ 212/215 qua; chạy riêng lại các file lỗi còn hai lỗi Auth/PracticeEssaysSample, timeout PracticeEssays đã hết khi chạy riêng. Chưa xác minh hai lỗi frontend này trên HEAD sạch, nên không kết luận là hồi quy hay lỗi nền. Chi tiết: [báo cáo đồng bộ](./reports/pm-2026-10-21-writing-plan-status.md).
