---
phase: 3
title: "Supabase storage client"
status: pending
priority: P2
effort: "4h"
dependencies: [1, 2]
---

# Phase 3: Client Supabase Storage

## Overview

Hiện thực `ObjectStorage` bằng `RestClient` gọi REST của Supabase Storage, đăng ký thành bean trong auto-config, và kiểm
chứng bằng `MockRestServiceServer` (không cần mạng, không cần khóa thật). **Điều kiện bắt đầu:** smoke thật ở cuối phase 1 đã có
báo cáo; phase này viết client theo **kết quả smoke**, không theo giả định ban đầu. Hợp đồng REST lấy từ bảng "Hợp đồng REST đã xác minh"
trong `plan.md` đã được cập nhật theo smoke.

<!-- Updated: Validation Session 1 - ví dụ timeout dùng Speaking ≤ 5 MB và mp3 admin ~10 MB -->

## Requirements

- Chức năng: 6 phương thức của `ObjectStorage` hoạt động đúng với Supabase; **mọi** lỗi hạ tầng quy về `StorageException`; key không hợp lệ quy về `InvalidObjectKeyException`; đầu vào sai do server/cấu hình quy về `StorageException(MISCONFIGURED)`; tất cả **trước khi** gọi mạng.
- Phi chức năng: có timeout kết nối, đọc và upload riêng; không retry ngầm; không theo redirect; khóa/key không lộ ở log, exception hay message; không dùng chung `RestClient.Builder` của ứng dụng (tránh dính load-balancer, interceptor của service khác); `HttpClient` được đóng khi bean hủy.

## Architecture

```
com.ieltspath.commonstorage.supabase
├── SupabaseObjectStorage.java      cài ObjectStorage, AutoCloseable
├── SupabaseErrors.java             ánh xạ mọi lỗi sang StorageException (không chép message upstream)
└── SupabaseRestClientFactory.java  dựng hai RestClient: baseUrl, header xác thực, request factory
```

### Dựng `RestClient` (`SupabaseRestClientFactory`)

- Hai client dùng chung `configure(builder, props)`: `rest` (read timeout `readTimeout`, 10s) cho thao tác nhỏ, và `uploadRest` (`uploadTimeout`, 120s) **chỉ** cho `put`. Một timeout chung 10s sẽ cắt ngang một bản ghi âm Speaking (tới 5 MB) trên mạng yếu.
- `baseUrl = {url}/storage/v1`.
- Header mặc định theo kết quả smoke #1. Giả định ban đầu: `apikey: <khóa>` **luôn gửi**; `Authorization: Bearer <khóa>` chỉ gửi khi khóa **không** bắt đầu bằng `sb_secret_` (khóa legacy là JWT).
- Request factory: `JdkClientHttpRequestFactory` trên `java.net.http.HttpClient` với `connectTimeout` và **`followRedirects(NEVER)`** (khóa không được gửi tới host khác qua redirect). Chọn factory này **chỉ khi smoke #8 xác nhận** `DELETE` có body chạy được; nếu không thì dùng dạng xóa không body (`DELETE /object/{bucket}/{key}`) và không bị ràng buộc vào factory này. Repo hiện có client dùng `SimpleClientHttpRequestFactory` (`OpenAiCompatibleEssayGrader`, `UserPublicProfileClient`), nên lựa chọn này phải được chứng minh, không mặc định.
- Dùng `RestClient.builder()` mới, không inject `RestClient.Builder` của Boot.
- Tách `configure(...)` khỏi phần request factory để test dùng `MockRestServiceServer.bindTo(builder)` (nó thay request factory) mà **vẫn** kiểm được header và baseUrl.
- Vòng đời: mỗi `SupabaseObjectStorage` sở hữu hai `HttpClient` (JDK 21 `HttpClient` là `AutoCloseable`); `SupabaseObjectStorage implements AutoCloseable` và `close()` đóng cả hai. Spring tự suy ra `close()` làm destroy method của bean.

### `SupabaseObjectStorage` (constructor nhận `RestClient` và `SupabaseStorageProperties`)

Kiểm tra đầu vào chung cho mọi phương thức (không có request nào được gửi khi sai):

- `key` qua `ObjectKeys.requireSafe` → `InvalidObjectKeyException` (message cố định, đầu vào của học viên nên đi đường 400 an toàn);
- `bucket` khớp `^[a-z0-9][a-z0-9_-]{2,62}$`; `ttl` trong `[1 giây, 15 phút]`; `content` không null/rỗng; `contentType` không rỗng → sai thì `StorageException(reason=MISCONFIGURED)`. Đây là lỗi lập trình hoặc cấu hình phía server, **không** được thành HTTP 400 gửi cho học viên (các `GlobalExceptionHandler` map `IllegalArgumentException`/`IllegalStateException` thành 400 và trả message).

