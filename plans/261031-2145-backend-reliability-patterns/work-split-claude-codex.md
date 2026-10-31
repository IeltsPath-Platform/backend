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
| 1 | Claude | Chưa bắt đầu | — | — |
| 2 | Claude | Chưa bắt đầu | — | — |
| 3 | Codex | Chờ phase 1, 2 | — | — |
| 4 | Codex | Chờ | — | — |
| 5 | Codex | Chờ | — | — |
| 6 | Codex | Chờ | — | — |
| 7 | Codex | Chờ | — | — |
| 8 | Codex | Chờ | — | — |

Claude cập nhật bảng này sau mỗi cổng duyệt.
