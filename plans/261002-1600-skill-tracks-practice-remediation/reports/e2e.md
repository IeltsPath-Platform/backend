# E2E: skill tracks, lesson practice, review ladder

- Ngày chạy: 2026-10-03; stack `docker-compose.mvp.yml` (project `ieltspath-mvp`), qua Gateway `http://127.0.0.1:8080`; learner mới tạo bằng `/api/users/register` (email, mật khẩu, token không ghi lại).
- Đáp án đúng/sai lấy từ `content_db.question_versions.answer_spec` qua `docker exec psql` (chỉ để test chọn câu trả lời; API học viên không lộ đáp án).
- Kết quả: 47/47 kiểm tra PASS.

| Bước | Kiểm tra | Kết quả | Chi tiết khi FAIL |
| --- | --- | --- | --- |
| 0 | register new learner | PASS |  |
| 0 | login | PASS |  |
| 1 | GET topics 200 | PASS |  |
| 1 | READING, LISTENING, WRITING present | PASS |  |
| 1 | exactly one IN_PROGRESS per skill | PASS |  |
| 2 | L1 practice items LOCKED before lesson | PASS |  |
| 2 | start practice -> 409 PRACTICE_LOCKED | PASS |  |
| 3 | L1 completed | PASS |  |
| 3 | no review after lesson completion | PASS |  |
| 3 | L1 practice AVAILABLE | PASS |  |
| 4 | L1 practice PS-KP3-A 2/4: no review (no unrevealed set left) | PASS |  |
| 4 | results carry explanation | PASS |  |
| 4 | L1 practice PASSED/ALL_SETS_ATTEMPTED | PASS |  |
| 4 | L2 completed | PASS |  |
| 4 | L2 practice PS-KP1-A graded 2/4 | PASS |  |
| 4 | reviewsCreated ['PRACTICE'] | PASS |  |
| 5 | next Reading lesson -> 403 REVIEW_REQUIRED | PASS |  |
| 5 | Listening LS1 -> 200 | PASS |  |
| 6 | set package not revealed before (20000000) | PASS |  |
| 6 | set questions carry hint key | PASS |  |
| 6 | failed set -> PENDING/THEORY failedSets=1 | PASS |  |
| 7 | THEORY: set null, theoryScope KNOWLEDGE_POINT, 1 quick-check, reason SECOND_FAIL | PASS |  |
| 7 | submit set at THEORY -> 409 THEORY_REQUIRED | PASS |  |
| 7 | theory-check -> PRACTICE with explanations | PASS |  |
| 6 | set package not revealed before (26000000) | PASS |  |
| 6 | set questions carry hint key | PASS |  |
| 6 | failed set -> SKIPPED/PRACTICE failedSets=2 | PASS |  |
| 8 | review ends SKIPPED | PASS |  |
| 8 | next Reading lesson reopens | PASS |  |
| 8 | L1 practice PASSED/REVIEW_FINISHED | PASS |  |
| 9 | L3 completed | PASS |  |
| 9 | L4 completed | PASS |  |
| 9 | test-assignments -> 409 PRACTICE_REQUIRED | PASS |  |
| 9 | practice PS-KP2-A first submission counted=True passed=True | PASS |  |
| 9 | practice PS-KP4-A first submission counted=True passed=True | PASS |  |
| 9 | test-assignments after practice -> assigned | PASS |  |
| 10 | Writing testStatus NONE | PASS |  |
| 10 | W1 /complete | PASS |  |
| 10 | W2 /complete | PASS |  |
| 10 | DEMO_WRITING PASSED | PASS |  |
| 10 | Writing test-assignments -> 409 NO_TOPIC_TEST | PASS |  |
| 11 | LS1 practice LOCKED before lesson | PASS |  |
| 11 | LS1 completed | PASS |  |
| 11 | LS1 practice AVAILABLE | PASS |  |
| 11 | PS-NUM below 70%: reviewsCreated=0, transcript=True | PASS |  |
| 11 | PS-SPELL below 70%: reviewsCreated=0, transcript=True | PASS |  |
| 11 | LS1 practice PASSED/ALL_SETS_ATTEMPTED | PASS |  |
