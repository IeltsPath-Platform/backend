---
phase: 1
title: "Module skeleton and config"
status: pending
priority: P2
effort: "2h"
dependencies: []
---

# Phase 1: Khung module và cấu hình

## Overview

Tạo module Maven `shared/common-storage`, thêm vào reactor, và dựng phần **không cần mạng**: interface `ObjectStorage`,
record dữ liệu, `StorageException` (có enum lý do), `SupabaseStorageProperties`, auto-config, và bản `UnavailableObjectStorage`.
Chưa có implementation gọi Supabase (phase 3). Cuối phase là một **smoke thật** với Supabase (chạy tay, không phải test tự
động) để chốt các điểm chưa xác minh **trước khi** viết client.

<!-- Updated: Validation Session 2 - smoke chạy trên project hiện tại, trong bucket contract-test-smoke -->
<!-- Updated: Validation Session 1 - smoke #9 (nhiều secret key); project test riêng là điều kiện tiên quyết; hàng #5 là đại diện mp3 admin -->

## Requirements

- Chức năng: module build được, nằm trong reactor, có interface công khai ổn định cho phase 2–3; properties fail nhanh khi cấu hình sai.
- Phi chức năng: Java 21, Spring Boot 3.5.14 (kế thừa từ `code-base`), không Lombok, không dependency ngoài danh sách dưới đây; khóa không lộ ở `toString()`.

## Architecture

Package gốc `com.ieltspath.commonstorage` (cùng kiểu `com.ieltspath.commonsecurity`).

```
com.ieltspath.commonstorage
├── ObjectStorage.java           interface, cổng duy nhất service gọi
├── StoredObject.java            record(bucket, key, contentType, sizeBytes)
├── SignedUpload.java            record(url, key)
├── StorageException.java        lỗi hạ tầng: operation, reason (enum), status, outcomeUnknown, ref
├── UploadRejectedException.java lỗi đầu vào: file bị từ chối (phase 2 dùng)
├── UnavailableObjectStorage.java bản cài khi bật cờ mà chưa cấp biến: mọi lệnh ném StorageException(UNAVAILABLE)
└── config/
    ├── SupabaseStorageProperties.java
    └── CommonStorageAutoConfiguration.java
resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### `ObjectStorage`

```java
public interface ObjectStorage {
    /** Ghi object mới. Không ghi đè: key đã tồn tại thì ném StorageException(ALREADY_EXISTS). */
    StoredObject put(String bucket, String key, byte[] content, String contentType);

    /** Xóa object. Idempotent: key không tồn tại không phải lỗi. */
    void delete(String bucket, String key);

    /** Object có tồn tại không. Gọi trước khi tin một key do client gửi lại. */
    boolean exists(String bucket, String key);

    /** Không gọi mạng. Chỉ đúng với bucket public. */
    String publicUrl(String bucket, String key);

    /** URL tải có hạn cho bucket private. ttl trong [1 giây, 15 phút]. Coi URL như bí mật: không log. */
    String signedDownloadUrl(String bucket, String key, Duration ttl);

