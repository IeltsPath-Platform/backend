# Chia việc Claude và Codex: reliability patterns

Plan: [plan.md](./plan.md). Luật cứng cho Codex và cổng duyệt của Claude: mục "Khung kiểm soát Codex" trong `plan.md`. File này chỉ nói **ai làm gì, khi nào, bàn giao thế nào**.

## Phân công

| Phase | Nội dung | Người làm | Người duyệt | Bắt đầu khi |
|---|---|---|---|---|
| 1 | Nền nhiều instance: Redis, khóa scheduler, Redis pub/sub cho game và notification, vé game Redis | **Claude** | Bạn (kiểm tay 2 trình duyệt) | Ngay |
| 2 | Observability: `common-observability`, log JSON, Prometheus | **Claude** | Bạn (xem log và Prometheus) | Phase 1 xong |
| 3 | Rate limit gateway, chống dò mật khẩu, định dạng lỗi | **Codex** | Claude | Phase 1, 2 xong |
| 4 | Relay outbox access → notification, dọn outbox game, xóa code chết content | **Codex** | Claude | Phase 3 qua cổng duyệt |
| 5 | Resilience4j cho 22 client, timeout gateway, contract test | **Codex** | Claude | Phase 4 qua cổng duyệt |
| 6 | Idempotency cấp gói admin, test cửa sổ lỗi saga, dedupe listener | **Codex** | Claude | Phase 5 qua cổng duyệt |
| 7 | Quy tắc version event, golden test, đưa lại từ DLQ, rà `@Version` | **Codex** | Claude | Phase 6 qua cổng duyệt |
| 8 | Bộ test lỗi tổng, bảng kịch bản trong tài liệu | **Codex** | Claude | Phase 7 qua cổng duyệt |
| — | Mở 1 PR backend vào `main` | **Claude** | Bạn (merge) | Phase 8 qua cổng duyệt |

Codex làm tuần tự 3 → 8 (một tác tử, không chạy song song). Không có việc frontend.

## Nhánh và cách làm chung

- Claude tạo `feat/reliability-patterns` từ `main` khi bắt đầu phase 1, push lên remote để Codex dùng.
- Phase 1–2: chỉ Claude commit. Phase 3–8: chỉ Codex commit code; Claude **không** commit code trong lúc Codex đang làm (tránh xung đột), chỉ review. Sửa sau review do Codex làm thành commit riêng.
- Không PR trung gian. Một PR vào `main` khi xong cả plan.
- Codex được bắt đầu phase kế tiếp trong lúc Claude duyệt phase trước; nếu review trả lại, Codex sửa phase cũ trước rồi làm tiếp.

## Vòng bàn giao mỗi phase Codex

```
Bạn dán prompt phase N cho Codex
  → Codex làm, báo cáo theo mẫu trong plan.md
Bạn dán báo cáo cho Claude ("codex báo như này")
  → Claude chạy cổng duyệt 4 bước (đối chiếu khung, test đỏ có thật, chạy lại lệnh, kiểm tay)
     ├─ Đạt: Claude `ck plan check N`, đưa prompt phase N+1
     └─ Chưa đạt: Claude viết danh sách lỗi (file:dòng, cách sửa) → bạn dán cho Codex
```

## Việc của Claude

| Khi | Việc |
|---|---|
| Bắt đầu | Tạo nhánh; làm phase 1 theo TDD; push |
| Sau phase 1 | Làm phase 2; push; báo bạn kiểm tay |
| Mỗi phase Codex | Cổng duyệt 4 bước; ghi kết quả vào `reports/review-codex-phase-N-report.md` |
| Cuối | Chạy `mvn -B -ntp verify` toàn reactor; kiểm tay acceptance tổng (scale 2 instance); mở 1 PR; cập nhật trạng thái plan |

## Việc của bạn

