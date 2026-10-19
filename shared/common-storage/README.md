# common-storage

Thư viện dùng chung để ghi, xóa và cấp URL cho object trên Supabase Storage. Không deploy độc lập. Service nào cần
lưu file thì khai báo dependency và tự chọn bucket.

## Cấu trúc package

```
com.ieltspath.commonstorage
├── ObjectStorage              cổng duy nhất service gọi (put, delete, exists, publicUrl, signed URL)
├── StoredObject, SignedUpload kết quả ghi và URL upload có hạn
├── StorageException           lỗi hạ tầng; thông báo cố định, không chứa key/bucket/khóa
├── UploadRejectedException    file bị từ chối (IllegalArgumentException, đi đường 400)
├── UnavailableObjectStorage   bản cài khi chưa cấu hình: mọi lệnh ném UNAVAILABLE
├── key/                       ObjectKeys, InvalidObjectKeyException: sinh và kiểm object key
├── upload/                    ContentTypes, ContentSniffer, UploadPolicy: chuẩn hóa MIME, magic bytes, giới hạn
├── config/                    SupabaseStorageProperties, CommonStorageAutoConfiguration
└── supabase/                  SupabaseObjectStorage (client REST) và helper package-private
```

`key/`, `upload/` và phần còn lại thuần Java, không phụ thuộc Supabase. Chỉ `supabase/` gọi mạng.

## Bật trong service

Auto-configuration tắt mặc định. Service phải bật và cấp cấu hình:

```yaml
app:
  storage:
    enabled: true
    supabase:
      url: ${SUPABASE_URL:}          # https://<project-ref>.supabase.co, không có /storage/v1
      secret-key: ${SUPABASE_SECRET_KEY:}
      # connect-timeout 3s, read-timeout 10s, upload-timeout 120s (mặc định)
```

- `url` và `secret-key` phải cùng trống hoặc cùng có. Chỉ một trong hai là lỗi, service không khởi động.
- Cả hai trống: service vẫn khởi động, mọi lệnh storage ném `UNAVAILABLE`.
- `SUPABASE_*` là tên biến môi trường. Hiện chưa có service nào ánh xạ chúng vào config-repo; việc đó thuộc plan của từng service.

## Quy tắc khi dùng

- **Key do client gửi lại:** gọi `ObjectKeys.requireOwnedBy` và `ObjectStorage.exists` trước khi lưu. Thư viện không ép
  được việc gọi.
- **`put` chỉ cho file nhỏ** (nội dung giữ trong bộ nhớ). File lớn dùng `signedUploadUrl`. Đường này **không** qua kiểm tra
  nội dung của module; giới hạn MIME và dung lượng phải đặt ở bucket.
- **Sau `StorageException` có `outcomeUnknown=true`** (timeout giữa chừng): không retry cùng key. Sinh key mới và xóa key cũ,
  best-effort.
- **`exists`** trả `false` chỉ khi bucket truy cập được. Khóa sai cũng trả 404 khi kiểm object.
- **URL ký là bí mật**: không ghi log. `SignedUpload.toString()` đã che URL.
- **Bucket là tham số**: module không biết bucket nào công khai. Gọi `publicUrl` trên bucket private chỉ cho ra URL không dùng được.
- **Lỗi đầu vào** (`InvalidObjectKeyException`, `UploadRejectedException`) là `IllegalArgumentException`, thông báo cố định.
  Lỗi hạ tầng (`StorageException`) không phải. `GlobalExceptionHandler` hiện có map `IllegalArgumentException` thành 400.

## Test

```powershell
mvn -q -pl shared/common-storage test
```

Test mock dùng `MockRestServiceServer`, không cần mạng. Contract test
(`contract/SupabaseStorageContractTest`) chạy với Supabase thật, mặc định bị skip. Chạy khi có:

- `STORAGE_CONTRACT_URL`: URL project.
- `STORAGE_CONTRACT_KEY_FILE`: đường dẫn file chứa khóa, nằm ngoài repo. Không đặt khóa vào biến môi trường hay dòng lệnh.
- `STORAGE_CONTRACT_BUCKET`: tên bucket bắt đầu bằng `contract-test-`. Tên khác thì test skip.

Test chỉ thao tác dưới `contract-test/{uuid}/` trong bucket đó và tự dọn.

## Tài liệu liên quan

- Kết quả smoke và hành vi thật của API: `plans/261019-0813-common-storage-module/reports/`.
- Quy trình xoay khóa và ranh giới khóa server: `AGENTS.md`, mục 3.4.