    /** URL để client tự PUT file. Thời hạn do Supabase quyết định, không truyền được. */
    SignedUpload signedUploadUrl(String bucket, String key);
}
```

Quyết định thiết kế:

- `put` nhận `byte[]`, **không** `InputStream`: ba chỗ dùng qua backend đều ≤ vài MB và đã đọc hết vào bộ nhớ để kiểm tra magic bytes. Streaming thêm độ phức tạp (Content-Length, retry) mà chưa ai cần (YAGNI). Bộ nhớ được bảo vệ bằng `UploadPolicy.readBounded` (phase 2) và checklist của service, không bằng `put`.
- Không có tham số `upsert`: ghi đè gây CDN trả bản cũ. Muốn đổi file thì sinh key mới rồi `delete` key cũ.
- `bucket` luôn là tham số. Module không biết `avatars` hay `speaking-recordings`.
- `exists` có mặt vì thư viện không thể ép service kiểm tra: một key do client gửi lại chỉ có giá trị khi file thật sự có (nộp bài có `audioReference` trỏ tới file chưa upload là lỗi thật).
- TTL tải xuống tối đa **15 phút**: token nằm trong query string, không thu hồi được.

### `StorageException`

`RuntimeException` (không phải `IllegalArgumentException`/`IllegalStateException`: các `GlobalExceptionHandler` hiện có map hai loại đó thành HTTP 400 và trả `getMessage()` cho client; lỗi hạ tầng phải đi đường 500).

- `enum Reason { BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, ALREADY_EXISTS, PAYLOAD_TOO_LARGE, RATE_LIMITED, UPSTREAM_ERROR, UNAVAILABLE, MISCONFIGURED, UNEXPECTED_RESPONSE }`.
- Trường: `operation` (`put`, `delete`, `exists`, `sign`, `signUpload`), `reason`, `status` (mã HTTP hiệu dụng, 0 nếu không có), `outcomeUnknown` (true khi timeout/IO xảy ra giữa chừng ở `put`/`delete`: object có thể đã được ghi/xóa), `ref`.
- `ref` = 8 ký tự hex đầu của SHA-256 của `bucket + "/" + key` (`StorageException.ref(bucket, key)`): đủ để đối chiếu log với một object cụ thể mà không lộ key (key chứa `userId`).
- `getMessage()` có dạng cố định `storage put failed: reason=ALREADY_EXISTS status=409 ref=1a2b3c4d`. **Không** chép `message` của Supabase, không có key, bucket, header, body, hay nguyên nhân gốc.
- Không gắn `cause` là exception của `RestClient`/JDK (message của chúng chứa URL đầy đủ có `userId`).

### `SupabaseStorageProperties`

```java
@ConfigurationProperties(prefix = "app.storage.supabase")
public record SupabaseStorageProperties(
        String url, String secretKey,
        Duration connectTimeout, Duration readTimeout, Duration uploadTimeout) { ... }
```

Compact constructor:

- `url` và `secretKey` **đều trống** → hợp lệ, `isConfigured() == false` (service bật cờ nhưng chưa cấp biến vẫn khởi động; xem auto-config).
- **Chỉ một** trong hai trống → `IllegalArgumentException("app.storage.supabase.url and app.storage.supabase.secret-key must be configured together")`. Thông báo không chứa giá trị.
- Có cả hai: parse `url` bằng `java.net.URI` (**không** so khớp chuỗi): scheme `https`; hoặc `http` chỉ khi `getHost()` là `localhost`, `127.0.0.1` hoặc `::1`. Từ chối nếu có userinfo (`http://localhost@evil.com` có host `evil.com` và có userinfo), query, fragment, hoặc path khác rỗng/`/`. Bỏ `/` cuối. Từ chối nếu chứa `/storage/v1`.
- Timeout phải > 0; mặc định `connectTimeout` 3 giây, `readTimeout` 10 giây (thao tác nhỏ), `uploadTimeout` 120 giây (chỉ `put`).
- `toString()` ghi đè: `SupabaseStorageProperties[url=…, secretKey=***]`. Đây là chỗ dễ lộ khóa nhất vì Spring có thể in properties khi báo lỗi binding.
- Thông báo lỗi của mọi nhánh trên **không chứa giá trị khóa** kể cả khi khóa sai định dạng.

### `CommonStorageAutoConfiguration` và `UnavailableObjectStorage`

