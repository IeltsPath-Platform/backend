---
phase: 8
title: "Luá»“ng há»c Speaking: kĂ­ch hoáº¡t KP, bĂ i há»c, bá»™ luyá»‡n táº­p"
status: completed
priority: P1
effort: "2d ká»¹ thuáº­t + cĂ´ng soáº¡n ná»™i dung (chÆ°a Æ°á»›c lÆ°á»£ng)"
dependencies: []
---

# Phase 8: Luá»“ng há»c Speaking: kĂ­ch hoáº¡t KP, bĂ i há»c, bá»™ luyá»‡n táº­p

## Overview

Hiá»‡n Speaking cĂ³ 8 KP nhÆ°ng khĂ´ng há»c viĂªn nĂ o táº¡o Ä‘Æ°á»£c mastery cho chĂºng. Phase nĂ y lĂ m cho luá»“ng há»c Speaking cháº¡y nhÆ° Reading, Listening, Writing: topic vĂ  KP ACTIVE trong má»™t course ACTIVE, cĂ³ bĂ i há»c vĂ  bá»™ luyá»‡n táº­p gáº¯n KP, há»c viĂªn lĂ m bĂ i thĂ¬ `kp_evidence` vĂ  `kp_mastery_profiles` tá»± Ä‘Æ°á»£c ghi. Khá»‘i KP cá»§a há»c viĂªn trong prompt cháº¥m AI (D18) láº¥y tá»« Ä‘Ă¢y. **Pháº£i xong trÆ°á»›c khi báº­t cháº¥m AI (D20).**

Dá»¯ liá»‡u kiá»ƒm tra 2026-11-03 (content_db): topic `SPEAKING_FRAMEWORK` INACTIVE, `course_id` rá»—ng, 0 lesson, 0 block; 8 KP `S55_PART1_FAMILIAR_ANSWERS`, `S55_PRONUNCIATION_BASICS`, `S60_FLUENCY_COHERENCE`, `S60_PART2_LONG_TURN`, `S60_SPEAKING_VOCABULARY`, `S65_GRAMMAR_RANGE`, `S65_PART3_DISCUSSION`, `S70_PRONUNCIATION_FEATURES` Ä‘á»u INACTIVE, 0 lesson vĂ  0 cĂ¢u há»i gáº¯n vĂ o; cĂ¢u há»i Speaking chá»‰ cĂ³ loáº¡i `SPEAKING` (56, má»¥c Ä‘Ă­ch `EXAM`/`PLACEMENT`), khĂ´ng cĂ³ cĂ¢u `LEARNING` nĂ o. Courses: `IELTS_5_5` vĂ  `IELTS_6_5` ACTIVE, `IELTS_6_0` vĂ  `IELTS_7_0` INACTIVE.

## Requirements

- Functional: topic Speaking thuá»™c course ACTIVE vĂ  hiá»‡n ra Ä‘Ăºng chá»— trong lá»™ trĂ¬nh; má»—i KP Speaking Ä‘Æ°á»£c kĂ­ch hoáº¡t cĂ³ Ă­t nháº¥t má»™t bĂ i há»c vĂ  má»™t bá»™ luyá»‡n táº­p (tá»‘i thiá»ƒu 3 cĂ¢u, theo `MIN_SET_QUESTIONS` cá»§a learning); há»c viĂªn lĂ m bĂ i thĂ¬ mastery KP Speaking Ä‘Æ°á»£c ghi; `knowledge_point_catalog` cá»§a learning cĂ³ cĂ¡c KP nĂ y vá»›i `has_practice_set` Ä‘Ăºng.
- Non-functional: khĂ´ng lĂ m lá»™ topic cho há»c viĂªn khi chÆ°a Ä‘á»§ ná»™i dung (kiá»ƒm tra `RefreshLearningTopicsUseCase`); khĂ´ng Ä‘á»•i hĂ nh vi Reading, Listening, Writing; ná»™i dung pháº£i qua quy trĂ¬nh duyá»‡t ná»™i dung hiá»‡n cĂ³.

