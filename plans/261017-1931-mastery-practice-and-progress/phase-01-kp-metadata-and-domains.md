---
phase: 1
title: "Metadata KP và cây đủ 6 miền"
status: completed
priority: P2
dependencies: []
---

# Phase 1: Metadata KP và cây đủ 6 miền

## Overview

Tài liệu mentor (01 §3, §7.2) gắn mỗi KID với cấp CEFR, khoảng band, mô tả can-do và phạm vi. Cây phủ 6 miền:
Listening, Reading, Writing, Speaking, Grammar, Vocabulary. KP hiện tại chỉ có `code`, `name`, `kind`, `skill`,
`description`, `status`, `learning_type`; band lấy theo course. ~36 KP seed đều là chiến lược làm bài R/L/W.

## Requirements

- **Migration content `V41__kp_cefr_and_can_do.sql`** (số tạm):
  - `cefr_level VARCHAR(2) NULL CHECK (cefr_level IN ('A1','A2','B1','B2','C1','C2'))`;
  - `can_do TEXT NULL`, `scope_in TEXT NULL`, `scope_out TEXT NULL`.
  - Cho phép null để không phá KP đang có. Không thêm lại band (band vẫn theo course, V8 đã bỏ).
- **Bảng CEFR ↔ band** (03 §E.2) là hằng số trong domain content: A1 0–2, A2 2–3.5, B1 3.5–4.5, B2 4.5–6, C1 6–7.5,
  C2 7.5–9. Dùng để gợi ý `cefr_level` mặc định theo band của course khi tạo KP; không bắt buộc khớp.
- **API KP:** `CreateKnowledgePointRequest`, request sửa và response thêm 4 trường trên (thêm trường, không đổi trường cũ).
  Cây `GET` trả thêm `cefrLevel`, `canDo`.
- **Mã KP:** giữ quy tắc `^[RLW](55|60|65|70)_...` cho KP chiến lược. Thêm tiền tố `G` (Grammar) và `V` (Vocabulary)
  cho KP `kind` GRAMMAR/VOCABULARY, skill `ALL`. Mã seed cũ (`DR_`, `LS_`) giữ nguyên, không đổi tên (đổi mã làm gãy
  tham chiếu của learning).
- **Seed Grammar và Vocabulary** (migration seed riêng, trạng thái `INACTIVE` cho tới khi được duyệt):
  - khoảng 8 KP Grammar cho band 5.5–6.5 (ví dụ: câu phức, mệnh đề quan hệ, bị động, so sánh, thì hoàn thành, mạo từ,
    hòa hợp chủ vị, từ nối);
  - khoảng 5 KP Vocabulary (collocation học thuật, từ mô tả xu hướng số liệu, paraphrase, word form, chủ đề thường gặp);
  - cạnh tiên quyết từ KP Grammar/Vocab tới KP chiến lược liên quan (ví dụ: từ mô tả xu hướng → Writing Task 1);
    use case kiểm vòng có sẵn.
  - Nội dung tự viết (D14 plan 1); danh sách cuối do người hiểu IELTS duyệt, file `seed-grammar-vocab.md` ghi bảng KP và
    cạnh để duyệt trước khi viết migration.
- **Speaking:** không seed KP Speaking (chưa chấm được). Ghi vào câu hỏi mở của plan.

## Related Code Files

(`C` = `content-service/src/main/java/com/ieltspath/content`)

- Sửa: KP domain aggregate, JPA entity, mapper, `CreateKnowledgePointRequest`, request sửa, response cây và KP
- Tạo: `C/domain/vo/CefrLevel.java` (enum + khoảng band), migration schema, migration seed
- Test: test domain cho `CefrLevel` (biên band), `@WebMvcTest` cho request mới (mã sai tiền tố → 400), integration
  Testcontainers: migration áp được, KP cũ vẫn đọc được với trường null, seed tạo cạnh không vòng

## Implementation Steps

1. Viết `seed-grammar-vocab.md` (bảng KP, mô tả can-do, cạnh) và xin duyệt.
2. Migration schema, enum, entity, mapper; test đỏ trước cho request mới.
3. API và cây trả trường mới.
4. Migration seed sau khi danh sách được duyệt.
5. `mvn -q -pl services/content-service -am test`.

## Success Criteria

- [x] KP có thể lưu và đọc `cefrLevel`, `canDo`, `scopeIn`, `scopeOut`; KP cũ không lỗi.
- [x] Tạo KP Grammar với mã `G55_...` được; mã sai tiền tố → `400`.
- [x] Cây có KP Grammar/Vocab nối vào KP chiến lược, không có vòng. (test: `GrammarVocabularySeedMigrationTest` trên PostgreSQL: 13 KP, 13 cạnh tới KP `STRATEGY` đang `ACTIVE`, 0 vòng)

## Progress

- Schema, API metadata và proposal `reports/seed-grammar-vocab.md` hoàn tất. Người dùng duyệt seed 2026-10-18. Số migration:
  V42 trống nhưng V43 và V44 đã áp dụng, Flyway từ chối V42 trên DB đã có V44 (trừ khi bật `out-of-order`), nên seed dùng
  **V45**, vẫn để KP `INACTIVE`. Xong: commit 649f623 (13 KP, 13 cạnh). Học viên chỉ thấy các KP này khi Content chuyển
  sang `ACTIVE`.
- Test: `mvn -B -ntp -f pom.xml -pl services/content-service test` — 407 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL Testcontainers ran.

## Risk Assessment

- Seed nội dung cần người duyệt; để `INACTIVE` để không lộ cho học viên khi chưa duyệt.
- Learning đọc KP qua API nội bộ: thêm trường là tương thích; kiểm `learning-content-internal-v1.md`.

## Bàn giao FE

Màn sửa KP thêm ô CEFR, can-do, phạm vi trong/ngoài. Cây hiển thị nhãn CEFR cạnh mã KP.