- `@AutoConfiguration`, `@ConditionalOnClass(RestClient.class)`, `@ConditionalOnProperty(prefix = "app.storage", name = "enabled", havingValue = "true")` (mặc định **tắt**), `@EnableConfigurationProperties(SupabaseStorageProperties.class)`.
- Một bean `ObjectStorage` (`@ConditionalOnMissingBean`): phase 1 trả `UnavailableObjectStorage` khi `!props.isConfigured()`; phase 3 trả `SupabaseObjectStorage` khi đã cấu hình.
- `UnavailableObjectStorage`: mọi phương thức ném `StorageException(reason=UNAVAILABLE, status=0)` (trừ `publicUrl`, vốn cần url, nên cũng ném). Lý do thiết kế: repo dùng `${X:}` và hạ cấp nhẹ nhàng cho tích hợp ngoài (`config-repo/assessment-service.yaml`), và compose đặt `restart: unless-stopped`, nên một biến thiếu **không** được làm cả service (ví dụ login ở user-service) crash-loop.
- Đăng ký trong `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (một dòng, giống `common-security`).

### Dependency của module (và chỉ những cái này)

| Artifact | Scope | Lý do |
| --- | --- | --- |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | `@AutoConfiguration`, `@ConditionalOn*` |
| `org.springframework:spring-web` | compile | `RestClient` |
| `com.fasterxml.jackson.core:jackson-databind` | compile | đọc JSON phản hồi của Supabase |
| `org.springframework.boot:spring-boot-starter-test` | test | JUnit 5, AssertJ, Mockito, `ApplicationContextRunner`, `MockRestServiceServer` (có trong `spring-test`) |

Không thêm `spring-boot-starter-web`, `spring-boot-starter-validation`, Lombok, hay SDK Supabase.

### Smoke thật (cuối phase 1, bắt buộc trước phase 3)

Mục đích: chốt các điểm chưa xác minh **trước khi** client được viết quanh chúng. **Điều kiện tiên quyết (người dùng chốt lại 2026-10-19, thay quyết định "project riêng" trước đó):** chạy trên **project Supabase hiện tại**, người dùng tự xóa phần test sau. Làm tay, **chỉ trong** một bucket private tạo mới tên `contract-test-smoke`; **không** đọc, ghi hay xóa object ở `media`, `avatars`, `speaking-recordings`. Nên tạo một **secret key tạm** chỉ để test rồi thu hồi (cũng trả lời smoke #9) thay vì dùng khóa chính của ứng dụng, vì khóa này bỏ qua RLS. Khóa lấy từ **file cục bộ nằm
ngoài repo**, không gõ vào dòng lệnh (lịch sử shell lưu lại; trên máy này `.bash_history` có thật).

| # | Câu hỏi | Cách thử | Quyết định phụ thuộc |
| --- | --- | --- |
| 1 | Khóa của dự án là `sb_secret_…` hay JWT legacy; header nào được? | Gọi `GET /storage/v1/bucket` chỉ với `apikey`, rồi chỉ với `Authorization: Bearer` | Nhánh header của `SupabaseRestClientFactory` |
| 2 | Ghi trùng key trả gì? | `POST` hai lần cùng key; ghi HTTP status **và** `statusCode` trong body | Phân loại lỗi theo status hiệu dụng (body `statusCode` nếu có) |
| 3 | Dạng xóa nào chạy; key không tồn tại trả gì? | `DELETE /object/{bucket}` body `{"prefixes":[…]}` và `DELETE /object/{bucket}/{key}` | Cách `delete` |
| 4 | Kiểm tra tồn tại bằng endpoint nào? | `HEAD /object/{bucket}/{key}` và `GET /object/info/{bucket}/{key}`; ghi mã khi có/không có | Cách `exists` |
| 5 | Token signed upload sống bao lâu? PUT cần header nào? | Giải mã `token` (JWT) lấy `exp`; `PUT` file nhỏ với/không `content-type`, `x-upsert`; rồi một file ~10 MB (đại diện mp3 của admin, **không** phải Speaking vốn ≤ 5 MB), ghi thời gian (để chọn `upload-timeout`) | `signedUploadUrl`, tài liệu |
| 6 | Thông báo lỗi của Supabase có lặp lại path/key không? | Gửi key có chuỗi giả giống userId vào thao tác lỗi | Xác nhận quyết định bỏ message upstream |
| 7 | API cho đọc `allowed_mime_types`, `file_size_limit` của bucket không? | `GET /bucket/{id}` trên bucket test | Mục kiểm giới hạn bucket ở checklist rollout |
| 8 | `DELETE` có body chạy được qua `java.net.http.HttpClient` không? | Một test nháp ngoài repo (jshell) | Chọn `JdkClientHttpRequestFactory` (phase 3) |
| 9 | Dashboard có cho tạo nhiều secret key (mỗi service một khóa) không? | Xem mục API Keys của project test; chỉ ghi có/không và cách thu hồi từng khóa | Số khóa mỗi service (câu hỏi mở 5 ở `plan.md`) |

Kết quả ghi vào `plans/261019-0813-common-storage-module/reports/smoke-<timestamp>-supabase-storage-contract-report.md`: **không** chứa khóa,
không chứa key thật, không dán body phản hồi nguyên văn nếu có thể chứa token. Nếu chưa tạo được bucket test hoặc khóa test, hoặc chưa chạy được:
**phase 3 không bắt đầu**; phase 1–2 vẫn làm bình thường.

## Related Code Files

- Create: `shared/common-storage/pom.xml`
- Create: `shared/common-storage/src/main/java/com/ieltspath/commonstorage/{ObjectStorage,StoredObject,SignedUpload,StorageException,UploadRejectedException,UnavailableObjectStorage}.java`
- Create: `shared/common-storage/src/main/java/com/ieltspath/commonstorage/config/{SupabaseStorageProperties,CommonStorageAutoConfiguration}.java`
- Create: `shared/common-storage/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/{StorageExceptionTest,UnavailableObjectStorageTest}.java`
- Create: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/config/{SupabaseStoragePropertiesTest,CommonStorageAutoConfigurationTest}.java`
- Modify: `pom.xml` (gốc): thêm `<module>shared/common-storage</module>` ngay sau `shared/common-security`
- Create: `plans/261019-0813-common-storage-module/reports/smoke-<timestamp>-supabase-storage-contract-report.md` (kết quả smoke, sau khi chạy)
- Không copy: `CommonSecurityApplication.java` và `application.yaml` của `common-security` (đây là thư viện, không có `main`)