## Architecture

BĂ i táº­p tá»± cháº¥m Ä‘Æ°á»£c á»Ÿ há»‡ thá»‘ng nĂ y lĂ  tráº¯c nghiá»‡m, Ä‘iá»n chá»— trá»‘ng, Ä‘Ăºng sai (Reading, Listening, Writing Ä‘á»u dĂ¹ng: vĂ­ dá»¥ Writing LEARNING cĂ³ 157 tráº¯c nghiá»‡m, 42 Ä‘iá»n chá»— trá»‘ng, 29 essay). Vá»›i Speaking, loáº¡i nĂ y kiá»ƒm tra **kiáº¿n thá»©c vĂ  chiáº¿n lÆ°á»£c** (chá»n cĂ¢u tráº£ lá»i má»Ÿ rá»™ng Ä‘Ăºng cho Part 1, sáº¯p xáº¿p bĂ i nĂ³i Part 2, chá»n cĂ¡ch paraphrase khi thiáº¿u tá»«, nháº­n ra lá»—i ngá»¯ phĂ¡p trong cĂ¢u nĂ³i Ä‘Ă£ chĂ©p), **khĂ´ng** chá»©ng minh kháº£ nÄƒng nĂ³i. CĂ¢u loáº¡i `SPEAKING` khĂ´ng tá»± cháº¥m Ä‘Æ°á»£c; ghi Ă¢m váº«n dĂ¹ng cho cháº¥m AI, khĂ´ng sinh báº±ng chá»©ng á»Ÿ phase nĂ y. Äiá»u nĂ y pháº£i nĂ³i rĂµ trong tĂ i liá»‡u Ä‘á»ƒ khĂ´ng hiá»ƒu mastery Speaking lĂ  Ä‘iá»ƒm nĂ³i.

CĂ¡c bÆ°á»›c:
1. **Course vĂ  topic:** chá»n course ACTIVE Ä‘á»ƒ gáº¯n topic Speaking (`IELTS_5_5` hay `IELTS_6_5`, hay cáº£ hai, cĂ¢u há»i má»Ÿ); Ä‘áº·t `sort_order` vĂ  chuá»—i tiĂªn quyáº¿t giá»¯a cĂ¡c KP (Ä‘á»“ thá»‹ tiĂªn quyáº¿t Ä‘Ă£ cĂ³ á»Ÿ V73: vĂ­ dá»¥ `S55_PART1_FAMILIAR_ANSWERS` trÆ°á»›c `S60_PART2_LONG_TURN`).
2. **KĂ­ch hoáº¡t:** migration chuyá»ƒn topic vĂ  cĂ¡c KP Ä‘á»§ ná»™i dung sang ACTIVE; KP chÆ°a cĂ³ ná»™i dung giá»¯ INACTIVE (khĂ´ng kĂ­ch hoáº¡t hĂ ng loáº¡t khi chÆ°a soáº¡n).
3. **Ná»™i dung:** bĂ i há»c (`lesson_blocks`: TEXT, EXERCISE, ASSET, VOCABULARY) vĂ  bá»™ luyá»‡n táº­p (`PRACTICE_SET`) gáº¯n qua `lesson_knowledge_points`, `question_knowledge_points`. DĂ¹ng cĂ´ng cá»¥ soáº¡n cĂ³ sáºµn cá»§a content-service: `LessonAuthoringAdminController`, `CourseImportController`, `AiCourseDraftController`, `AiPracticeSetDraftController`; **ná»™i dung do ngÆ°á»i dĂ¹ng duyá»‡t**, khĂ´ng Ä‘Æ°a báº£n AI soáº¡n vĂ o phĂ¡t hĂ nh khi chÆ°a duyá»‡t.
4. **Learning:** kiá»ƒm tra `knowledge_point_catalog` Ä‘Æ°á»£c lĂ m má»›i tá»« content (`RefreshLearningTopicsUseCase`), quy táº¯c má»Ÿ khĂ³a topic vĂ  mastery cháº¡y cho KP Speaking nhÆ° cĂ¡c ká»¹ nÄƒng khĂ¡c, vĂ  `LearningSkill.SPEAKING` Ä‘Æ°á»£c cĂ¡c luá»“ng nĂ y xá»­ lĂ½ (kiá»ƒm tra trÆ°á»›c, vĂ¬ Writing, Reading, Listening Ä‘Ă£ cháº¡y cĂ²n Speaking chÆ°a tá»«ng cĂ³ KP ACTIVE).
5. **Frontend:** kiá»ƒm tra trang lá»™ trĂ¬nh, bĂ i há»c, luyá»‡n táº­p hiá»ƒn thá»‹ topic Speaking Ä‘Ăºng; khĂ´ng giáº£ Ä‘á»‹nh nĂ³ Ä‘Ă£ cĂ³.

