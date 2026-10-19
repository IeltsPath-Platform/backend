---
phase: 2
title: "Key and content policies"
status: pending
priority: P2
effort: "3h"
dependencies: [1]
---

# Phase 2: Chính sách key và nội dung file

## Overview

Phần thuần Java, không mạng, không Spring: (a) sinh và kiểm tra **object key** an toàn, (b) chuẩn hóa MIME, (c) kiểm tra file
upload có đúng loại khai báo không bằng **magic bytes**. Đây là lớp phòng thủ cho đường upload **đi qua backend** (avatar,
Speaking), vì Supabase chỉ kiểm tra MIME mà client tự khai.

## Requirements

- Chức năng: `ObjectKeys` sinh key `{prefix}/{userId}/{uuid}.{ext}` và kiểm tra key do bên ngoài đưa vào; `UploadPolicy` từ chối file rỗng, quá lớn, sai loại, hoặc nội dung không khớp loại khai báo.
- Phi chức năng: không phụ thuộc Spring; không ném `ArrayIndexOutOfBounds` với mảng ngắn; không bao giờ đưa nội dung file hay key vào thông báo lỗi; tất cả hàm tĩnh/thuần nên test không cần mock.

## Architecture

```
com.ieltspath.commonstorage
├── key/
│   ├── ObjectKeys.java
│   └── InvalidObjectKeyException.java   lỗi key không hợp lệ, thông báo cố định
└── upload/
    ├── ContentTypes.java     chuẩn hóa MIME + bảng alias + đuôi file
    ├── ContentSniffer.java   magic bytes -> MIME chuẩn
    └── UploadPolicy.java     record(allowedContentTypes, maxBytes), check(...)
```

### `ObjectKeys`

Hai mức kiểm tra, vì có hai loại key khác nhau: key **do module sinh ra** (chặt) và key **đã có sẵn** như `listening/demo/numM.mp3` (seed của content-service, có chữ hoa).

```java
public final class ObjectKeys {
    // --- Sinh key mới (chặt: chữ thường + UUID ngẫu nhiên) ---
    /** "{prefix}/{userId}/{uuid}.{ext}"  ví dụ speaking/0b1f…/9c2e….webm */
    public static String forUser(String prefix, UUID userId, String extension);
    /** "{prefix}/{uuid}.{ext}"  cho nội dung không thuộc user */
    public static String of(String prefix, String extension);

    // --- Kiểm key đưa vào BẤT KỲ phương thức nào của ObjectStorage (rộng: chỉ an toàn đường dẫn) ---
    /** Ném InvalidObjectKeyException nếu key không an toàn. Dùng cho delete/exists/publicUrl/ký URL. */
    public static void requireSafe(String key);

    // --- Kiểm key do client gửi lại (chặt + đúng chủ) ---
    /** Ném InvalidObjectKeyException nếu không phải key do forUser(prefix, userId, …) sinh ra. */
    public static void requireOwnedBy(String key, String prefix, UUID userId);
    /** Như trên nhưng trả true/false, không ném. */
    public static boolean isOwnedBy(String key, String prefix, UUID userId);
}
```

Quy tắc (một nơi duy nhất, mỗi quy tắc có test ghim):

- **Prefix:** 1–3 đoạn, mỗi đoạn khớp `[a-z0-9][a-z0-9-]{0,31}` (ví dụ `speaking`, `avatar`, `listening/practice`). Prefix sai → `IllegalArgumentException` **lúc sinh key** (lỗi lập trình, không phải đầu vào người dùng).
- **Key an toàn (`requireSafe`, rộng):** các đoạn cách nhau bởi `/`, mỗi đoạn khớp `[A-Za-z0-9][A-Za-z0-9._-]{0,99}`; không đoạn rỗng, không `..`, không bắt đầu bằng `/`, không đoạn kết thúc bằng `.`; tối đa 8 đoạn; tổng ≤ 300 ký tự (cột DB là `varchar(500)`). **Chữ hoa được phép** vì key đã seed có chữ hoa.
- **Key sinh mới (chặt):** chữ thường, đoạn cuối là `{uuid}.{ext}`; `ext` khớp `[a-z0-9]{1,5}`.
- **Đúng chủ (`requireOwnedBy`/`isOwnedBy`):** key phải an toàn, bắt đầu bằng `{prefix}/{userId}/`, và sau đó có **đúng một** đoạn khớp `{uuid}.{ext}`. Prefix nhiều đoạn được hỗ trợ (so khớp theo từng đoạn, không so chuỗi con).
- **`InvalidObjectKeyException`** kế thừa `IllegalArgumentException` (để nhóm lỗi đầu vào) nhưng thông báo **cố định** `object key is invalid`, **không** chứa giá trị. Lý do: các `GlobalExceptionHandler` hiện có (ví dụ `user-service`) trả `getMessage()` của `IllegalArgumentException` cho client với HTTP 400 và log stack trace; một thông báo chứa key sẽ lộ `userId`.
- Bộ ký tự đã bó (chữ hoa vẫn là ký tự an toàn trong URL), nên **không cần URL-encode** khi ghép vào đường dẫn REST và URL công khai.

