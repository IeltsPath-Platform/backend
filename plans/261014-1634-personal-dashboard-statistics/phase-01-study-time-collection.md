---
phase: 1
title: "Study time collection"
status: pending
effort: ""
dependencies: []
---

# Phase 1: Thu thời gian học và lịch sử mục tiêu

## Overview

Nguồn chuẩn: [thiết kế §2.1, §3.2, §4](./dashboard-statistics-design.md). Triển khai dữ liệu mới tại User và hook FE trên trang học thật. Không cộng duration client CRUD hiện tại vào thống kê chính thức.

## Requirements

- Ghi thời gian active có giới hạn theo session/lease, retry không trùng; tách ngày/timezone và course đã xác nhận.
- Goal snapshot theo ngày, không hồi tố target. Dữ liệu trước ngày bật đo là NOT_TRACKED.
- Lỗi telemetry không chặn submit bài; không ghi nội dung bài học, đáp án, token.

## Architecture

`api → application/usecase → domain/aggregate + repository`; HTTP xác nhận source thông qua application port và infrastructure adapter. Hành vi nhận khoảng thời gian/lease là domain, transaction/lock do application điều phối. JPA adapter chỉ persistence. Không HTTP trong transaction đang giữ khóa.

Tận dụng Clock, validation/ErrorResponse và CurrentUserProvider hiện có. Start idempotency unique `(userId,clientSessionId)`. Heartbeat unique `(sessionId,epoch,sequence)`; payload hash phát hiện cùng key khác nội dung. Lease authority chỉ một user/session tại một thời điểm, khóa trên row user/lease ổn định hoặc advisory transaction lock. Takeover đóng interval của epoch cũ tại thời điểm server, epoch cũ không thể ghi đè.

## Related Code Files

Đường dẫn dưới đây tính từ root BE `D:/Fall2026/New folder/BE/backend` hoặc FE `D:/Fall2026/New folder/FE/frontend`. Tên mới là đề xuất.

- BE tạo `services/user-service/src/main/java/com/ieltspath/user/domain/aggregate/StudySession.java` và repository tương ứng.
- BE tạo use cases `StartStudySessionUseCase`, `RecordStudyHeartbeatUseCase`, `CloseStudySessionUseCase`, `GetStudyTimeStatisticsUseCase` trong `services/user-service/src/main/java/com/ieltspath/user/application/usecase/`.
- BE tạo `application/port/StudySourceVerifier.java`, adapter HTTP theo owner; endpoint context tối thiểu tại Learning/Assessment nếu chưa có API trả dữ liệu cần xác nhận.
- BE tạo controller/DTO dưới `services/user-service/src/main/java/com/ieltspath/user/api/`; entity/repository/adapter cho session, interval, daily total, goal history theo convention user-service.
- BE sửa `services/user-service/src/main/java/com/ieltspath/user/application/usecase/CreateLearningGoalUseCase.java` và `ChangeLearningGoalStatusUseCase.java` để ghi goal history cùng transaction; không sửa semantic các API goal hiện tại.
- BE migration mới `services/user-service/src/main/resources/db/migration/V{next}__study_time_tracking.sql`.
- FE tạo `src/features/study-activity/useStudySession.ts`, `studyActivityApi.ts`; sửa `src/lib/httpClient.ts` chỉ nếu cần keepalive option.
- FE nối tại LessonPage/PracticePage/ReviewPage/PlacementPage/TopicTestPage và media đã liệt kê trong bản thiết kế.
- Không xóa file hiện có; không đổi config/shared/security hoặc activity CRUD chỉ để dashboard chạy.

## Implementation Steps

1. Chốt policy idle/hidden/retention/timezone; freeze schema request/response, mã lỗi và interval bounds trước nối FE.
2. Viết test state-machine: start replay, close replay, lease expiry/takeover, payload conflict, out-of-order, khoảng chồng lấn, future/backlog quá hạn. Session đóng không được resurrect bằng retry.
3. Migration additive với unique keys và index; atomic insert receipt + interval + upsert daily tổng. Trường ALL và course chỉ là hai view cùng dữ liệu, API không cộng ALL với course một lần nữa.
4. Xác nhận source/ownership/course từ owner một lần khi start; giữ correlation và bearer đúng contract, giới hạn timeout. Source không hợp lệ hoặc service owner lỗi thì không start và FE vẫn được học.
5. Ghi history goal có hiệu lực ngày kế tiếp; goal đang active lúc deploy tạo bản đầu. Với thay đổi timezone, ngày chuyển tiếp phải có mốc UTC không chồng lấn; một ngày báo cáo chỉ có một planned target, không cộng target hai lần khi có nhiều timezone record.
6. API query daily tổng theo khoảng hữu hạn; tuần/tháng/course scope và thiếu dữ liệu đúng §2.1. Tổng hợp tại database.
7. FE hook active/media/visibility, monotonic clock, sequential retry và reset sau lease expiry. Không lưu Bearer vào persistent storage. Mount/unmount/React StrictMode không tạo session trùng.
8. Flush keepalive best effort khi hidden và trước logout; normal heartbeat dùng auth refresh hiện có. Request cuối không cố refresh/login sau khi app logout.
9. Đo request rate và đối chiếu ledger/totals trong cửa sổ raw retention; chỉ thêm cleanup job sau khi chính sách purge được duyệt.

## Success Criteria

- [ ] 60 giây active trong một tab không thành 120 giây khi tab thứ hai mở cùng bài.
- [ ] Mất ACK và gửi lại cùng payload không đổi daily total.
- [ ] Request source/user/epoch sai bị từ chối trước ghi.
- [ ] Qua ngày, goal đổi, hidden/idle/media đều đúng policy và có test.
- [ ] Không backfill active time bằng attempt elapsed/audio duration.
- [ ] Query có index user/date/scope, khoảng tối đa 366 ngày; không đọc findAll unbounded.
- [ ] Telemetry offline không làm mất bài nộp hoặc thay đổi gate.

## Validation

Chạy unit/MVC trước, rồi `mvn -q -pl services/user-service -am test`; Testcontainers PostgreSQL cho locks, concurrent replay, migration và aggregate. Nếu sửa context endpoint ở owner khác, chạy test đúng module đó. FE manual browser: 2 tab, refresh, background, sleep/resume, offline, mất token và logout; dùng fake clock cho phép tính thuần nếu framework test được duyệt.

## Risks and rollback

Idle policy có thể đếm thiếu người đọc lâu; ghi là thời gian được ghi nhận, đo UX trước sửa tham số. Không kỳ vọng chống gian lận tuyệt đối từ client. Rollback hook/API đọc, giữ dữ liệu/migration; không sửa lịch sử migration. Dedupe watermark còn hiệu lực sau purge raw interval.