## Implementation Steps

1. Nhánh nền: tạo `feat/common-storage` từ `feat/common-events` (nơi `shared/common-events` đang có), hoặc từ `main` sau khi `common-events` merge. **Không** tách từ `main` hiện tại (thiếu `common-events` trong `<modules>`). Commit các sửa chưa commit của plan Supabase cũ cùng thư mục plan này (`docs(storage): plan the common-storage module`).
2. Tạo `shared/common-storage/pom.xml`: parent `com.ieltspath:code-base:1.0-SNAPSHOT` (`relativePath ../../pom.xml`), `artifactId common-storage`, `name common-storage`. Plugin: `maven-compiler-plugin 3.14.0` (source/target 21) và `maven-surefire-plugin 3.5.3`, cùng phiên bản với `common-events`. Không khai báo annotation processor Lombok (module không dùng Lombok; `common-events` và `common-security` có khai báo, ta không cần). **Không** khai báo `spring-boot-maven-plugin`.
3. Thêm `<module>shared/common-storage</module>` vào `pom.xml` gốc. Chạy `mvn -q -pl shared/common-storage -am compile` để chắc reactor nhận.
4. Viết `StoredObject`, `SignedUpload` (record); `StorageException` (enum `Reason`, `ref(bucket,key)`, message cố định) kèm `StorageExceptionTest` trước; `UploadRejectedException` (enum `Reason`: `EMPTY, TOO_LARGE, TYPE_NOT_ALLOWED, CONTENT_MISMATCH`; thông báo không chứa nội dung file).
5. Viết `ObjectStorage` với Javadoc như trên, rồi `UnavailableObjectStorage` + test.
6. Viết `SupabaseStoragePropertiesTest` rồi `SupabaseStorageProperties` (record, compact constructor, `isConfigured`, `toString`).
7. Viết `CommonStorageAutoConfiguration` và file `.imports`; viết `CommonStorageAutoConfigurationTest`.
8. Chạy `mvn -q -pl shared/common-storage test`. (Không cần build service nào: chưa service nào phụ thuộc module này, nên một lệnh `-pl services/... -am compile` không kiểm được gì về module mới.)
9. Commit: `feat(storage): add the common-storage module skeleton`.
10. **Smoke thật** theo bảng ở trên (trong bucket `contract-test-smoke` của project hiện tại). Ghi báo cáo. Cập nhật mục "Hợp đồng REST đã xác minh" của `plan.md` và các phase 3–4 nếu kết quả khác giả định.

## Kiểm thử

`StorageExceptionTest`: `getMessage()` đúng định dạng cố định cho mỗi `Reason`; không chứa key, bucket hay chuỗi bất kỳ truyền vào ngoài `ref`; `ref("avatars","a/b")` ổn định, 8 ký tự hex, và khác với `ref("avatars","a/c")`; `cause` luôn `null`.

