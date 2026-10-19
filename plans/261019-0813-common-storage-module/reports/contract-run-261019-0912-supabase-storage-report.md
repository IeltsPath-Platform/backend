# Contract test Supabase Storage: báo cáo chạy thật

Ngày: 2026-10-19. Project: `kndrsdqzbzsaiuuruqno`. Bucket: `contract-test-smoke` (private). Khóa: đọc từ file ngoài repo, không in.
Lệnh: `mvn -pl shared/common-storage test -Dtest=SupabaseStorageContractTest` với `STORAGE_CONTRACT_*` đặt trong phiên.

## Kết quả

- **3/3 xanh** (`writesReadsSignsAndDeletesOnlyInsideTheTestBucket`, `wrongKeyIsRejectedWithoutLeakingIt`, `keyIsServerSecretNotJwt`).
- Bucket test sau khi chạy: **0 object**.
- Mock test trong module: 60 xanh, 3 skipped (contract test khi thiếu biến).

## Phát hiện trong quá trình chạy (đã sửa)

1. **Khóa sai làm `exists` trả `false` giả.** Với khóa sai, `GET /object/info` vẫn trả 400 `statusCode` 404 "Bucket not found", giống hệt object không tồn tại. Với object có thật, khóa sai cũng trả 404. Nên 404 từ `info` không đủ để kết luận "không có". **Sửa:** khi `info` trả 404, gọi thêm `GET /bucket/{bucket}`. Chỉ trả `false` khi bucket truy cập được. Bucket lỗi thì ném `BAD_REQUEST`, bucket không có thì `NOT_FOUND`.
2. **Khóa sai trả 400, không phải 401/403.** Endpoint `bucket` trả 400 với thông điệp "headers must have required property 'authorization'". Lỗi xác thực vì vậy nằm ở reason `BAD_REQUEST`. Chấp nhận có chủ đích: `SupabaseErrors` chỉ đọc mã, không đọc thông điệp.
3. **Contract test từng in khóa khi lỗi:** `assertThat(secret)` đưa giá trị vào thông báo. **Sửa:** dùng boolean.

## Các điểm đã xác nhận bằng chạy thật

- `DELETE` bulk có body qua `java.net.http.HttpClient` (JDK) hoạt động. Lựa chọn `JdkClientHttpRequestFactory` được xác nhận.
- Ghi, đọc, ký download và upload hoạt động từ client. Signed download hết hạn sau TTL.
- Signed upload: `PUT` trực tiếp bằng HTTP client chạy được.

## Chưa xác minh / câu hỏi còn mở

1. **Nhiều secret key** (smoke câu 9): chưa kiểm, cần xem Dashboard.
2. **`content-type` khi PUT signed upload**: chưa tách được hai yếu tố vì key đã tồn tại. Cần thử với key mới.
3. **Khóa `smoketest` đã lộ trong chat**: chưa có xác nhận đã thu hồi. File `supabase-smoke.key` hiện chứa khóa mà người dùng tạo sau đó; người dùng cần xác nhận khóa đã lộ đã bị thu hồi.
4. **Bucket test và khóa tạm** chưa xóa: chờ người dùng.
