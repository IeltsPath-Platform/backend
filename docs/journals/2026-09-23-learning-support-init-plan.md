# 2026-09-23 — Plan init learning-support-service

Shell `learning-support-service` đã có gateway và config, chưa có schema hay API. Plan HOLD thêm Flyway cho 8 bảng cộng outbox, rồi CRUD owner-scoped. Không RBAC, không consumer, không publisher.

Red team sửa Compose cho cùng network và env với `user-service`, map `timestamptz` sang `Instant`, và giới hạn 409 vào unique violation. Validation giữ last-write-wins, cho client ghi activity/streak, và chấp nhận activity trùng khi retry.

Chưa implement.
