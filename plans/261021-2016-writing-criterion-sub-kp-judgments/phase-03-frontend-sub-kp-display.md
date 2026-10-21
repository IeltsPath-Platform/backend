---
phase: 3
title: "Frontend: hiển thị đánh giá KP con và nút Luyện tập"
status: complete
priority: P2
effort: "0.75d"
dependencies: [2]
---

# Phase 3: Frontend — hiển thị đánh giá KP con

Repo: `frontend` (thư mục `frontend/` cùng workspace với `backend/`; đường dẫn dưới đây tương đối với `frontend/`). Đọc [plan.md](./plan.md) trước, đặc biệt mục "Hợp đồng JSON". Phase 2 phải chốt hợp đồng (có thể làm song song nếu dùng đúng JSON trong plan).

## Overview
Kết quả chấm essay trong course workspace hiện nhãn từng KP con dưới mỗi tiêu chí (Đạt / Cần luyện / Chưa đủ bằng chứng), và study point `KNOWLEDGE_POINT` có đoạn trích + nút Luyện tập mở đúng bộ luyện.

## Requirements
- Bài chấm cũ (không có `knowledgePointJudgments`, không có kind `KNOWLEDGE_POINT`) hiển thị y như hiện tại.
- `NOT_ASSESSED` hiển thị trung tính ("Chưa đủ bằng chứng"), **không** dùng style cảnh báo/yếu.
- Nút Luyện tập của study point `KNOWLEDGE_POINT` dùng `practiceKnowledgePointId ?? knowledgePointId`; không có id nào → không có nút. Loading, chặn bấm lặp, báo lỗi giữ nguyên hành vi của `CRITERION`.
- Kind lạ trong tương lai tiếp tục được xử lý như `AI` (hành vi hiện tại), không crash.
- Không thêm thư viện; tiếng Việt cho nhãn; theo class `lp-*` và token CSS hiện có.
- Trang kết quả standalone (`/practice/writing/submissions/:id/result`) ngoài phạm vi.

## Architecture / thay đổi cụ thể

### Types — `src/types/learningPath.ts`
```ts
export type EssayJudgment = 'PASS' | 'FAIL' | 'NOT_ASSESSED'
export interface EssayKnowledgeJudgment {
  knowledgePointId: string
  code: string | null
  name: string | null
  judgment: EssayJudgment
  excerpt: string | null
  reason: string | null
}
// EssayCriterion: thêm (tùy chọn để fixture cũ vẫn biên dịch)
knowledgePointJudgments?: EssayKnowledgeJudgment[]
// EssayStudyPoint: mở rộng
kind: 'CRITERION' | 'KNOWLEDGE_POINT' | 'AI'
excerpt?: string | null
practiceKnowledgePointId?: string | null
```
Cập nhật cả kiểu DTO ở `learningAdapters.ts` (~dòng 273–285): `criteria[]` thêm `knowledgePointJudgments?: Array<{knowledgePointId?: string; code?: string|null; name?: string|null; judgment?: string; excerpt?: string|null; reason?: string|null}>`, `studyPoints[]` thêm `excerpt?`, `practiceKnowledgePointId?`.

### Adapter — `src/features/learning-path/api/learningAdapters.ts` (`mapEssayGrade`, ~dòng 646–672)
- `criteria`: thêm `knowledgePointJudgments: (criterion.knowledgePointJudgments ?? []).filter((item) => item.knowledgePointId).map(...)` với `judgment` chuẩn hóa: `'PASS'`/`'FAIL'` giữ, mọi giá trị khác → `'NOT_ASSESSED'`; `code/name/excerpt/reason` thiếu → `null`.
- `studyPoints`: `kind` = `'CRITERION'` | `'KNOWLEDGE_POINT'` | `'AI'` (khác → `'AI'`); thêm `excerpt: point.excerpt ?? null`, `practiceKnowledgePointId: point.practiceKnowledgePointId ?? null`; bộ lọc: `CRITERION` và `KNOWLEDGE_POINT` cần `knowledgePointId`, `AI` cần `title` không rỗng.