**Cảnh báo ghép đường dẫn:** KHÔNG dùng `.uri("/object/{bucket}/{key}", bucket, key)`. `DefaultUriBuilderFactory` mã hóa `/` trong biến thành `%2F`, làm hỏng đường dẫn có nhiều đoạn. Ghép chuỗi `"/object/" + bucket + "/" + key` rồi truyền vào `.uri(String)`; an toàn vì bộ ký tự của `bucket` và `key` đã bị bó, không có `{`, `}`, `?`, `#`, `%`. Có một test ghim đúng điều này.

| Phương thức | Gọi | Xử lý |
| --- | --- | --- |
| `put(bucket,key,content,contentType)` | `POST /object/{bucket}/{key}` trên `uploadRest`, body = `content`, `Content-Type` = MIME đã chuẩn hóa (`ContentTypes.normalize`), `x-upsert: false`, `Cache-Control: max-age=3600` | 2xx → trả `StoredObject(bucket,key,mime,content.length)`; không cần đọc JSON phản hồi. Key đã tồn tại → `ALREADY_EXISTS`. Timeout/IO → `UNAVAILABLE` với `outcomeUnknown=true` (object có thể đã được ghi) |
| `delete(bucket,key)` | Theo smoke #3. Mặc định: `DELETE /object/{bucket}` body `{"prefixes":["{key}"]}` | 2xx → xong; key không tồn tại không phải lỗi. Timeout/IO → `outcomeUnknown=true` |
| `exists(bucket,key)` | Theo smoke #4: `HEAD /object/{bucket}/{key}` **hoặc** `GET /object/info/{bucket}/{key}` | 200 → `true`; 404 (kể cả `statusCode` 404 trong body) → `false`; lỗi khác → `StorageException` |
| `publicUrl(bucket,key)` | không mạng | `{url}/storage/v1/object/public/{bucket}/{key}`. Module **không** biết bucket nào public: gọi nó cho bucket private chỉ cho ra URL không dùng được, không phải lỗ hổng |
| `signedDownloadUrl(bucket,key,ttl)` | `POST /object/sign/{bucket}/{key}`, body `{"expiresIn":<giây>}` | Đọc `signedURL`; thiếu, rỗng, hoặc không bắt đầu bằng `/` → `UNEXPECTED_RESPONSE`. Trả `{url}/storage/v1` + `signedURL`. Javadoc: URL là bí mật, không log |
| `signedUploadUrl(bucket,key)` | `POST /object/upload/sign/{bucket}/{key}`, body `{}`, **không** `x-upsert` | Đọc `url` (cùng kiểm tra như trên). Trả `SignedUpload(url đầy đủ, key)`. Client sau đó tự `PUT` file tới URL này; **không** có kiểm tra nội dung trên đường này (xem rủi ro) |

### Ánh xạ lỗi (`SupabaseErrors`)