- Dán prompt cho Codex và chuyển báo cáo của Codex cho Claude.
- Kiểm tay phase 1 (2 trình duyệt chơi game, nhận thông báo) và phase 2 (xem log, Prometheus).
- Trả lời khi Claude hoặc Codex dừng hỏi (`NEEDS_CONTEXT`).
- Merge PR cuối.

## Prompt cho Codex

Dán nguyên khối, thay `N` và tên file phase. Prompt dùng chung cho mọi phase:

```
Làm phase N của plan backend/plans/261031-2145-backend-reliability-patterns.

Đọc trước, theo thứ tự:
1. backend/AGENTS.md
2. plan.md, mục "Khung kiểm soát Codex" (luật cứng, mẫu báo cáo) và "Quyết định đã chốt"
3. <phase-file>, toàn bộ, đặc biệt mục "Khung code cho Codex"

Làm trên nhánh feat/reliability-patterns (git pull trước). Không tạo nhánh khác, không mở PR.
Làm đúng "Khung code cho Codex": tên lớp, chữ ký, file, migration, tên test như ghi.
Chỉ sửa file trong bảng "File được sửa". Gặp mục "Dừng và hỏi" hoặc khung không khớp code thật thì dừng, báo NEEDS_CONTEXT.
Commit 1: chỉ test mới (phải đỏ). Commit sau: code. Kết thúc: chạy "Lệnh kiểm chứng" của phase.
Không đọc .env, không ghi Redis/RabbitMQ/Supabase thật, không push lên main.

Cuối cùng báo cáo đúng mẫu trong plan.md (Phase, Status, Commits, Files, Red tests, Verification, Deviations, Concerns).
```

| Phase | `<phase-file>` | Ghi chú thêm cho prompt |
|---|---|---|
| 3 | `phase-03-api-reliability.md` | Liệt kê đường dẫn route upload và AI tìm được vào báo cáo |
| 4 | `phase-04-outbox-relay.md` | Kiểm tên ràng buộc CHECK thật của `outbox_events` trước khi viết migration |
| 5 | `phase-05-resilience-va-goi-service.md` | Version resilience4j xung đột → BLOCKED, dán lỗi |
| 6 | `phase-06-idempotency-va-saga.md` | Test cửa sổ lỗi đỏ vì lỗi thật → dừng, không tự sửa saga |
| 7 | `phase-07-ra-messaging-va-concurrency.md` | Listener tự xử lý ACK: mặc định giữ nguyên, chỉ thêm Javadoc |
| 8 | `phase-08-test-loi-tong.md` | Chỉ viết test đánh dấu "mới"; không viết lại test có sẵn |

## Theo dõi tiến độ

| Phase | Người làm | Trạng thái | Commit cuối | Báo cáo review |
|---|---|---|---|---|
| 1 | Claude (code), Codex (việc còn lại: gói B) | Code xong và đã merge; còn tài liệu "Chạy nhiều instance", dọn văn bản plan, `WorkItemSync`, test relay song song; chờ kiểm tay | `3c6abea7`…`c54dfbdb` | — |
| 2 | Claude (code), Codex (gói B) | Code xong và đã merge; còn test `/actuator/prometheus` mỗi service, gauge DLQ, correlation qua outbox, tài liệu; chờ kiểm tay | `fa162926`…`(prometheus)`, fix `5221e81c` | — |
| 3 | Codex | `DONE_WITH_CONCERNS` (`e5161ba3`); chạy lại xanh; **chưa qua cổng**, chờ gói A | `56458bdf`, `e5161ba3` | Cổng duyệt 2026-11-01 (trong mục "Sửa sau cổng duyệt" của phase 3) |
| 4 | Claude (code), Codex (gói B) | Code và test xong, đã merge; còn bảng event trong tài liệu, 2 test; chờ kiểm tay | `6b539bf4`…`e30674e0` | — |
| 5 | Codex (gói C) | Chưa làm; spec đã đính chính | — | — |
| 6 | Codex (gói D) | Chưa làm | — | — |
| 7 | Claude (code), Codex (gói B) | Phần chính xong, đã merge; còn test song song, test replay learning, rà listener | `9e8aa3d4`…`32d3886d` | — |
| 8 | Codex (gói E) | Chưa làm; thêm kịch bản draining và correlation | — | — |

