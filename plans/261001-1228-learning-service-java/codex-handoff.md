# Giao việc cho agent code: learning-service Java

Mỗi PR một **phiên agent mới**, dán nguyên khối "Prompt". Prompt trỏ tới "Quy tắc chung" ngay dưới.

## Quy tắc chung (mọi PR)

1. **Đọc trước khi code:** `AGENTS.md`, `CLAUDE.md`, `plans/261001-1228-learning-service-java/plan.md` (bảng "Quyết định",
   "Mô hình dữ liệu"), file phase của PR. Luật nghiệp vụ: `plans/260929-1640-lesson-learning-pipeline-mvp/plan.md` (bảng
   "Quyết định áp dụng"), `phase-06`, `phase-07` của 1640 (đọc luật, **không** làm theo tên file Python), contract
   `docs/contracts/*`, `plans/260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md` (kịch bản Lan).
2. **Python đã xóa ở PR 1.** Cần tham khảo hành vi cũ thì `git show 90fd390:services/ai-learning-service/<đường dẫn>`. Không
   khôi phục file Python nào.
3. **Plan là nguồn quyết định.** Plan và code mâu thuẫn, hoặc thiếu thông tin → dừng, báo `NEEDS_DECISION` kèm lựa chọn.
4. **Kiến trúc:** đúng AGENTS §3 (domain không Spring/JPA; application không import api/infrastructure; gọi service khác qua
   `application/port` + adapter). Mẫu gần nhất: `services/library-service`. Không N+1, không gọi repository/HTTP trong vòng lặp.
5. **Test mức nhẹ** (người dùng chốt): không TDD đầy đủ; làm đúng các test ghi trong phase. Testcontainers cần Docker; thiếu
   Docker thì test bị skip → báo rõ, **không** báo pass.
6. **Cấm:** sửa migration đã có; sửa `common-security`, config dùng chung ngoài phạm vi phase; thêm thư viện lớn (Spring AI…);
   test gọi LLM thật; log nội dung câu trả lời, token, secret; ghi giá trị secret vào file; ghi mã plan, số phase vào code
   comment, tên test, migration, commit message; commit `.env`, `graphify-out/`, build output.
7. **Git:** nhánh mới từ `feat/main-follow`, tên `feat/learning-<việc>`. Conventional commit, không nhắc AI/agent. Không push.
8. **Sau khi code:** có `graphify` thì `graphify update .`.
9. **Báo cáo cuối:**
   ```text
   Status: DONE | BLOCKED | NEEDS_DECISION
   Đã làm: <theo Requirements của phase>
   File đổi: <danh sách>
   Test đã chạy: <lệnh> → <kết quả thật, số pass/fail/skip>
   Lệch plan: <hoặc "không">
   Cần người duyệt: <nếu có>
   ```

## Thứ tự PR