- **Status hiệu dụng:** nếu thân lỗi là JSON có `statusCode` (số hoặc chuỗi số) thì dùng nó, ngược lại dùng HTTP status. Lý do: Supabase có thể trả HTTP 400 kèm `statusCode` 404/409 trong body (chưa xác minh; smoke #2 chốt). Chỉ **đọc** `statusCode`, không chép `message`.
- Ánh xạ: 400→`BAD_REQUEST`, 401→`UNAUTHORIZED`, 403→`FORBIDDEN`, 404→`NOT_FOUND`, 409→`ALREADY_EXISTS`, 413→`PAYLOAD_TOO_LARGE`, 429→`RATE_LIMITED`, còn lại (kể cả 5xx và thân lỗi không phải JSON, ví dụ trang HTML 502)→`UPSTREAM_ERROR`.
- Bắt **mọi** `RestClientException` (gồm `RestClientResponseException`, `ResourceAccessException`, lỗi chuyển đổi body) **và** `RuntimeException` do `HttpClient` ném (ví dụ `IllegalArgumentException` của JDK về URI): tất cả thành `StorageException`; không loại nào lọt ra ngoài thô.
- I/O, timeout → `UNAVAILABLE`, `status=0`, và `outcomeUnknown=true` cho `put` và `delete`.
- 200 nhưng thân rỗng, không phải JSON, hoặc thiếu trường bắt buộc → `UNEXPECTED_RESPONSE`.
- `StorageException` **không** chứa `message` của Supabase, key, bucket, header hay nguyên nhân gốc; chỉ có `operation`, `reason`, `status`, `outcomeUnknown`, `ref` (SHA-256 rút gọn của `bucket/key`). Với một lỗi khó chẩn đoán, người vận hành dùng `ref` để đối chiếu và log của Supabase.
- Không retry. Javadoc `put`: sau timeout (`outcomeUnknown`) caller **không** retry cùng key (có thể gặp `ALREADY_EXISTS`) mà sinh key mới và `delete` key cũ best-effort. `delete` best-effort **không** được che exception gốc của caller (caller bắt, ghi, rồi ném lại lỗi ban đầu).

### Đăng ký bean (cập nhật `CommonStorageAutoConfiguration`)

```java
@Bean
@ConditionalOnMissingBean(ObjectStorage.class)
ObjectStorage objectStorage(SupabaseStorageProperties props) {
    return props.isConfigured() ? SupabaseObjectStorage.create(props) : new UnavailableObjectStorage();
}
```

`@ConditionalOnMissingBean` cho phép service thay bằng bản giả khi test. Spring tự gọi `close()` của `SupabaseObjectStorage` khi context đóng.

## Related Code Files

- Create: `shared/common-storage/src/main/java/com/ieltspath/commonstorage/supabase/{SupabaseObjectStorage,SupabaseErrors,SupabaseRestClientFactory}.java`
- Modify: `shared/common-storage/src/main/java/com/ieltspath/commonstorage/config/CommonStorageAutoConfiguration.java` (thêm bean)
- Create: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/supabase/SupabaseObjectStorageTest.java`
- Create: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/supabase/{SupabaseRestClientFactoryTest,SupabaseErrorsTest}.java`
- Modify: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/config/CommonStorageAutoConfigurationTest.java`

## Implementation Steps

1. Đọc báo cáo smoke; cập nhật giá trị mặc định trong bảng trên theo kết quả (header, dạng xóa, endpoint `exists`, factory). Nếu báo cáo chưa có: **dừng**.
2. Viết `SupabaseRestClientFactory.configure(builder, props)` + test header (hai dạng khóa) trước.
3. Viết `SupabaseErrors` + test riêng (bảng ánh xạ, body không phải JSON, `statusCode` trong body, không rò thông điệp).
4. Viết `SupabaseObjectStorage.publicUrl` và kiểm tra đầu vào chung (không cần mạng) + test.
5. Viết `put` + test. Sau đó `delete`, `exists`, `signedDownloadUrl`, `signedUploadUrl`, mỗi cái một vòng test–code.
6. `close()` và test đóng client.
7. Thêm bean vào auto-config; cập nhật `CommonStorageAutoConfigurationTest`.
8. Chạy `mvn -q -pl shared/common-storage test`.
9. Commit: `feat(storage): add the Supabase object storage client`.

## Kiểm thử

Dùng khóa giả cố định (`sb_secret_test-not-real`, và `eyJ.test.not-real` cho dạng legacy); URL giả `https://test-ref.supabase.co`.
`MockRestServiceServer.bindTo(builder)` với builder từ `configure(...)`. Mọi kịch bản lỗi dùng một key chứa `userId` giả và một thân lỗi **mô phỏng Supabase lặp lại key và khóa** trong `message`.

`SupabaseRestClientFactoryTest`:

- khóa `sb_secret_…`: có `apikey`, **không** có `Authorization`; khóa dạng JWT: có cả hai (theo kết quả smoke);
- mọi request tới đúng `https://test-ref.supabase.co/storage/v1/...`;
- hai client có timeout khác nhau (đọc từ factory, không cần mạng).

`SupabaseErrorsTest`: mỗi dòng của bảng ánh xạ; body `{"statusCode":"404",...}` kèm HTTP 400 → `NOT_FOUND`; body HTML → `UPSTREAM_ERROR`; body rỗng; `IllegalArgumentException` giả từ HttpClient → `StorageException`; `outcomeUnknown` đúng cho `put`/`delete` khi IO.

`SupabaseObjectStorageTest`:

- `put`: `POST`, URL `/storage/v1/object/avatars/<key>` (các `/` **không** bị mã hóa `%2F`), `Content-Type` đã chuẩn hóa (`audio/webm;codecs=opus` đi dạng `audio/webm`), `x-upsert: false`, body đúng từng byte; trả `StoredObject` đúng;
- `put` trùng key → `ALREADY_EXISTS`; timeout (response creator ném `IOException`) → `UNAVAILABLE`, `outcomeUnknown=true`;
- `delete`: đúng dạng gọi theo smoke; key không tồn tại không ném; IO → `outcomeUnknown=true`;
- `exists`: 200 → `true`; 404 → `false`; 500 → `StorageException`;
- `signedDownloadUrl`: body `{"expiresIn":300}` khi `ttl=5 phút`; `signedURL` → URL đầy đủ đúng; `ttl` 16 phút hoặc 0 → `MISCONFIGURED`, không gọi mạng;
- `signedUploadUrl`: không có `x-upsert`; `url` → `SignedUpload` URL đầy đủ và `key` đúng;
- `publicUrl`: chuỗi chính xác, không gọi mạng; key `listening/demo/numM.mp3` (chữ hoa) **được chấp nhận**;
- 200 thiếu trường / không phải JSON → `UNEXPECTED_RESPONSE`; 401/403/404/429/500 → `reason` tương ứng;
- đầu vào sai: key `a/../b` → `InvalidObjectKeyException` với message cố định; bucket `A` hoặc 2 ký tự, `content` rỗng, `contentType` rỗng → `MISCONFIGURED`; **không có request nào được gửi** (`server.verify()` không có expectation);
- **không rò khóa/key:** với mỗi kịch bản lỗi, `getMessage()` của exception không chứa khóa giả, `userId` trong key, key, bucket, hay chuỗi mà server mô phỏng lặp lại; `getCause()` là `null`;
- `close()` đóng cả hai client; gọi lại không ném.

`CommonStorageAutoConfigurationTest` (bổ sung):

- `enabled=true`, url và khóa hợp lệ → bean `ObjectStorage` là `SupabaseObjectStorage`; context đóng thì `close()` được gọi;
- service tự khai báo bean `ObjectStorage` → bean của module **không** được tạo;
- `toString()` của bean không chứa khóa.

## Success Criteria

- [ ] Báo cáo smoke đã có và các giá trị mặc định ở phase này khớp báo cáo.
- [ ] `mvn -q -pl shared/common-storage test` xanh; báo số test của phase và tổng.
- [ ] Có test ghim: `/` trong key không bị mã hóa; header đúng cho cả hai dạng khóa; không retry; không gắn cause; không theo redirect; key seed có chữ hoa qua được.
- [ ] Có test rò rỉ: server mô phỏng lặp lại key và khóa trong `message` mà `StorageException` không chứa chúng.
- [ ] Grep `shared/common-storage/src/main` không thấy `System.out`, `printStackTrace`, logger in key hoặc khóa.
- [ ] Không thay đổi file nào ngoài `shared/common-storage` (và `pom.xml` gốc đã đổi ở phase 1).


## Risk Assessment

| Rủi ro | Mitigation |
| --- | --- |
| Dạng xóa của SDK khác reference của Supabase | Smoke #3 chốt trước khi viết; mock test ghim dạng đã chọn; contract test (phase 4) chạy lại với project thật |
| Chọn `JdkClientHttpRequestFactory` mà không cần thiết | Chỉ chọn nếu smoke #8 chứng minh; nếu dùng xóa không body thì không bị ràng buộc |
| `put` timeout sau khi đã ghi | `outcomeUnknown`; caller dùng key mới; `delete` best-effort; contract/ops ghi nhận số object mồ côi nếu có |
| Phân loại lỗi chỉ theo HTTP status có thể sai (quirk 400 + `statusCode` trong body) | Dùng status hiệu dụng; smoke #2 xác nhận; test ghim cả hai nguồn |
| `setReadTimeout` của `JdkClientHttpRequestFactory` không áp đúng cho phần upload | Smoke #5 đo upload ~10 MB (đại diện mp3 admin; Speaking ≤ 5 MB); nếu timeout đứng sai chỗ thì đặt timeout ở `HttpRequest` qua interceptor/customizer và ghi lại lý do |
| Khóa dạng mới bị Supabase chặn nếu `User-Agent` giống trình duyệt | Client Java gửi UA riêng của JDK; không đặt UA giả trình duyệt |
| Supabase đổi định dạng phản hồi | Phản hồi được kiểm chặt (`UNEXPECTED_RESPONSE`) thay vì NPE; contract test bắt sớm |
| Signed upload qua mặt kiểm tra nội dung | Chỉ admin dùng; bucket phải có giới hạn MIME/dung lượng ở Dashboard (checklist rollout); `exists` trước khi lưu `media_reference`; sniff sau upload để plan content quyết định |
| Rollback | Xóa package `supabase/` và bean; phase 1–2 vẫn biên dịch (interface còn, bản `UnavailableObjectStorage` còn) |