Claude cập nhật bảng này sau mỗi cổng duyệt.

## Chạy một mạch: các gói còn lại cho Codex

Từ 2026-11-01 Codex làm **toàn bộ việc còn lại của plan này** theo năm gói, rồi báo một lần; Claude duyệt hàng loạt và làm phần kiểm tay. Plan đính chính [261101-0930-reliability-followups](../261101-0930-reliability-followups/plan.md) là spec chi tiết của các gói; khi mâu thuẫn với phase file cũ, spec đính chính thắng. Nhánh: `feat/reliability-patterns` (đã chứa commit merge `1899f6e0`: phase 4, 7 và fix correlation của Claude).

| Gói | Nội dung | Spec | Phụ thuộc | Lệnh kiểm chứng |
|---|---|---|---|---|
| A | Sửa phase 3 sau cổng duyệt: path auth, `reset-password`, XFF từ phải sang trái, `AuthRouteConsistencyTest` | [phase 3 §Sửa sau cổng duyệt](./phase-03-api-reliability.md) và [đính chính, phase 2](../261101-0930-reliability-followups/phase-02-chinh-phase-3-auth-va-xff.md) | — | `mvn -B -ntp -pl infra/api-gateway -am verify` |
| B1 | Phase 1 còn lại: dọn văn bản phase 1, `WorkItemSync` không giữ khóa lúc gọi HTTP, test song song relay assessment và community, mục "Chạy nhiều instance" trong `docs/system-architecture.md` | [đính chính, phase 5](../261101-0930-reliability-followups/phase-05-don-phase-1-va-khoa-http.md) | — | `mvn -B -ntp -pl services/learning-service,services/assessment-service,services/community-service -am verify` |
| B2 | Phase 2 còn lại: correlation qua outbox (4 migration, adapter, publisher); test `/actuator/prometheus` có security thật ở 9 service; gauge `dead_letter_messages{queue}` thay counter `messages_dead_lettered_total` ở notification, access, learning (dùng `DeadLetterReplayer.counts()`, `NaN` khi broker lỗi); tài liệu | [đính chính, phase 1](../261101-0930-reliability-followups/phase-01-correlation-qua-outbox.md) và [phase 2 §Test viết trước, bước 5](./phase-02-observability.md) | — | `mvn -B -ntp -pl shared/common-observability,services/assessment-service,services/community-service,services/access-service,services/learning-service,services/notification-service -am verify` |
| B3 | Phase 4 còn lại: bảng event (producer, consumer, queue, DLQ, contract) trong `docs/system-architecture.md`; test `temporaryFailureIsRetriedThenSucceeds` (notification, `SubscriptionGranted`); test `rollbackLeavesNoOutboxRow` (access) | [phase 4 §Test viết trước, bước 3; §Tài liệu](./phase-04-outbox-relay.md) | — | `mvn -B -ntp -pl services/access-service,services/notification-service -am verify` |
| B4 | Phase 7 còn lại: test song song không mất cập nhật cho tiến độ bài học (learning), bộ thẻ (library), phòng (game); test integration endpoint replay của learning; phân loại và Javadoc mọi listener còn lại (access `LearningPointsGrantListener`, learning `PointsGrantSettledListener`, bốn listener grading của notification) | [phase 7 §Test viết trước 2–3, §Success Criteria](./phase-07-ra-messaging-va-concurrency.md) | — | `mvn -B -ntp -pl services/learning-service,services/library-service,services/game-service,services/access-service,services/notification-service -am verify` |
| C | Phase 5 theo thiết kế đã đính chính (lượt 3a rồi 3b) | [phase 5](./phase-05-resilience-va-goi-service.md) và [đính chính, phase 3](../261101-0930-reliability-followups/phase-03-thiet-ke-lai-resilience.md) | A, B2 | theo phase 5 |
| D | Phase 6 như phase file | [phase 6](./phase-06-idempotency-va-saga.md) | C | theo phase 6 |
| E | Cấu hình graceful shutdown và draining, rồi phase 8 gồm kịch bản mới (dừng một replica, correlation HTTP→outbox→consumer, danh sách endpoint auth) | [đính chính, phase 4](../261101-0930-reliability-followups/phase-04-graceful-shutdown-va-draining.md) và [phase 8](./phase-08-test-loi-tong.md) | A–D | `mvn -B -ntp verify` |

