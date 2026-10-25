# Baseline data — content schema V78

Nguồn: Testcontainers PostgreSQL `postgres:15-alpine`, Flyway target V78; đo ngày 2026-10-24. Migration mới nhất trước phase 2 là `V78__section_question_sort_order_deferrable.sql`.

## T/F/NG theo đáp án đúng

| Đáp án đúng | Câu đã xuất bản | Bộ có chứa câu |
|---|---:|---:|
| FALSE | 18 | 12 |
| NO | 4 | 4 |
| NOT_GIVEN | 21 | 13 |
| TRUE | 19 | 13 |
| YES | 2 | 2 |

Trong các bộ luyện đã xuất bản, `PS-TF-A`, `PS-TF-B`, `PS-TF-C` mỗi bộ có 1 câu đúng FALSE, 1 câu đúng NOT_GIVEN và 1 câu đúng TRUE. Sau mô phỏng backfill, mỗi mã TF/NG có tối đa 1 câu cùng bẫy trong một bộ; không bộ nào đạt ngưỡng 2 câu.

## distractors CHOICE đã có

| Mã lỗi | Câu |
|---|---:|
| CONTRADICTED_BY_PASSAGE | 4 |
| DETAIL_AS_MAIN_IDEA | 1 |
| DISTRACTOR_CORRECTED | 1 |
| INFERENCE_UNSUPPORTED | 8 |
| KEYWORD_MATCH_ONLY | 7 |
| NUMBER_FORMAT | 1 |
| OFF_TOPIC | 2 |
| PARAPHRASE_TRAP | 1 |
| SCOPE_OVERGENERALISED | 1 |
| WRONG_PARAGRAPH | 2 |
| WRONG_TIME_FRAME | 1 |

## Error type ACTIVE

Có 28 mã ACTIVE; 19 mã có `knowledge_point_id`, 9 mã không có. 19 mã có KP cha: `COLLOCATION`, `DETAIL_AS_MAIN_IDEA`, `DISTRACTOR_CORRECTED`, `INFERENCE_UNSUPPORTED`, `KEYWORD_MATCH_ONLY`, `LINKER_MISUSE`, `NUMBER_FORMAT`, `PARAPHRASE_TRAP`, `SCOPE_OVERGENERALISED`, `SPELLING`, `T1_DATA_MISREPORTED`, `T1_NO_OVERVIEW`, `TFNG_FALSE_AS_NG`, `TFNG_NG_AS_FALSE`, `UNDERDEVELOPED_IDEA`, `WORD_CHOICE`, `WORD_LIMIT_EXCEEDED`, `WRONG_PARAGRAPH`, `WRONG_SPEAKER`.

## Bộ có ít nhất 2 câu của cùng một bẫy sau mô phỏng

| Mã lỗi | KP cha | Bộ đạt ngưỡng | Số câu bẫy lớn nhất trong một bộ |
|---|---|---:|---:|
| COLLOCATION | W70_LEXICAL_PRECISION | 0 | 0 |
| DETAIL_AS_MAIN_IDEA | DR_IDEA_OR_DETAIL | 0 | 0 |
| DISTRACTOR_CORRECTED | L65_CORRECTION | 0 | 1 |
| INFERENCE_UNSUPPORTED | R65_EVIDENCE_INFERENCE | 1 | 2 |
| KEYWORD_MATCH_ONLY | TR_R_SCANNING | 0 | 1 |
| LINKER_MISUSE | DEMO_WRITING_LINKING | 0 | 0 |
| NUMBER_FORMAT | LS_NUM | 0 | 1 |
| PARAPHRASE_TRAP | R65_PARAPHRASE | 0 | 1 |
| SCOPE_OVERGENERALISED | R70_HEDGING_SCOPE | 0 | 1 |
| SPELLING | LS_SPELL | 0 | 0 |
| T1_DATA_MISREPORTED | W60_T1_KEY_FEATURES | 0 | 0 |
| T1_NO_OVERVIEW | W60_T1_KEY_FEATURES | 0 | 0 |
| TFNG_FALSE_AS_NG | TFNG_FALSE_VS_NOT_GIVEN | 0 | 1 |
| TFNG_NG_AS_FALSE | TFNG_FALSE_VS_NOT_GIVEN | 0 | 1 |
| UNDERDEVELOPED_IDEA | W70_IDEA_DEVELOPMENT | 0 | 0 |
| WORD_CHOICE | W70_LEXICAL_PRECISION | 0 | 0 |
| WORD_LIMIT_EXCEEDED | PR_SENTENCE_COMPLETION | 0 | 0 |
| WRONG_PARAGRAPH | PR_MATCHING_INFORMATION | 0 | 1 |
| WRONG_SPEAKER | L65_SPEAKER_PURPOSE | 0 | 0 |

**Điều kiện dừng của plan đã xảy ra:** không có bộ T/F/NG nào có ≥2 câu của `TFNG_NG_AS_FALSE` hoặc `TFNG_FALSE_AS_NG`. Dừng trước phase 2 để xin hướng xử lý; chưa thu thập snapshot BandEstimator/báo cáo học viên. Không có dữ liệu production/shared DB nào được đọc.