`UnavailableObjectStorageTest`: mỗi trong 6 phương thức ném `StorageException` với `reason=UNAVAILABLE`.

`SupabaseStoragePropertiesTest`:

- cả hai trống (null, rỗng, khoảng trắng) → hợp lệ, `isConfigured()=false`;
- chỉ `url` có hoặc chỉ `secretKey` có → ném đúng thông báo "must be configured together", **không** chứa giá trị khóa;
- `http://example.com` → từ chối; `http://localhost:54321` → chấp nhận;
- `http://localhost@evil.com`, `http://localhost.evil.com`, `https://user:pw@abc.supabase.co`, `https://abc.supabase.co/path`, `https://abc.supabase.co?x=1`, `https://abc.supabase.co/#f` → từ chối;
- `https://abc.supabase.co/` → chuẩn hóa `https://abc.supabase.co`; `https://abc.supabase.co/storage/v1` → từ chối;
- khóa có ký tự lạ/quá dài vẫn **không** làm thông báo lỗi chứa khóa;
- `toString()` không chứa khóa giả `test-key-not-real`;
- timeout bằng 0 hoặc âm → từ chối; mặc định 3s / 10s / 120s khi không đặt.

`CommonStorageAutoConfigurationTest` (`ApplicationContextRunner`, `AutoConfigurations.of(CommonStorageAutoConfiguration.class)`):

- không đặt `app.storage.enabled` → context khởi động, **không** có bean `ObjectStorage` hay `SupabaseStorageProperties`;
- `enabled=true`, cả hai trống → có bean `ObjectStorage` kiểu `UnavailableObjectStorage`, context khởi động được;
- `enabled=true`, chỉ một trống hoặc url sai → context **thất bại**, thông báo nêu tên property, không chứa giá trị;
- service tự khai báo bean `ObjectStorage` → bean của module không được tạo;
- file `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` có trên classpath và liệt kê `CommonStorageAutoConfiguration` (đọc resource, để việc phát hiện tự động không chỉ đúng nhờ `AutoConfigurations.of`).

## Success Criteria

- [ ] `mvn -q -pl shared/common-storage test` xanh; ghi số test.
- [ ] Module có trong `<modules>` của pom gốc; `mvn -q -pl shared/common-storage -am compile` thành công; không có `CommonSecurityApplication`/`application.yaml` sao chép.
- [ ] Không có dependency ngoài bảng ở trên (`mvn -pl shared/common-storage dependency:tree` để kiểm).
- [ ] Test chứng minh `toString()` và thông báo của mọi exception/properties không chứa khóa hay key.
- [ ] `feat/common-storage` tách đúng nền; plan Supabase cũ đã được commit.
- [ ] Báo cáo smoke có mặt trong `reports/` (không chứa khóa); câu hỏi mở 1, 2, 3, 6 đã có câu trả lời hoặc ghi "chưa xác minh được". Nếu chưa có: phase 3 chưa bắt đầu.

## Risk Assessment

| Rủi ro | Mitigation |
| --- | --- |
| IDE đang chạy ghi class vào `target/` gây `ClassFormatError` | Build trong bản copy ngoài workspace hoặc dừng IDE (đã gặp ở `common-events`) |
| `@ConditionalOnClass(RestClient.class)` ẩn lỗi khi thiếu `spring-web` | Module khai báo `spring-web` compile nên luôn có; test auto-config chạy trên classpath thật |
| Đặt tên property sai làm service khác khó đồng bộ | Chốt `app.storage.enabled` và `app.storage.supabase.*` ở plan này; đổi sau phải sửa mọi config-repo |
| Smoke chạm nhầm dữ liệu thật (cùng project với ứng dụng, khóa toàn quyền bỏ qua RLS) | Chỉ thao tác trong bucket `contract-test-smoke`; ưu tiên secret key tạm rồi thu hồi; khóa đọc từ file ngoài repo, không lấy từ `.env`; không gọi thao tác nào lên `media`/`avatars`/`speaking-recordings` |
| Chưa có bucket test hoặc khóa test | Phase 3 bị chặn có chủ đích; phase 1–2 vẫn giao được giá trị (phần thuần Java) |
| Rollback | Xóa dòng `<module>` và thư mục module; không service nào phụ thuộc ở phase này |
