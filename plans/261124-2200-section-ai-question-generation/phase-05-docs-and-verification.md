# Phase 05 - Tài liệu và kiểm tra cuối

Phủ: E23, E41 và tổng kiểm.

## Tài liệu (cùng PR với code của phase tương ứng)

- `docs/contracts/ai-drafts-v1.md`: kind và status mới, route, mã lỗi, hết hạn, claim/attempt; sửa mô tả thử lại nếu phase 01 thấy sai.
- `docs/contracts/content-package-authoring-v1.md`: `QUESTION_NOT_PUBLISHED`, `publish-drafts`, quy tắc mục đích câu.
- `docs/ai-policy.md`: prompt theo phần có nguồn và câu của người soạn; không có dữ liệu học viên; luôn qua duyệt.
- `docs/system-architecture.md`: bảng scheduler. `infra/config-server/config-repo`: khóa cấu hình mới và tier Gateway.

## Triển khai

1. Frontend trước: hiểu kind/status mới, nút ẩn khi API trả `404`.
2. Dừng mọi instance content-service cũ, rồi chạy bản mới (migration chạy lúc khởi động). Bản cũ gặp `SECTION_QUESTIONS` hoặc `EXPIRED`
   sẽ lỗi ở `valueOf`, nên không chạy song song hai phiên bản.
3. Khởi động lại Gateway sau khi đổi config-repo.

Lùi backend: chuyển dòng `SECTION_QUESTIONS` sang bảng lưu tạm trước khi chạy bản cũ. Không sửa migration đã áp dụng.

## Kiểm tra

```powershell
mvn -q -pl services/content-service -am test
mvn -q -pl infra/api-gateway test
mvn -q -pl services/content-service -am -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- Đối chiếu từng dòng E1..E41 với tên test; dòng nào chưa có test thì chưa xong.
- Kiểm tay với LLM thật (không đưa vào CI): một phần Reading, một phần Listening, mỗi dạng câu một lượt. Ghi số câu xin, dùng
  được, bị loại theo lý do, và số câu người duyệt thấy sai mà thuật toán cho qua. Chỉnh ngưỡng bằng cấu hình, ghi lại trước và sau.
- Kiểm tay: F5 khi đang sinh, hai tab cùng sửa, đổi đoạn văn khi chờ duyệt, màn hình hẹp, bàn phím.
- Báo cáo nêu rõ phần nào chưa kiểm được.
