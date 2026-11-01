---
phase: 1
title: "Sửa cấu hình client từ vựng của content"
status: pending
priority: P1
effort: "0.5-1 ngày"
dependencies: []
---

# Phase 01: Sửa cấu hình client từ vựng của content

## Overview

`LibraryVocabularySenseClient` (content-service) đọc khóa `library.content.base-url` với mặc định `http://localhost:8082`, là cổng của chính content-service. Khóa này chỉ được cấu hình cho library-service (để library trỏ tới content). Kết quả dự kiến: lời gọi `/api/content/vocabulary/senses` và `/api/content/admin/vocabulary/bulk` từ content rơi vào chính content-service và lỗi. Phase này xác nhận lỗi rồi sửa.

## Requirements

- Xác nhận lỗi bằng bằng chứng chạy được (xem Tests), không chỉ đọc code.
- Sau khi sửa: content-service có khóa cấu hình riêng `content.library.base-url`, mặc định `${LIBRARY_SERVICE_URL:http://localhost:8081}` trong `content-service.yaml`, và `docker-compose.yml` đưa `LIBRARY_SERVICE_URL: http://library-service:8081` vào content-service.
- Không đổi hành vi lỗi của client (`VocabularyServiceUnavailableException`, `VocabularyRejectedException`).

## Tests first (red)

1. **Chứng minh lỗi.** Nếu Docker chạy: dựng stack (`docker compose up -d --build`), đăng nhập tài khoản `CONTENT_AUTHOR` và gọi luồng dùng từ vựng của content (duyệt course draft có từ vựng, hoặc lesson có vocabulary); ghi lại phản hồi và log cho thấy request đi vào content-service chứ không tới library. Nếu không dựng được stack, viết một test cấu hình (cùng kiểu `LearningSupportRouteConfigTest` đọc YAML/Compose) khẳng định `content-service.yaml` không có khối `library` hoặc khóa cho client và `docker-compose.yml` không có URL library cho content-service: test này đỏ trước khi sửa. Ghi rõ trong báo cáo đã dùng cách nào.
2. **Test hồi quy sau sửa** (đỏ trước, xanh sau): đọc `infra/config-server/config-repo/content-service.yaml` và `docker-compose.yml`, khẳng định khóa `content.library.base-url` tồn tại, trỏ `LIBRARY_SERVICE_URL`, và block `content-service` trong Compose truyền `LIBRARY_SERVICE_URL` tới `http://library-service:8081`.
3. Cập nhật `LibraryVocabularySenseClientTest` để dùng khóa mới (constructor nhận base-url trực tiếp thì không cần đổi).

## Related Code Files

- Sửa: `content-service/.../infrastructure/client/LibraryVocabularySenseClient.java` (`@Value("${content.library.base-url:http://localhost:8081}")`).
- Sửa (cấu hình tập trung, commit riêng): `infra/config-server/config-repo/content-service.yaml` (thêm `content.library.base-url: ${LIBRARY_SERVICE_URL:http://localhost:8081}`; khối `content:` đã có ở dòng 18, thêm vào đó), `docker-compose.yml` (block `content-service`, thêm `LIBRARY_SERVICE_URL: http://library-service:8081`; library-service chạy cổng 8081).
- Sửa tài liệu: README hoặc bảng biến môi trường của content-service nếu có liệt kê URL; `docs/system-architecture.md` bảng phụ thuộc (thêm hàng content → library).

## Implementation Steps

1. Viết bằng chứng và test đỏ (mục Tests 1, 2).
2. Đổi khóa, thêm cấu hình và biến Compose.
3. Chạy `mvn -B -ntp -pl services/content-service -am test` và `docker compose config --quiet`.
4. Nếu Docker chạy: lặp lại luồng từ vựng và ghi kết quả đã sửa.

## Success Criteria

- [ ] Có bằng chứng lỗi trước khi sửa (log, hoặc test cấu hình đỏ).
- [ ] Test hồi quy xanh; test content-service và `docker compose config --quiet` xanh.
- [ ] Bảng phụ thuộc trong `docs/system-architecture.md` có hàng content → library.

## Risk Assessment

- Sửa Docker và config tập trung: commit riêng, nêu lý do trong mô tả. Rollback: revert commit; hành vi trở lại như cũ (lời gọi tự trỏ về chính nó).
- Sau phase này chiều content → library thật sự hoạt động; cùng lúc library còn gọi ngược content cho tới phase 02. Hai lời gọi có timeout (2s kết nối, 5s đọc) nên không treo vô hạn; phase 02 nên theo sau sớm.
- Chưa chạy thử trước khi sửa là rủi ro nằm ở bước 1: nếu bằng chứng cho thấy không phải lỗi (ví dụ có cấu hình ở nơi khác), dừng và báo cáo.
