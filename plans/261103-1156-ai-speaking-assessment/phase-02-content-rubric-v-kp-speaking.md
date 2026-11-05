---
phase: 2
title: "Content rubric vĂ  KP Speaking"
status: completed
priority: P2
effort: "1d"
dependencies: [1]
---

# Phase 2: Content rubric vĂ  KP Speaking

## Overview

Cho phĂ©p bá»™ tiĂªu chĂ­ Speaking náº±m trong cĂ¹ng háº¡ táº§ng rubric cá»§a Writing (báº£ng, API quáº£n lĂ½, trang quáº£n trá»‹), thĂªm bá»‘n KP tiĂªu chĂ­ **ACTIVE** (FC, LR, GRA, PR), cĂ¹ng API ná»™i bá»™ Ä‘á»ƒ learning-service láº¥y rubric vĂ  Ă¡nh xáº¡ tiĂªu chĂ­ sang KP (D16, D17, D19). CĂ¡c KP `S55_*`, `S60_*`, `S65_*`, `S70_*` Ä‘Æ°á»£c kĂ­ch hoáº¡t á»Ÿ phase 8, khĂ´ng pháº£i á»Ÿ Ä‘Ă¢y; cáº§n thĂªm endpoint láº¥y `can_do` cá»§a KP theo danh sĂ¡ch mĂ£ (phase 5 dĂ¹ng cho khá»‘i KP cá»§a há»c viĂªn). LĂ m theo Ä‘Ăºng khuĂ´n `261021-0103-writing-criterion-kp-evidence` vĂ  `261021-2016-writing-criterion-sub-kp-judgments`.

## Requirements

- Functional: ngÆ°á»i cĂ³ quyá»n sá»­a Ä‘Æ°á»£c bá»™ tiĂªu chĂ­ Speaking qua trang quáº£n trá»‹ nhÆ° Writing (nhĂ¡p, phĂ¡t hĂ nh, phiĂªn báº£n má»›i); learning-service láº¥y Ä‘Æ°á»£c rubric Speaking Ä‘ang ACTIVE (tiĂªu chĂ­, mĂ´ táº£ theo band, `system_prompt`, `knowledgePointId` cá»§a tá»«ng tiĂªu chĂ­); láº¥y Ä‘Æ°á»£c cáº£ phiĂªn báº£n `RETIRED` theo `code` vĂ  `version`.
- Non-functional: rubric Ä‘Ă£ phĂ¡t hĂ nh báº¥t biáº¿n; khĂ´ng Ä‘á»•i hĂ nh vi Writing (test Writing cÅ© váº«n xanh); `system_prompt` cĂ³ giá»›i háº¡n Ä‘á»™ dĂ i.

## Architecture

Hiá»‡n cĂ³ (content-service, V50): `rubrics` (CHECK `task IN ('TASK_1','TASK_2')`, chá»‰ má»™t báº£n ACTIVE má»—i `task`), `rubric_criteria` (CHECK `code IN ('TR','TA','CC','LR','GRA')`), `rubric_band_descriptors`, `rubric_examples`; `RubricController`, `ManageRubricsUseCase`, `InternalRubricController`. Learning Ä‘á»c qua `LearningContentClient.getActiveRubric(task)` vĂ  `getRubricVersion(code, version)`.