| # | PR | Phase | Cần có trước | Trạng thái |
| --- | --- | --- | --- | --- |
| 1 | Xóa Python, khung learning-service | 1 | — | Xong (merge #26 nhầm vào main, revert #27; khôi phục #28 `07324d9`) |
| 2 | Bài học, nộp bài, cổng, mastery | 2 | PR 1 | Xong (merge #29) |
| 3 | Bài ôn, giao mã đề | 3 | PR 2 | Xong (Claude làm vì Codex hết token; nhánh `feat/learning-reviews-tests`, kèm sửa khối essay và audio) |
| 4 | Consumer AssessmentCompleted.v2 | 4 | PR 3 | Xong (Claude, nhánh `feat/learning-reviews-tests`) |
| 5 | Tài liệu repo | 5 | PR 4 | Xong (Claude, nhánh `feat/learning-reviews-tests`) |

Sau PR 4: PR 8 của `260929-1640/codex-handoff.md` (assessment tự chấm), rồi E2E.

---

## PR 1 — Xóa Python, khung learning-service

```text
Làm phase 1 của plans/261001-1228-learning-service-java/phase-01-xoa-python-dung-khung.md.
Đọc mục "Quy tắc chung" trong plans/261001-1228-learning-service-java/codex-handoff.md và làm theo.
Nhánh: feat/learning-service-skeleton.

- Xóa toàn bộ services/ai-learning-service/, compose ai-learning-*, llm-stub, volume, route Gateway cũ, contract tutor-sse-v1,
  practice-v1, docs/ai-learning-database.md.
- Tạo services/learning-service (port 8086, package com.group01.learning) theo mẫu library-service, thêm vào reactor.
- V1__learning_schema.sql: mọi bảng ở mục "Mô hình dữ liệu" của plan.md; cột/index theo V11 trong
  plans/260929-1640-lesson-learning-pipeline-mvp/phase-06-ai-learning-api-bai-hoc-va-bai-on.md (bỏ tiền tố path_).
- config-repo/learning-service.yaml (mật khẩu DB chỉ qua ${LEARNING_DB_PASSWORD}, không fallback), compose learning-db (5436),
  Gateway /api/learning/** -> lb://learning-service + internal-jwt-paths.
- Sửa contract lesson-learning-v1.md (prefix /api/learning, thêm GET /mastery) và assessment-completed-v2.md (tên queue
  learning.*, bỏ phần parking/path) đúng như phase.
Không viết nghiệp vụ ở PR này. .env không được đọc hay sửa: thiếu LEARNING_DB_PASSWORD thì báo người dùng tự thêm.
Test: mvn -q -pl services/learning-service -am test; mvn -q -pl infra/api-gateway test; mvn -q compile -DskipTests;
docker compose config --quiet. Báo cáo kèm danh sách chỗ còn chữ ai-learning (để PR 5 sửa).
```

## PR 2 — Bài học, nộp bài, cổng, mastery

```text
Làm phase 2 của plans/261001-1228-learning-service-java/phase-02-bai-hoc-va-mastery.md.
Đọc mục "Quy tắc chung" trong plans/261001-1228-learning-service-java/codex-handoff.md và làm theo.
Nhánh: feat/learning-lessons.

- Domain thuần: MasteryCalculator (port compute_mastery, git show 90fd390:services/ai-learning-service/app/mastery/mastery.py,
  giữ comment nguồn Apache-2.0), AnswerSpecGrader (docs/contracts/answer-spec-v1.md), TopicStatusDeriver, LessonAccessGate,
  ReviewRule.
- LearningContentClient (port + RestClient) gọi /internal/learning-content/* của content (contract learning-content-internal-v1.md).
- 5 endpoint bài học + GET /api/learning/mastery theo lesson-learning-v1.md; mọi ghi trong một @Transactional có
  pg_advisory_xact_lock theo user; bằng chứng kp_evidence chỉ ở lần nộp đầu của khối.
- Lỗi cổng: {detail, code, ...} qua GlobalExceptionHandler.
Test: unit cho 5 lớp domain (ReviewRule chạy kịch bản Lan ngưỡng 0.6 của seed-content.md; số mastery lệch → dừng báo, không sửa
số cho khớp; AnswerSpecGrader chạy hết docs/contracts/answer-spec-v1-vectors.json); 1 test Testcontainers luồng nộp
(trùng requestId, nộp lại không thêm bằng chứng, bài xong chèn review); @WebMvcTest allowlist field câu hỏi.
Regression: mvn -q -pl services/learning-service -am test.
```

## PR 3 — Bài ôn, giao mã đề

```text
Làm phase 3 của plans/261001-1228-learning-service-java/phase-03-bai-on-va-ma-de.md.
Đọc mục "Quy tắc chung" trong plans/261001-1228-learning-service-java/codex-handoff.md và làm theo.
Nhánh: feat/learning-reviews-tests.

GET/POST review, POST /topics/{id}/test-assignments theo lesson-learning-v1.md và luật 6b ở phase-06 của 1640 (SKIPPED sau 3
set trượt, hết gói lấy gói lâu nhất, mã đề idempotent, TEST_UNAVAILABLE). Dùng LessonAccessGate, kp_evidence (source review_set),
advisory lock theo user của PR 2.
Test: các ca Testcontainers trong phase. Regression: mvn -q -pl services/learning-service -am test.
```

## PR 4 — Consumer AssessmentCompleted.v2

```text
Làm phase 4 của plans/261001-1228-learning-service-java/phase-04-consumer-ket-qua-de.md.
Đọc mục "Quy tắc chung" trong plans/261001-1228-learning-service-java/codex-handoff.md và làm theo.
Nhánh: feat/learning-assessment-consumer.

Spring AMQP listener + topology retry/DLQ (tên queue theo docs/contracts/assessment-completed-v2.md đã sửa ở PR 1), parse event
(tham khảo git show 90fd390:services/ai-learning-service/app/adapters/formal_evidence_adapter.py và app/messaging/), idempotency
và chấm lại theo assessment_result_versions, bằng chứng source assessment, TOPIC_GATE theo phase-07 của 1640, ReviewRule.
Một transaction cho mọi ghi, ACK sau commit. Không gọi HTTP, không log câu trả lời.
Test: unit parse event; Testcontainers Postgres + RabbitMQ cho TOPIC_GATE kịch bản Lan, giao lại event, version 2.
Regression: mvn -q -pl services/learning-service -am test.
```

## PR 5 — Tài liệu repo

```text
Làm phase 5 của plans/261001-1228-learning-service-java/phase-05-tai-lieu-va-plan-con.md.
Đọc mục "Quy tắc chung" trong plans/261001-1228-learning-service-java/codex-handoff.md và làm theo.
Nhánh: feat/learning-docs.

Chỉ tài liệu: AGENTS.md, CLAUDE.md, README.md, docs/system-architecture.md, .sdd/database/DATABASE_V5.md. Mỗi dữ kiện kiểm với
code. Không sửa plans/. Kiểm: mvn -q compile -DskipTests và grep như Success Criteria của phase.
```