### Helper — `src/features/learning-path/components/essayCriteria.ts`
Thêm `export const JUDGMENT_LABEL: Record<EssayJudgment, string> = { PASS: 'Đạt', FAIL: 'Cần luyện', NOT_ASSESSED: 'Chưa đủ bằng chứng' }`. `groupStudyPoints` giữ nguyên (chỉ gắn `AI` vào nhóm `CRITERION`; `KNOWLEDGE_POINT` là mục riêng theo thứ tự backend).

### Study points — `src/features/learning-path/components/EssayStudyPoints.tsx`
- `StudyPointItem`: với `kind === 'KNOWLEDGE_POINT'`: tiêu đề `point.name ?? point.title`; dòng nhỏ `lp-essay-study__criterion` hiện `{tên tiêu chí} · Cần luyện`; nếu có `excerpt` hiện `<blockquote className="lp-essay-study__excerpt">{excerpt}</blockquote>`; `reason` như hiện có.
- Nút Luyện tập: điều kiện hiện tại chỉ cho `CRITERION`; đổi thành `const target = point.kind === 'KNOWLEDGE_POINT' ? (point.practiceKnowledgePointId ?? point.knowledgePointId) : point.kind === 'CRITERION' ? point.knowledgePointId : null` và hiện nút khi `onPractise && target`.
- `aria-label` nút giữ dạng `Luyện tập {title}`.

### Bắt đầu luyện — `src/features/learning-path/lib/studyPointPractice.ts`
`const target = point.practiceKnowledgePointId ?? point.knowledgePointId; if (!target) return; ... learningApi.startFreePractice(target)`. `pointName/pointCode` vẫn lấy từ `point.name/code`.

### Khối tiêu chí — `src/features/learning-path/components/PracticeEssays.tsx` (~dòng 449–472)
Trong mỗi `<li>` tiêu chí, sau `lp-essay-criteria__notes`, nếu `criterion.knowledgePointJudgments?.length`:
```tsx
<ul className="lp-essay-criteria__kps" aria-label={`Kiến thức con của ${CRITERION_NAME[criterion.code] ?? criterion.code}`}>
  {criterion.knowledgePointJudgments.map((item) => (
    <li className={`is-${item.judgment === 'PASS' ? 'good' : item.judgment === 'FAIL' ? 'weak' : 'neutral'}`} key={item.knowledgePointId}>
      <span>{item.name ?? item.code}</span>
      <em>{JUDGMENT_LABEL[item.judgment]}</em>
      {item.excerpt ? <q>{item.excerpt}</q> : null}
    </li>
  ))}
</ul>
```

### CSS — `src/features/learning-path/practice-essay.css`
Thêm gần `.lp-essay-criteria__notes` (~dòng 560) và `.lp-essay-study__note` (~dòng 639): `.lp-essay-criteria__kps` (list không bullet, khoảng cách nhỏ như `__notes`), `.is-good` / `.is-weak` / `.is-neutral` dùng biến màu đã có trong file (xem `.is-good` ở `.lp-essay-criteria__notes`; `is-neutral` dùng màu chữ phụ, **không** màu cảnh báo), `.lp-essay-study__excerpt` (blockquote nhỏ, viền trái). Không thêm màu mới ngoài biến sẵn có.

## Related Code Files
Modify:
- `src/types/learningPath.ts`
- `src/features/learning-path/api/learningAdapters.ts`
- `src/features/learning-path/components/essayCriteria.ts`
- `src/features/learning-path/components/EssayStudyPoints.tsx`
- `src/features/learning-path/components/PracticeEssays.tsx`
- `src/features/learning-path/lib/studyPointPractice.ts`
- `src/features/learning-path/practice-essay.css`
Tests:
- `src/features/learning-path/api/learningAdapters.studyPoints.test.ts`
- `src/features/learning-path/components/EssayStudyPoints.test.tsx`
- `src/features/learning-path/components/PracticeEssays.test.tsx`
- `src/features/learning-path/lib/studyPointPractice.test.ts`
Không sửa: `LessonPractice.tsx` (đã truyền `startStudyPointPractice`), mock API (không có essay study point), trang standalone.