Tên file luôn là UUID ngẫu nhiên do server sinh: không tin tên file client gửi, và key mới mỗi lần nên không bao giờ ghi đè (tránh CDN trả bản cũ).

`requireOwnedBy` dùng khi client gửi lại `audioReference` ở bước nộp bài; service gọi nó **cùng** `ObjectStorage.exists` (phase 1) trước khi lưu. Thư viện không ép được việc gọi; mục "Việc của plan service" trong `plan.md` biến nó thành điều kiện bắt buộc.

### `ContentTypes`

- `normalize(String declared)`: lowercase, **cắt tham số** sau `;` (`audio/webm;codecs=opus` → `audio/webm`), trim; null/rỗng → `null`.
- Alias về MIME chuẩn: `audio/x-wav`, `audio/wave`, `audio/vnd.wave` → `audio/wav`; `audio/mp3` → `audio/mpeg`; `audio/x-m4a`, `audio/m4a` → `audio/mp4`; `image/jpg` → `image/jpeg`.
- `extensionFor(String canonical)`: `image/png`→`png`, `image/jpeg`→`jpg`, `image/webp`→`webp`, `audio/webm`→`webm`, `audio/mp4`→`m4a`, `audio/ogg`→`ogg`, `audio/mpeg`→`mp3`, `audio/wav`→`wav`; loại khác → `IllegalArgumentException`.

### `ContentSniffer`

`Optional<String> detect(byte[] content)` đọc tối đa 12 byte đầu, trả MIME chuẩn:

| MIME | Dấu hiệu |
| --- | --- |
| `image/png` | `89 50 4E 47 0D 0A 1A 0A` |
| `image/jpeg` | `FF D8 FF` |
| `image/webp` | `"RIFF"` ở 0–3 **và** `"WEBP"` ở 8–11 |
| `audio/wav` | `"RIFF"` ở 0–3 **và** `"WAVE"` ở 8–11 |
| `audio/webm` | `1A 45 DF A3` (EBML) **và** chuỗi ASCII `webm` (DocType) xuất hiện trong 64 byte đầu; Matroska (`matroska`) không được nhận |
| `audio/mp4` | `"ftyp"` ở byte 4–7 **và** brand ở byte 8–11 thuộc `M4A `, `mp42`, `mp41`, `isom`, `iso2` |
| `audio/ogg` | `"OggS"` |
| `audio/mpeg` | `"ID3"`, hoặc byte 0 = `FF` và `(byte 1 & 0xE0) == 0xE0` |

Không có `image/svg+xml`: SVG là văn bản có thể chứa script, **không** cho upload qua backend (bucket `avatars` cũng không bật loại này).
Giới hạn đã biết (ghi trong Javadoc): chỉ 12–64 byte đầu được xem, nên file polyglot, MP4 có hình đội lốt `audio/mp4`, hay byte `FF Ex` ngẫu nhiên vẫn có thể qua.
Sniffer chặn được **giả mạo rõ ràng** (ảnh đội lốt audio, văn bản đội lốt ảnh, Matroska video đội lốt webm), không phải bộ giải mã đầy đủ; đừng coi nó là cổng bảo mật duy nhất.
Ảnh còn mang EXIF (GPS, thông tin máy): module **không** xử lý; plan avatar phải xóa EXIF trước khi ghi lên bucket public (checklist trong `plan.md`).

### `UploadPolicy`

