---
title: "Platform hardening: secrets, dead outbox, architecture tests"
description: "Bỏ secret dự phòng trong config-repo, dọn outbox không có người nhận, thêm test kiến trúc chạy ở CI, và vài chỗ lệch nhỏ"
status: pending
priority: P1
branch: "fix/grade-writting-llm"
tags: [security, config, outbox, archunit, ci, docs]
blockedBy: []
blocks: []
created: "2026-11-11T09:20:00.000Z"
---

# Platform hardening

Sinh từ một lượt review hệ thống ngày 2026-11-11. Mọi nhận xét đã được kiểm lại với code; bảng dưới ghi cái nào đúng, cái
nào sai, cái nào bỏ. Chưa có code nào được viết.

## Nhận xét gốc và kết quả kiểm

| Nhận xét | Kết quả kiểm (2026-11-11) | Xử lý |
| --- | --- | --- |
| Config-repo có secret dự phòng | **Đúng.** 7 file: `access`, `api-gateway`, `assessment`, `community`, `content`, `notification`, `user`. Gồm `EXTERNAL_JWT_SECRET` và `GATEWAY_INTERNAL_JWT_SECRET` (gateway, user, và internal ở 5 service khác), `*_DB_PASSWORD`, `RABBITMQ_PASSWORD` (assessment). Learning và Library không có | Phase 1 |
| Mọi service đọc chung `.env`, kể cả khóa Supabase | **Đúng khi chạy trên máy** (`optional:file:../../.env` ở 9 service). **Không đúng trong Docker:** `.env` không vào image, Compose chỉ truyền `SUPABASE_SECRET_KEY` cho service cần lưu file | Phase 1 ghi rõ; không đổi cơ chế chạy local |
| Chưa có CD, chưa có nơi quản lý secret | **Đúng** | Sẽ deploy thật: plan `261111-1640-production-deployment` (cần phase 1 ở đây xong trước) |
| Outbox Game không có relay | **Đúng.** 10 use case ghi `outbox_events`; chỉ có `OutboxRetentionJob` xóa; không service nào nhận event Game | Phase 2 |
| Outbox Content không có relay | **Sai một nửa.** Có bảng `outbox_events` (V1) nhưng **không code nào ghi**: bảng chết. `AGENTS.md`/`CLAUDE.md` mô tả sai | Phase 2 |
| Community có bảng outbox không dùng | **Sai.** `CommunityEventOutboxAdapter` ghi, `CommunityOutboxRelay` gửi, Notification nhận | Không làm |
| Quy tắc chưa được máy kiểm | **Đúng phần lớn.** Không có ArchUnit, formatter, linter, JaCoCo. Đã có `ServiceDependencyGraphTest`, `AuthRouteConsistencyTest` | Phase 3 |
| Giá chấm Writing ở hai nơi | **Đúng, nhẹ.** Backend `learning.writing.point-cost`; frontend `FALLBACK_TERMS = { pointCost: 3 }` và `SPEAKING_AI_POINT_COST` dùng khi API giá lỗi | Phase 4 |
| Bài Writing lưu ở hai DB | Đúng nhưng **có chủ đích**: bài luyện ở Learning, bài thi ở Assessment (hai bounded context) | Không làm |
| Phí Practice và thi gate khác nhau | Đúng, **quyết định sản phẩm** (gate thuộc khóa đã trả tiền) | Phase 4: ghi lý do vào `docs/ai-policy.md` |
| Library có XP, streak, tim riêng | Đúng nhưng **có chủ đích**: plan `261106-2330-mobile-vocabulary-study` chốt hệ thống riêng cho mobile, độc lập với web | Không gộp. Phase 4: ghi ngoại lệ vào `AGENTS.md` §3.8 để không bị hiểu nhầm lần sau |

## Quyết định đã chốt (người dùng, 2026-11-11; không đảo)

| Câu hỏi | Quyết định |
| --- | --- |
| Dự án có lên production thật không | **Có.** CD và quản lý secret làm ở plan riêng (chờ thông tin nơi deploy); phase 1 vẫn làm trước, độc lập |
| Bỏ secret dự phòng tới đâu | **Bỏ hết** (JWT, DB, RabbitMQ): thiếu biến thì service không khởi động |
| Game: bỏ ghi outbox hay làm relay | **Bỏ ghi và xóa bảng** của Game và Content (thay đổi schema phá hủy đã được duyệt) |
| Duyệt ArchUnit (dependency test mới) | **Duyệt**, chỉ scope `test` |

## Phases

| Phase | Việc | Phạm vi | Phụ thuộc | Status |
| --- | --- | --- | --- | --- |
| 1 | [Bỏ secret dự phòng](./phase-01-remove-secret-fallbacks.md) | config-repo, docs | - | Pending |
| 2 | [Dọn outbox không có người nhận](./phase-02-dead-outbox-cleanup.md) | game, content, docs | - | Pending |
| 3 | [Test kiến trúc ở CI](./phase-03-architecture-tests.md) | pom, test mọi service Java | - | Pending |
| 4 | [Chỗ lệch nhỏ và tài liệu](./phase-04-small-fixes-and-docs.md) | frontend, docs | - | Pending |

Các phase độc lập; làm 1 trước vì rủi ro cao nhất và rẻ nhất.

## Rủi ro chung
- Phase 1 làm hỏng môi trường chạy local của ai thiếu biến trong `.env`: có `.env.example` và thông báo lỗi khởi động rõ tên biến.
- Phase 2 xóa bảng: dữ liệu trong đó là event không ai đọc; vẫn là thay đổi schema phá hủy, cần duyệt (mục "Cần chốt").
- Phase 3 làm CI đỏ vì vi phạm cũ: dùng danh sách vi phạm đã chốt, chỉ chặn vi phạm mới.

## Quy ước
- Không ghi giá trị secret vào plan, báo cáo, commit. Chỉ ghi tên biến.
- Migration mới, không sửa migration cũ. Commit theo Conventional Commits, không ghi số phase hay mã plan.