Quy ước thêm cho chạy một mạch:

- Mỗi gói: commit test đỏ trước, commit code sau, chạy lệnh kiểm chứng, ghi báo cáo theo mẫu vào `reports/codex-package-<chữ>-report.md` (gói B mỗi mục B1–B4 một báo cáo). Báo cáo ghi mọi Deviation, kể cả sửa test đã có.
- Gói bị `NEEDS_CONTEXT` hoặc `BLOCKED`: ghi vào báo cáo, **bỏ qua gói phụ thuộc vào nó**, làm tiếp gói độc lập. Không chờ Claude giữa các gói.
- Phạm vi file của mỗi gói là các file nêu trong spec của nó; cần file khác thì ghi `NEEDS_CONTEXT`.
- Số migration: access `V7` (idempotency, gói D) và `V8` (correlation, gói B2); assessment `V21`; community `V9`; learning `V41`. Kiểm số kế tiếp còn trống trước khi tạo.
- Kiểm tay trên stack thật là việc của Claude, không phải của Codex.

### Prompt tổng cho Codex (dán nguyên khối)

```
Làm toàn bộ việc còn lại của plan backend/plans/261031-2145-backend-reliability-patterns, theo năm gói A, B (B1–B4), C, D, E.

Đọc trước, theo thứ tự:
1. backend/AGENTS.md
2. plan.md, mục "Khung kiểm soát Codex" (luật cứng, mẫu báo cáo)
3. work-split-claude-codex.md, mục "Chạy một mạch: các gói còn lại cho Codex" (bảng gói, spec, phụ thuộc, lệnh kiểm chứng, quy ước)
4. Spec của từng gói ngay trước khi làm gói đó. Spec ở plans/261101-0930-reliability-followups thắng phase file cũ khi mâu thuẫn.

Thứ tự: A → B1, B2, B3, B4 → C (3a rồi 3b) → D → E. B độc lập với A. C cần A và B2. D cần C. E cần A–D.

Làm trên nhánh feat/reliability-patterns (đã có commit merge 1899f6e0; git pull nếu cần). Không tạo nhánh khác, không mở PR, không push, không merge. Không đọc .env, không ghi Redis/RabbitMQ/Supabase thật.

Với mỗi gói: commit 1 chỉ có test mới (phải đỏ; ghi tên test đỏ và lý do); commit sau là code; chạy "lệnh kiểm chứng" của gói và dán số thật; ghi báo cáo theo mẫu vào plans/261031-2145-backend-reliability-patterns/reports/codex-package-<chữ>-report.md. Không sửa, xóa hay @Disabled test cũ để cho xanh; nếu buộc phải sửa một test đã có, ghi rõ vào Deviations. Không đặt mã plan, số phase, nhãn B1–B10 trong code, tên test, tên migration hay commit message.

Khi gặp NEEDS_CONTEXT hoặc BLOCKED trong một gói: ghi vào báo cáo của gói đó, bỏ qua mọi gói phụ thuộc nó, làm tiếp gói độc lập, không chờ ai. Khung không khớp code thật, cần quyết định sản phẩm, hoặc cần file ngoài phạm vi spec thì dừng gói đó (không tự đổi).

Kết thúc: chạy mvn -B -ntp verify toàn reactor, rồi in một bảng tóm tắt: gói, trạng thái, commit, số test chạy/lỗi, Deviations, Concerns. Kiểm tay trên stack thật và cổng duyệt là việc của Claude, đừng làm.
```
