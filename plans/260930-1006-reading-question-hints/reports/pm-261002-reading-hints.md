# Reading hints — 2026-10-02

| Hạng mục | Kết quả |
| --- | --- |
| Plan | Completed; 3/3 phase, 9/9 tiêu chí; `feat/lesson-reading-hints`, base `0ffb101` |
| Phase 1 | Content V13, 7 seed hint/Q3 null, admin + nội bộ; learner contract đã khép |
| Phase 2 | Java policy + lịch sử mọi response; GET/POST hint nullable; giữ tới khi khối từng đạt; replay chính xác; evidence/mastery giữ nguyên |
| Phase 3 | 7 tài liệu đồng bộ; DATABASE_V5 V5.4/§5.5/Java §7.4; Learning V1 index; Content lưu/Learning quyết định |
| Review | Không finding |

## Kiểm chứng

- Content: 140 pass, 0 skip trên nhánh trước; không chạy lại phiên này.
- Baseline: 175 Learning + 8 common-security, 0 fail/error/skip. RED thiếu policy trước code; focused 76 pass.
- Regression: 183 Learning + 8 common-security, 0 fail/error/skip; Docker/Testcontainers thực chạy. Assertion DB cuối: 1 pass. Không cộng các lượt chạy thành tổng test riêng biệt.
- Graphify exit 0: 8.147 node, 28.230 edge. Contract: 19 JSON example parse OK. `git diff --check` pass.
- Validator kiến trúc: 7 link OK; 49 warning = 15 code reference + 34 config key. Không clean; enum FILL/CHOICE và reference ngoài source Learning gây cảnh báo.
- Không full reactor, live E2E hoặc push.

## Phạm vi và rủi ro

- Mapping Java đã chấp nhận thay Python; schema/index dùng Java §7.4/V1. Tài liệu mở 3 → 7 file để khép pending note; không đổi nghiệp vụ.
- Đóng: stale learner-contract note, checklist phase 1, dependency 1640/0851, index/seed-ID, pending docs. Task tools không khả dụng; sync trực tiếp cả 3 phase.
- Còn: test HTTP 400 cho Content hint 501 ký tự chưa có; `@Size(max = 500)` đã có. Owner Content maintainer; DoD: `QuestionControllerTest` trả 400 với 501 ký tự. Theo dõi từ phiên trước; khoảng trống coverage, không blocker 9 tiêu chí.
- Còn: validator 49 warning. Owner maintainer tài liệu/tooling; DoD: xác minh reference bằng source scope đúng hoặc sửa enum detection khi xử lý validator.
- Rủi ro nội dung: so chuỗi seed không bắt diễn đạt đáp án gián tiếp; owner người soạn, DoD: duyệt hint mới theo luật contract.

## Đóng gói

- Code/contract và tài liệu/status được tách thành hai commit local; hash ghi trong báo cáo bàn giao của phiên. Không push.

## Câu hỏi chưa giải quyết

- Không có câu hỏi chặn delivery. Hint nhiều bậc và chặn lộ đáp án tại API admin thuộc đợt sau đã chốt.