## Related Code Files

- Create: `services/content-service/src/main/resources/db/migration/V{next}__activate_speaking_learning_track.sql` (gáº¯n course, kĂ­ch hoáº¡t topic vĂ  KP Ä‘á»§ ná»™i dung; `{next}` lĂ  sá»‘ trá»‘ng lĂºc lĂ m, kiá»ƒm tra láº¡i)
- Create: ná»™i dung bĂ i há»c vĂ  bá»™ luyá»‡n táº­p qua cĂ´ng cá»¥ soáº¡n cĂ³ sáºµn (khĂ´ng pháº£i file code; ghi láº¡i cĂ¡ch nháº­p vĂ  nguá»“n trong `reports/`)
- Review: `RefreshLearningTopicsUseCase`, `knowledge_point_catalog`, Ä‘Æ°á»ng má»Ÿ khĂ³a topic vĂ  lesson cá»§a learning-service, mĂ n lá»™ trĂ¬nh á»Ÿ frontend (tĂ¬m khi lĂ m)
- Modify (náº¿u cáº§n): code learning-service chá»— chÆ°a há»— trá»£ `LearningSkill.SPEAKING`
- Create (test): test migration (topic gáº¯n course, KP ACTIVE Ä‘Ăºng danh sĂ¡ch), test learning ghi mastery cho má»™t KP Speaking qua má»™t bĂ i luyá»‡n táº­p
- Modify: `docs/` mĂ´ táº£ luá»“ng há»c Speaking vĂ  ghi chĂº mastery Speaking tá»« bĂ i táº­p lĂ  kiáº¿n thá»©c, khĂ´ng pháº£i Ä‘iá»ƒm nĂ³i

## Implementation Steps

1. Äá»c cĂ¡ch Writing Ä‘Æ°á»£c kĂ­ch hoáº¡t (migration vĂ  test cá»§a plan `261021-0103` vĂ  `261021-2016`) vĂ  `RefreshLearningTopicsUseCase`; chá»‘t course gáº¯n topic, danh sĂ¡ch KP kĂ­ch hoáº¡t Ä‘á»£t Ä‘áº§u, vĂ  tiĂªu chĂ­ "Ä‘á»§ ná»™i dung".
2. Soáº¡n ná»™i dung cho Ä‘á»£t KP Ä‘áº§u (Ä‘á» xuáº¥t báº¯t Ä‘áº§u vá»›i `S55_PART1_FAMILIAR_ANSWERS`, `S60_FLUENCY_COHERENCE`, `S60_SPEAKING_VOCABULARY`, `S65_GRAMMAR_RANGE`: khá»›p tiĂªu chĂ­ FC, LR, GRA nĂªn khá»‘i KP trong prompt cĂ³ Ă­ch nháº¥t); má»—i KP: bĂ i há»c + bá»™ luyá»‡n táº­p â‰¥ 3 cĂ¢u `LEARNING`.
3. Test migration trÆ°á»›c (Ä‘á» Ä‘Ăºng lĂ½ do), rá»“i viáº¿t migration kĂ­ch hoáº¡t.
4. Kiá»ƒm tra trĂªn Postgres táº¡m: chuá»—i course â†’ topic â†’ KP ACTIVE; `knowledge_point_catalog` nháº­n KP; há»c viĂªn thá»­ lĂ m bĂ i thĂ¬ cĂ³ dĂ²ng `kp_mastery_profiles`.
5. Kiá»ƒm tra frontend lá»™ trĂ¬nh, bĂ i há»c, luyá»‡n táº­p hiá»ƒn thá»‹ Ä‘Ăºng; Reading, Listening, Writing khĂ´ng Ä‘á»•i.
6. Cáº­p nháº­t tĂ i liá»‡u; bĂ¡o cĂ¡o cho ngÆ°á»i dĂ¹ng danh sĂ¡ch KP Ä‘Ă£ kĂ­ch hoáº¡t vĂ  KP cĂ²n INACTIVE.