```java
public record UploadPolicy(Set<String> allowedContentTypes, long maxBytes) {
    public static UploadPolicy of(long maxBytes, String... allowedContentTypes);
    /** Kiểm tra sớm theo Content-Length, trước khi đọc body. */
    public void requireSize(long sizeBytes);
    /** Đọc tối đa maxBytes+1 byte từ stream; vượt maxBytes thì ném TOO_LARGE. Không tin Content-Length. */
    public byte[] readBounded(InputStream in) throws IOException;
    /** Trả MIME chuẩn để lưu. Ném UploadRejectedException. */
    public String check(byte[] content, String declaredContentType);
}
```

Thứ tự `check` (dừng ở lỗi đầu tiên; mỗi lỗi một `Reason`):

1. `content` null hoặc dài 0 → `EMPTY`.
2. `content.length > maxBytes` → `TOO_LARGE`.
3. `ContentTypes.normalize(declared)` rỗng hoặc không thuộc `allowedContentTypes` → `TYPE_NOT_ALLOWED`.
4. `ContentSniffer.detect(content)` rỗng hoặc khác MIME đã chuẩn hóa → `CONTENT_MISMATCH`.
5. Trả MIME chuẩn.

`of(...)` chuẩn hóa tập `allowed` qua `ContentTypes` và **từ chối wildcard** (`audio/*`, `*/*`) bằng `IllegalArgumentException`: muốn cho loại nào phải nêu rõ.
`readBounded` đọc bằng `readNBytes(maxBytes + 1)`: kể cả khi header `Content-Length` bị nói dối hay body chunked, bộ nhớ của **riêng bước này** bị chặn ở `maxBytes + 1`. Nó **không** ngăn Spring multipart đã đệm body trước đó; vì vậy checklist ở `plan.md` buộc service đặt `max-file-size`/`max-request-size` và giới hạn đồng thời.
Module **không** chứa policy dựng sẵn cho avatar hay Speaking: đó là nghiệp vụ của user-service/assessment-service (quy tắc số 1 trong `plan.md`).

## Related Code Files

- Create: `shared/common-storage/src/main/java/com/ieltspath/commonstorage/key/{ObjectKeys,InvalidObjectKeyException}.java`
- Create: `shared/common-storage/src/main/java/com/ieltspath/commonstorage/upload/{ContentTypes,ContentSniffer,UploadPolicy}.java`
- Create: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/key/ObjectKeysTest.java`
- Create: `shared/common-storage/src/test/java/com/ieltspath/commonstorage/upload/{ContentTypesTest,ContentSnifferTest,UploadPolicyTest}.java`
- Modify: không (dùng `UploadRejectedException` đã tạo ở phase 1)

## Implementation Steps

1. Viết `ContentTypes` + `ContentTypesTest`: bảng alias, cắt tham số, `extensionFor` cho đủ 8 loại và loại lạ.
2. Viết `ContentSniffer` + `ContentSnifferTest`. Dựng mảng byte trong test bằng helper (không thêm file nhị phân vào repo): header PNG 8 byte, `FF D8 FF E0`, `RIFF....WEBP`, `RIFF....WAVE`, `1A 45 DF A3`, `00 00 00 18 'f' 't' 'y' 'p'`, `OggS`, `ID3`, `FF FB`.
3. Viết `UploadPolicy` + `UploadPolicyTest`.
4. Viết `ObjectKeys` + `ObjectKeysTest`.
5. Chạy `mvn -q -pl shared/common-storage test`.
6. Commit: `feat(storage): add object key and upload content policies`.

## Kiểm thử

`ObjectKeysTest`:

- `forUser("speaking", userId, "webm")` khớp regex `^speaking/<userId>/[0-9a-f-]{36}\.webm$`; hai lần gọi cho hai key khác nhau; prefix `listening/practice` (2 đoạn) hợp lệ, prefix `Speaking`, `a//b`, 4 đoạn, đoạn 33 ký tự → `IllegalArgumentException`;
- `requireSafe` **chấp nhận** `listening/demo/numM.mp3`, `listening/practice/pt-listening-01-s1.mp3` và key do `forUser`/`of` sinh;
- `requireSafe` từ chối: `""`, `null`, `"/a/b"`, `"a//b"`, `"a/../b"`, `"a/b."`, `"a/b c"`, `"a/é"`, `"a/%2e%2e/b"`, `"a/b?x"`, 9 đoạn, 301 ký tự, đoạn dài 101 ký tự;
- `requireOwnedBy`/`isOwnedBy`: đúng chủ → qua/`true`; sai `userId`, sai `prefix`, key của người khác, key thêm một đoạn (`speaking/<id>/x/y.webm`), tên file không phải UUID (`speaking/<id>/a.webm`), prefix chỉ khớp một phần (`speaking-x/<id>/…`), key không an toàn → ném/`false`; `isOwnedBy` **không bao giờ ném**;
- thông báo của `InvalidObjectKeyException` luôn đúng bằng `object key is invalid` và **không** chứa key, `userId` hay prefix (test với key chứa userId giả);
- `extension` không hợp lệ (`"WEBM"`, `"we bm"`, `"toolong"`) → `IllegalArgumentException`.

