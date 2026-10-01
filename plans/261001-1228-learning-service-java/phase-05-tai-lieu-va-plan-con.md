---
phase: 5
title: "Tài liệu repo"
status: completed
priority: P2
dependencies: [4]
effort: "0.5 ngày"
---

# Phase 5: Tài liệu repo

Plan con đã được đồng bộ ngày 2026-10-01 (chú thích đầu phase + [`python-to-java-mapping.md`](./python-to-java-mapping.md));
phase này chỉ sửa tài liệu của repo.

## Kết quả (2026-10-01)

- Sửa `AGENTS.md` (bỏ §3.8 Python, thêm §3.8 Learning Service), `CLAUDE.md`, `README.md` §6, `docs/system-architecture.md`, `.sdd/database/DATABASE_V5.md` (§7 thay bằng schema V1 9 bảng), README learning-service. `assessment-completed-v2` sửa thứ tự bằng chứng theo `ordinal`. Các chỗ còn chữ Python/ai-learning là ghi chú lịch sử.

## Requirements

Đọc trước khi sửa, chỉ ghi dữ kiện đã kiểm với code:
- `AGENTS.md`: bỏ dòng Python, §3.8 (AI Learning Python), luật DeepTutor/tutor/quota tutor, lệnh pytest, cấm import `deeptutor`
  (thư mục `third_party/deeptutor` vẫn là bản đọc, giữ ghi chú nguồn của `MasteryCalculator`); thêm learning-service vào
  bảng module (8 → 9 service Java); tech stack bỏ FastAPI/pydantic/psycopg2/httpx/python-jose/pika/Flyway container.
- `CLAUDE.md` root: bảng module (cổng 8086, `learning_db` 5436, route `/api/learning/**`), lệnh chạy, cạm bẫy, contract.
- `README.md`, `docs/system-architecture.md`, `.sdd/database/DATABASE_V5.md` (mục AI Learning → Learning, schema V1 Java).

## Success Criteria

- [ ] `grep -ri "ai-learning\|ai_learning\|AI_LEARNING"` ngoài `plans/`, `docs/journals/` không còn kết quả, trừ ghi chú
  lịch sử có chủ đích (liệt kê trong báo cáo).