## Success Criteria

- [ ] Topic Speaking thuá»™c course ACTIVE; má»—i KP kĂ­ch hoáº¡t cĂ³ bĂ i há»c vĂ  bá»™ luyá»‡n táº­p â‰¥ 3 cĂ¢u `LEARNING`; KP chÆ°a Ä‘á»§ ná»™i dung váº«n INACTIVE.
- [ ] Há»c viĂªn thá»­ lĂ m bĂ i luyá»‡n táº­p Speaking thĂ¬ `kp_evidence` vĂ  `kp_mastery_profiles` cĂ³ dĂ²ng cho KP Speaking Ä‘Ă³ (kiá»ƒm tra báº±ng test vĂ  báº±ng truy váº¥n).
- [ ] `knowledge_point_catalog` cá»§a learning cĂ³ cĂ¡c KP Speaking vá»›i `has_practice_set` Ä‘Ăºng.
- [ ] Test cá»§a Reading, Listening, Writing vĂ  toĂ n bá»™ test content-service, learning-service xanh.
- [ ] Ná»™i dung Ä‘Ă£ Ä‘Æ°á»£c ngÆ°á»i dĂ¹ng duyá»‡t; tĂ i liá»‡u ghi rĂµ mastery Speaking tá»« bĂ i táº­p lĂ  kiáº¿n thá»©c vĂ  chiáº¿n lÆ°á»£c.

## Risk Assessment

- CĂ´ng soáº¡n ná»™i dung lĂ  pháº§n lá»›n cĂ´ng sá»©c vĂ  chÆ°a Æ°á»›c lÆ°á»£ng Ä‘Æ°á»£c (sá»‘ KP, sá»‘ bĂ i, ngÆ°á»i soáº¡n). LĂ m theo Ä‘á»£t: 4 KP Ä‘áº§u á»©ng vá»›i FC, LR, GRA; KP cĂ²n láº¡i sau.
- Mastery Speaking tá»« tráº¯c nghiá»‡m cĂ³ thá»ƒ cao dĂ¹ ngÆ°á»i há»c nĂ³i kĂ©m, lĂ m khá»‘i KP trong prompt Ä‘Ă¡nh lá»‡ch nháº­n xĂ©t. Giáº£m báº±ng cĂ¡ch dáº·n LLM chá»‰ dĂ¹ng KP lĂ m gá»£i Ă½ vĂ  pháº£i cĂ³ dáº«n chá»©ng trong transcript (D8), vĂ  gáº¯n nhĂ£n nguá»“n KP trong prompt.
- KĂ­ch hoáº¡t topic quĂ¡ sá»›m lĂ m lá»™ topic trá»‘ng cho há»c viĂªn: chá»‰ kĂ­ch hoáº¡t KP Ä‘á»§ ná»™i dung; kiá»ƒm tra `RefreshLearningTopicsUseCase`.
- CĂ³ thá»ƒ cĂ³ chá»— trong learning hoáº·c frontend chÆ°a tá»«ng cháº¡y vá»›i KP Speaking ACTIVE: phĂ¡t hiá»‡n á»Ÿ bÆ°á»›c 4 vĂ  5, sá»­a trong phase nĂ y.
- Rollback: chuyá»ƒn topic vĂ  KP vá» INACTIVE; evidence Ä‘Ă£ ghi giá»¯ nguyĂªn.

