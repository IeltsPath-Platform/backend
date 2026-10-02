---
phase: 1
title: "Sắp xếp package, enum trạng thái"
status: completed
---

# Phase 1: Sắp xếp package, enum trạng thái

Không đổi hành vi. Chỉ chuyển chỗ và đổi kiểu, để các phase sau có khung đích.

## Việc

- `application/writing/EssayPrompt`, `WritingGrade` → `domain/vo` (giá trị nghiệp vụ, lưu kèm bài luận).
- `application/writing/EssayGrader`, `WritingSettings` → `application/service`.
- `application/{LessonAccess, ReviewReevaluation, AnswerSheet, LessonEvidenceReference}` → `application/service`.
- `AnswerSheet.passes` (mốc 70%) → `domain/service/PassMark`.
- Enum mới ở `domain/vo`: `ReviewStatus` (PENDING, DONE, SKIPPED), `WritingSubmissionStatus` (GRADING, PAYMENT_PENDING,
  GRADED, FAILED), `EvidenceSource` (lesson_exercise, review_set, lesson_writing, assessment).
- `api/dto` → `api/dto/request`, `api/dto/response`.
- Sửa import ở test theo package mới (test đặt mirror package).

## Kiểm

`mvn -q -pl services/learning-service -am test` pass, số test không đổi.