Thay Ä‘á»•i:
1. Migration ná»›i CHECK: `rubrics.task` thĂªm `'SPEAKING'`; `rubric_criteria.code` thĂªm `'FC'`, `'PR'`. Chá»‰ má»™t báº£n ACTIVE má»—i `task` váº«n Ä‘Ăºng vá»›i `'SPEAKING'`.
2. ThĂªm cá»™t `rubrics.system_prompt TEXT NULL`: Ä‘oáº¡n hÆ°á»›ng dáº«n chung do ngÆ°á»i quáº£n trá»‹ viáº¿t (vai trĂ² ngÆ°á»i cháº¥m, cĂ¡ch cĂ¢n nháº¯c, lÆ°u Ă½). TiĂªu chĂ­ vĂ  mĂ´ táº£ theo band váº«n á»Ÿ cĂ¡c báº£ng cĂ³ sáºµn Ä‘á»ƒ giao diá»‡n sá»­a tá»«ng pháº§n.
3. Quy táº¯c lÆ°u vĂ  phĂ¡t hĂ nh cho `SPEAKING`: báº¯t buá»™c Ä‘á»§ FC, LR, GRA; PR cĂ³ thá»ƒ cĂ³ Ä‘á»ƒ hiá»ƒn thá»‹ cho há»c viĂªn nhÆ°ng **khĂ´ng bao giá» gá»­i cho LLM cháº¥m band** (D9); giá»›i háº¡n Ä‘á»™ dĂ i `system_prompt`; phĂ¡t hĂ nh báº£n má»›i thĂ¬ báº£n ACTIVE cÅ© chuyá»ƒn `RETIRED`; báº£n ACTIVE khĂ´ng sá»­a Ä‘Æ°á»£c (`RubricNotEditableException` sáºµn cĂ³).
4. KP: topic ACTIVE má»›i cho Speaking (nhÆ° `DEMO_WRITING`) vĂ  4 KP tiĂªu chĂ­ `DEMO_SPEAKING_CRITERION_FC/LR/GRA/PR`; báº£ng Ă¡nh xáº¡ `speaking_criterion_knowledge_points (criterion_code â†’ knowledge_point_id)` khĂ´ng gáº¯n phiĂªn báº£n rubric (cĂ¹ng lĂ½ do D2 cá»§a plan Writing). Ănh xáº¡ lĂ  dá»¯ liá»‡u seed (migration); chÆ°a sá»­a qua trang rubric á»Ÿ v1. KhĂ´ng cĂ³ báº£ng KP con; viá»‡c kĂ­ch hoáº¡t cĂ¡c KP `S55_*`, `S60_*`, `S65_*`, `S70_*` cá»§a topic `SPEAKING_FRAMEWORK` (V73) thuá»™c phase 8.
5. API ná»™i bá»™: má»Ÿ `getActiveRubric("SPEAKING")` (Ä‘Ă£ Ä‘i qua `InternalRubricController`) vĂ  tráº£ thĂªm `systemPrompt` vĂ  `knowledgePointId` cá»§a tá»«ng tiĂªu chĂ­; **endpoint tráº£ `code`, `name`, `can_do` cá»§a KP theo danh sĂ¡ch mĂ£ cĂ³ thá»ƒ khĂ´ng cáº§n ná»¯a**: `LearningContentClient.KnowledgePoint` cá»§a learning-service Ä‘Ă£ mang `canDo` qua `getTopicSequence` (xem phase 5b má»¥c 5.6). Viá»‡c cáº§n lĂ m á»Ÿ phase nĂ y chá»‰ lĂ  **kiá»ƒm tra** topic `SPEAKING_FRAMEWORK` (V103) cĂ³ trong `getTopicSequence` kĂ¨m `canDo`; chá»‰ khi khĂ´ng cĂ³ má»›i thĂªm endpoint (hiá»‡n `workItemSubjects` chá»‰ tráº£ `code` vĂ  `name`).

## Related Code Files

- Create: `services/content-service/src/main/resources/db/migration/V{next}__speaking_rubric_and_criterion_kps.sql` (`{next}` lĂ  sá»‘ trá»‘ng káº¿ tiáº¿p lĂºc lĂ m; V102 Ä‘ang dĂ¹ng á»Ÿ nhĂ¡nh `feat/practice-writing-sample-answer`, kiá»ƒm tra láº¡i)
- Modify: `domain/aggregate/Rubric.java`, `application/usecase/ManageRubricsUseCase.java`, `infrastructure/persistence/adapter/JdbcRubricRepository.java`, `api/dto/response/RubricVersionResponse.java` (vĂ  DTO request tÆ°Æ¡ng á»©ng), `api/controller/InternalRubricController.java`
- Modify (learning-service): `application/port/LearningContentClient.java` vĂ  `infrastructure/client/RestLearningContentClient.java` (rubric cĂ³ `systemPrompt` vĂ  `knowledgePointId` cá»§a tiĂªu chĂ­)
- Create (test): migration test Testcontainers (rubric Speaking, KP ACTIVE, Ă¡nh xáº¡), test use case lÆ°u vĂ  phĂ¡t hĂ nh, test WebMvc cho API má»›i
- Modify: `docs/contracts/` (há»£p Ä‘á»“ng JSON rubric Speaking)
- (ÄÆ°á»ng dáº«n Java dÆ°á»›i `services/content-service/src/main/java/com/ieltspath/content/`)