`ContentTypesTest`: `audio/webm;codecs=opus` → `audio/webm`; `Audio/WebM ` → `audio/webm`; alias; `null`/rỗng → `null`.

`ContentSnifferTest`:

- mỗi định dạng trong bảng nhận đúng MIME; `ftyp` với brand lạ (`qt  `) và EBML với DocType `matroska` → `Optional.empty()`;
- `RIFF` + `WEBP` ≠ `RIFF` + `WAVE` (hai loại phân biệt đúng);
- mảng rỗng, 1 byte, 3 byte, 7 byte → `Optional.empty()`, **không ném** `ArrayIndexOutOfBounds`;
- văn bản thường (`<svg …>`, `GIF89a`, `%PDF`) → `Optional.empty()`.

`UploadPolicyTest` (policy mẫu `of(10, "image/png","image/jpeg","image/webp")`, kích thước nhỏ để test nhanh):

- file rỗng → `EMPTY`; 11 byte → `TOO_LARGE`; khai `audio/webm` → `TYPE_NOT_ALLOWED`; khai `image/png` nhưng nội dung JPEG → `CONTENT_MISMATCH`; khai `image/png;charset=x` nội dung PNG → trả `image/png`;
- `requireSize(11)` ném `TOO_LARGE`; `requireSize(10)` qua;
- `readBounded`: stream 10 byte qua; stream 11 byte và stream vô hạn (generator) → `TOO_LARGE` mà **không** đọc quá `maxBytes+1` byte (đếm byte đã đọc trong test);
- `of(..., "audio/*")` và `of(..., "*/*")` → `IllegalArgumentException`;
- thông báo của mọi `UploadRejectedException` **không** chứa byte nội dung hay tên file; `getReason()` đúng.

## Success Criteria

- [ ] `mvn -q -pl shared/common-storage test` xanh; báo số test của phase.
- [ ] Mọi quy tắc key ở trên có một test ghim; không dùng regex ở chỗ khác ngoài `ObjectKeys`.
- [ ] Key seed cũ có chữ hoa (`listening/demo/numM.mp3`) qua `requireSafe`; thông báo lỗi key luôn cố định.
- [ ] Không có class nào trong `key/` hay `upload/` import `org.springframework.*`.
- [ ] `ContentSniffer` không ném với mảng ngắn (test có mảng 0–7 byte).
- [ ] Không file nhị phân nào được thêm vào repo.

## Risk Assessment

| Rủi ro | Mitigation |
| --- | --- |
| Magic bytes cho MP3 có thể khớp nhầm byte ngẫu nhiên (`FF E…`) | Chấp nhận: chỉ dùng để chặn giả mạo rõ ràng, luôn kết hợp với MIME khai báo và giới hạn dung lượng |
| Chặn nhầm file hợp lệ (ví dụ MP4 có box lạ trước `ftyp`) | Chỉ nhận khi `ftyp` ở byte 4; ghi rõ giới hạn trong Javadoc. MediaRecorder của Safari ghi `ftyp` ngay đầu; nếu thực tế khác, nới ở service bằng test thật |
| Key cũ (chữ hoa) bị từ chối | Hai mức kiểm tra: `requireSafe` rộng cho key có sẵn, chặt chỉ cho key sinh mới và `isOwnedBy` |
| Service quên gọi `isOwnedBy` khi nhận key từ client | Ghi rõ trong Javadoc; nằm trong checklist của plan endpoint ở từng service, không ép được ở thư viện |
| Rollback | Xóa thư mục `key/` và `upload/`; phase 3 chưa tồn tại nên chưa ai dùng |
