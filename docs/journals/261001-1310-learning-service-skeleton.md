# Xóa Python, dựng khung Java

**Date**: 2026-10-01 13:10
**Component**: learning-service
**Status**: Completed

## What Happened

Xóa 135 file Python tracked; dựng Java:8086, JWT chung, Flyway chín bảng, Gateway discovery, reactor 13 module. DB:5436; cập nhật hai contract.

## Giới hạn

API học viên và consumer chưa triển khai. README/AGENTS/CLAUDE/architecture còn tham chiếu Python để PR5 cập nhật.

## Technical Details

`V1__learning_schema.sql`; learning/common-security 11 test, Gateway 8 test: pass, 0 skip; reactor compile pass. Compose thiếu `LEARNING_DB_PASSWORD`.

## What We Tried

Chạy gates; sửa logging lỗi bất ngờ tránh lộ dữ liệu; gate learning chạy lại thành công.

## Root Cause Analysis

Xóa consumer Python trước khi port xong gây khoảng trống nhận event; lựa chọn này đã được chấp nhận.

## Lessons Learned

Khung khởi động được chưa chứng minh luồng học chạy được.

## Decisions

Chọn xóa ngay theo MVP, bỏ chạy song song Python/Java vì MVP mới bỏ tutor và learner memory cũ. Không chuyển dữ liệu dev. File runtime riêng tư ngoài Git được giữ, không đọc.

## Next Steps

Người dùng thêm biến trước Compose. Maintainer làm PR2–4 tuần tự; PR5 sửa docs trước E2E. Không đọc/sửa `.env`.