## Implementation Steps (TDD)
1. Baseline: `npm run typecheck`, `npm run lint`, `npx vitest run` (toàn bộ). Ghi test đỏ có sẵn (plan study points trước đó ghi `npm test` còn lỗi nền) — **không sửa** test không liên quan.
2. Viết test (đỏ):
   - `learningAdapters.studyPoints.test.ts`: (a) criteria có `knowledgePointJudgments` được map, judgment lạ → `NOT_ASSESSED`, thiếu `knowledgePointId` bị loại, thiếu mảng → `[]`; (b) study point `KNOWLEDGE_POINT` giữ `excerpt` và `practiceKnowledgePointId`, thiếu `knowledgePointId` bị loại; (c) kind lạ vẫn → `AI`. **Cập nhật** các kỳ vọng `toEqual` hiện có của `CRITERION`/`AI` thêm `excerpt: null, practiceKnowledgePointId: null` (adapter luôn gán hai trường này) — đây là thay đổi test có chủ đích.
   - `EssayStudyPoints.test.tsx`: `KNOWLEDGE_POINT` hiện tên, excerpt trong blockquote, reason; có nút Luyện tập khi có `practiceKnowledgePointId`, gọi `onPractise` với đúng point; không `practiceKnowledgePointId` nhưng có `knowledgePointId` vẫn có nút; cả hai null → không nút; thứ tự render theo mảng.
   - `studyPointPractice.test.ts`: dùng `practiceKnowledgePointId` khi có (`startFreePractice` gọi với id đó), fallback `knowledgePointId`, cả hai null → không gọi.
   - `PracticeEssays.test.tsx`: grade có judgments → hiện ba nhãn "Đạt", "Cần luyện", "Chưa đủ bằng chứng" đúng tiêu chí; `NOT_ASSESSED` không có class `is-weak`; grade không có judgments → không render list `Kiến thức con của …`.
3. Code theo thứ tự types → adapter → helper → components → css. Chạy lại test đến khi xanh.
4. Chạy `npx vitest run` toàn bộ, `npm run typecheck`, `npm run lint`; so với baseline.
5. Nếu có thể chạy app (`/run` hoặc dev server) kiểm bằng mắt một grade giả có đủ ba loại judgment; nếu không, ghi "chưa kiểm bằng mắt".

## Success Criteria
- [x] 50 test mục tiêu pass; `typecheck`, `lint`, `build` pass. Full Vitest có 223/224 test pass; test AuthPage thất bại do thiếu `ToastProvider` (ngoài các file Phase 3).
- [x] Grade cũ không có judgments không render danh sách kiến thức con; test `PracticeEssays.test.tsx` hồi quy pass.
- [x] `NOT_ASSESSED` hiển thị nhãn trung tính và class `is-neutral`, không dùng `is-weak`.
- [x] Nút Luyện tập của `KNOWLEDGE_POINT` ưu tiên `practiceKnowledgePointId`, fallback `knowledgePointId`; thiếu cả hai thì ẩn nút.
- [x] Không thêm dependency.

## Risk Assessment
- Fixture `EssayStudyPoint` ở nhiều test không có hai trường mới: để tùy chọn (`?`) trong type để tránh sửa hàng loạt; chỉ các kỳ vọng `toEqual` của adapter đổi.
- `groupStudyPoints` chỉ gắn `AI` vào `CRITERION`; `KNOWLEDGE_POINT` luôn là mục riêng. Chấp nhận.
- Hợp đồng JSON thay đổi sau phase 2 → phải sửa adapter theo; khi lệch, hợp đồng trong plan.md là chuẩn.
- Lỗi nền `npm test` có từ plan trước: tách bạch, không che.
