# Smoke Supabase Storage: báo cáo

Ngày: 2026-10-19. Project: dự án hiện tại (`kndrsdqzbzsaiuuruqno`). Bucket test: `contract-test-smoke` (private, không giới hạn MIME/dung lượng). Chỉ thao tác trong bucket test. Không in khóa, token, `signedURL` đầy đủ hay body nguyên văn.

## Kết quả

| # | Câu hỏi | Kết quả |
|---|---|---|
| 1 | Header nào được chấp nhận | `apikey` → 200. `Authorization: Bearer` → 400. Khóa `sb_secret_` không phải JWT |
| 2 | Ghi trùng key | HTTP **400**, `statusCode` trong body **409**, `error` = `Duplicate`. Xác nhận quirk: map theo `statusCode` trong body |
| 3 | Kiểm tra tồn tại | `HEAD` có/không đều **400**, không có body: **không dùng được**. `GET /object/info/{bucket}/{key}`: có → 200, không → 400 với body `statusCode` 404 |
| 4 | Signed download | `expiresIn=2`: tải được 200, byte khớp. Sau 3 giây: 400 |
| 5 | Signed upload | Token sống **7200 giây** (2 giờ). `PUT` không `content-type` → 200. `PUT` lần hai vào cùng URL → 400 (đã tồn tại, không liên quan header) |
| 6 | Message lỗi có lặp lại key không | Không. Key chứa UUID giả, message là `The resource already exists`, không có UUID |
| 7 | Dạng khóa | `sb_secret_`. Bucket API trả `allowed_mime_types: null`, `file_size_limit: null` nên có thể kiểm bằng API |
| 8a | Xóa bulk `DELETE /object/{bucket}` body `{"prefixes":[…]}` | Key có: 200, trả mảng object đã xóa, object biến mất. Key không có: **200 `[]`**. Idempotent |
| 8b | Xóa theo đường dẫn `DELETE /object/{bucket}/{key}` | Key có: 200 `{"message":"Successfully deleted"}`. Key không có: **400**, body 404 `NoSuchKey`. **Không** idempotent |
| 9 | Nhiều secret key | **Chưa kiểm**: cần xem Dashboard API Keys |
| — | Upload ~10 MB qua signed URL | 200, **7,1 giây** |

## Quyết định ảnh hưởng

- `exists`: dùng `GET /object/info/{bucket}/{key}`, đọc `statusCode` trong body. Không dùng `HEAD`.
- `delete`: dùng dạng bulk (đã đúng trong plan). Dạng đường dẫn chỉ dùng khi không tồn tại thì coi 400/404 là thành công.
- Phân loại lỗi: lấy `statusCode` từ body khi có, không chỉ HTTP status (ghi trùng: 400 + 409).
- Token signed upload: 2 giờ cố định. Plan cũ ghi "2 h" là đúng nhưng không có nguồn chính thức; bây giờ có số đo.
- `upload-timeout` 120 giây là dư so với 7 giây đo được cho 10 MB.
- Bucket có trường `allowed_mime_types` và `file_size_limit`: checklist rollout kiểm được bằng API, không cần Dashboard.

## Dọn dẹp

- Đã xóa mọi object trong bucket test. Kiểm tra liệt kê: trống.
- Chưa xóa bucket `contract-test-smoke` và chưa thu hồi khóa `smoke-temp`: chờ người dùng xác nhận (theo ý người dùng sẽ xóa sau).
- File tạm (ảnh 1×1, 10 MB) nằm trong scratchpad, file 10 MB đã xóa.

## Chưa xác minh / câu hỏi còn mở

1. Câu 9: Nhiều secret key có được không? Cần xem Dashboard.
2. Câu 5: Có cần `content-type` khi PUT vào signed URL không? Thí nghiệm chưa đủ vì lần thử thứ hai bị chặn bởi trùng key. Cần thử với key mới để tách hai yếu tố.
3. Khóa `smoketest` đã lộ trong chat: người dùng cần thu hồi, chưa xác nhận.
