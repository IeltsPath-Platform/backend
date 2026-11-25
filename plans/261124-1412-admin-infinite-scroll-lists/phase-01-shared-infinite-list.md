# Phase 01: Hook và thành phần dùng chung cho cuộn vô hạn

Plan: [plan.md](plan.md) · Trạng thái: done · Chỉ frontend, chưa đổi màn nào

## Context

- `frontend/src/features/admin/lib/useAdminResource.ts` (tải một lần theo khóa, hủy yêu cầu cũ; mẫu cho `status`/`error`/`reload`)
- Các màn đang tự phân trang để biết hình dạng dữ liệu: `questions/pages/QuestionBankPage.tsx`, `packages/pages/PackagesPage.tsx`, `lessons/components/QuestionChooser.tsx`
- `frontend/src/features/admin/components/AdminState.tsx` (`AdminLoading`, `AdminError`), `AdminLayer.tsx` (thân hộp thoại và ngăn kéo là vùng cuộn)
- `frontend/src/test/` (tiện ích test dùng chung, `render`)
- `frontend/.sdd/constraints/admin-ui.md` (quy ước UI), `frontend/AGENTS.md` (không thêm dependency)

## Yêu cầu

**Hook `useInfiniteList`** (`src/features/admin/lib/useInfiniteList.ts`):

```ts
useInfiniteList<T>(key: string, load: (page: number, signal: AbortSignal) => Promise<PageResult<T>>, options?: { getId?: (item: T) => string })
// PageResult<T> = { content: T[]; page: number; totalElements: number; totalPages: number }
// -> { items: T[]; total: number; status: 'loading' | 'success' | 'error'; error: string | null;
//      loadingMore: boolean; moreError: string | null; hasMore: boolean;
//      loadMore(): void; retryMore(): void; reload(): void; refresh(): void }
```

1. Đổi `key` thì bỏ danh sách cũ, về trang 0, hủy yêu cầu đang chạy, `status = 'loading'`.
2. `loadMore()` tải trang kế tiếp; chỉ chạy khi `hasMore` và chưa có yêu cầu đang chạy (chống gọi chồng).
3. Gộp theo `getId` (mặc định `item.id`), bỏ dòng trùng; giữ thứ tự.
4. `hasMore` = `page + 1 < totalPages` của trang cuối đã tải.
5. Lỗi khi tải trang đầu → `status = 'error'`. Lỗi khi tải thêm → **giữ các dòng đã có**, đặt `moreError`, **không tự tải lại**; chỉ `retryMore()` mới thử lại (chặn vòng lặp).
6. `reload()` tải lại từ đầu (xóa danh sách); `refresh()` tải lại từ đầu ở nền, giữ danh sách cũ trên màn cho đến khi trang đầu mới về (dùng sau thao tác ghi).
7. Hủy yêu cầu cũ bằng `AbortSignal`; bỏ qua `AbortError`.

**Thành phần `InfiniteSentinel`** (`src/features/admin/components/InfiniteSentinel.tsx`):

- Vẽ một phần tử cuối danh sách và gọi `onReachEnd` khi nó vào vùng nhìn cách đáy vùng cuộn ≤ 400px.
- **Gốc quan sát = vùng cuộn gần nhất** của phần tử (duyệt cha có `overflow-y: auto|scroll`, không thấy thì cửa sổ). Tính lại khi mount.
- Danh sách ngắn hơn vùng nhìn: sentinel vẫn thấy sau khi tải, nên phải gọi tiếp cho đến khi đầy vùng hoặc hết dữ liệu (dựa vào `hasMore`, không tự lặp khi `moreError`).
- Hiển thị bên trong chính nó: `AdminLoading` nhỏ ("Đang tải thêm…") khi `loadingMore`; thông báo lỗi kèm nút "Thử lại" khi có `moreError`; dòng "Đã hiện tất cả {n} {đơn vị}" khi hết dữ liệu và `items.length > 0`. Nhãn đơn vị truyền qua prop (`noun="câu hỏi"`).
- Vùng trạng thái có `role="status"` (đọc cho trình đọc màn hình); không dùng toast cho trạng thái này.
- Dự phòng: không có `IntersectionObserver` (môi trường cũ) thì hiện nút "Tải thêm" thay thế.
- Chữ nghĩa: phần tử cuối là `<li>`/`<tr>` hay `<div>` tùy ngữ cảnh: nhận prop `as` (`'div' | 'tr'` với `colSpan`).

**Tiện ích dùng chung:**

- Dòng tóm tắt `InfiniteCount`: "Đang hiện {items} / {total} {noun}", **đặt ở đầu danh sách và dính** (`position: sticky; top: 0`) trong vùng cuộn, đã chốt. Nằm trên đầu bảng dính nên hai phần tử dính phải xếp chồng đúng (đầu bảng đặt `top` bằng chiều cao dòng đếm, qua biến CSS). Không hiện khi chưa có dòng nào.
- CSS: `.adm-infinite` (khoảng cách, màu chữ phụ), đầu bảng dính `.adm-table--sticky thead th { position: sticky; top: 0; z-index: 1; background: var(--adm-surface-2) }` trong vùng cuộn. Dùng token `--adm-*`, không màu cứng.
- Tiện ích test `src/test/intersection.ts`: một `IntersectionObserver` giả đăng ký theo phần tử, hàm `intersect(element)` kích hoạt thủ công, `reset()`; gắn vào `setup` hoặc để mỗi test gọi `mockIntersectionObserver()`. Ghi rõ cách dùng ở đầu file.

## Files tạo / sửa

- Tạo: `lib/useInfiniteList.ts`, `lib/useInfiniteList.test.tsx`, `components/InfiniteSentinel.tsx`, `components/InfiniteSentinel.test.tsx`, `src/test/intersection.ts`, CSS trong `admin.css` (hoặc file CSS dùng chung đã có).
- Không sửa màn nào, không thêm dependency, không đổi `package.json`.

## Test

- Hook: tải trang đầu; `loadMore` nối trang kế và bỏ trùng; đổi `key` đặt lại về trang 0 và hủy yêu cầu cũ; hai `loadMore` liền nhau chỉ gửi một yêu cầu; hết dữ liệu thì `hasMore = false` và `loadMore` không gọi; lỗi trang đầu → `error`; lỗi tải thêm giữ dòng cũ, đặt `moreError`, không tự thử lại, `retryMore` thử lại; `refresh` giữ dòng cũ trong lúc tải.
- Sentinel: gọi `onReachEnd` khi giao nhau; không gọi khi `hasMore` sai hoặc đang `loadingMore`; hiện đủ ba trạng thái (đang tải, lỗi kèm Thử lại, hết); tìm đúng vùng cuộn gần nhất (hộp thoại có `overflow-y: auto`, danh sách tự cuộn, cửa sổ); dự phòng nút "Tải thêm" khi không có observer.
- Chạy bằng PowerShell: `npx.cmd vitest run src/features/admin/lib src/features/admin/components`, rồi `npm run lint`, `npm run typecheck`.

## Rủi ro

- Tìm vùng cuộn bằng `getComputedStyle` trong jsdom không có bố cục thật: test dùng style gán tay (`overflow-y`) để phân biệt, không thể thử cuộn thật. Cần thử tay trên trình duyệt ở phase 02.
- Sentinel đặt trong `<tbody>` phải là `<tr>` hợp lệ; trong danh sách `<ul>` phải là `<li>`.