## Implementation Steps

1. Viáº¿t test migration trÆ°á»›c (Ä‘á» Ä‘Ăºng lĂ½ do): chĂ¨n rubric `SPEAKING` Ä‘áº¡t CHECK; `FC`, `PR` Ä‘Æ°á»£c nháº­n; Writing khĂ´ng Ä‘á»•i.
2. Viáº¿t migration: ná»›i CHECK, cá»™t `system_prompt`, topic vĂ  KP tiĂªu chĂ­ ACTIVE, báº£ng Ă¡nh xáº¡ tiĂªu chĂ­ sang KP, seed má»™t rubric Speaking **nhĂ¡p** (`DRAFT`, rá»—ng hoáº·c tá»‘i thiá»ƒu) Ä‘á»ƒ giao diá»‡n cĂ³ chá»— báº¯t Ä‘áº§u; khĂ´ng phĂ¡t hĂ nh rubric giáº£.
3. Má»Ÿ rá»™ng `Rubric`, `ManageRubricsUseCase` (quy táº¯c á»Ÿ má»¥c 3), repository, DTO; test.
4. API ná»™i bá»™ vĂ  há»£p Ä‘á»“ng; cáº­p nháº­t client phĂ­a learning-service; test.
5. Kiá»ƒm tra chuá»—i course, topic, KP ACTIVE báº±ng truy váº¥n trĂªn Postgres táº¡m nhÆ° plan Writing Ä‘Ă£ lĂ m.

## Success Criteria

- [ ] Táº¡o, sá»­a nhĂ¡p vĂ  phĂ¡t hĂ nh rubric `SPEAKING` hoáº¡t Ä‘á»™ng á»Ÿ backend; má»™t báº£n ACTIVE má»—i `task`; báº£n ACTIVE báº¥t biáº¿n.
- [ ] KhĂ´ng phĂ¡t hĂ nh Ä‘Æ°á»£c rubric Speaking thiáº¿u FC, LR hoáº·c GRA, hoáº·c `system_prompt` quĂ¡ giá»›i háº¡n.
- [ ] Learning láº¥y Ä‘Æ°á»£c rubric ACTIVE (kĂ¨m `systemPrompt`, `knowledgePointId`), vĂ  rubric `RETIRED` theo phiĂªn báº£n.
- [ ] 4 KP tiĂªu chĂ­ thuá»™c topic ACTIVE trong course ACTIVE vĂ  cĂ³ máº·t trong `knowledge_point_catalog` cá»§a learning.
- [ ] Test Writing cÅ© xanh; há»£p Ä‘á»“ng JSON Ä‘Ă£ chá»‘t (phase 5 vĂ  6 dá»±a vĂ o).

## Risk Assessment

- Ná»›i CHECK cháº¡m consumer Writing: cháº¡y toĂ n bá»™ test content-service; náº¿u quĂ¡ rá»™ng thĂ¬ tĂ¡ch báº£ng riĂªng cho Speaking (quyáº¿t Ä‘á»‹nh sau khi Ä‘á»c háº¿t nÆ¡i dĂ¹ng `rubrics`).
- Topic Speaking má»›i cĂ³ thá»ƒ lá»™ ra danh má»¥c há»c viĂªn: kiá»ƒm tra `RefreshLearningTopicsUseCase` trÆ°á»›c (nhÆ° plan Writing). Rollback: bá» Ă¡nh xáº¡, learning quay vá» khĂ´ng ghi evidence Speaking.
- NgÆ°á»i quáº£n trá»‹ nháº­p rubric xáº¥u: cháº·n báº±ng quy táº¯c phĂ¡t hĂ nh (má»¥c 3); pháº§n Ä‘á»‹nh dáº¡ng JSON vĂ  chá»‘ng prompt injection do code ná»‘i thĂªm (D17) nĂªn má»™t rubric sai khĂ´ng phĂ¡ Ä‘Æ°á»£c Ä‘á»‹nh dáº¡ng.

