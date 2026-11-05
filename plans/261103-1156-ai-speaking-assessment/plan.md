---
title: "Cháº¥m Speaking báº±ng AI (Azure Speech + LLM)"
description: "Há»c viĂªn ná»™p ghi Ă¢m Speaking luyá»‡n táº­p; Azure AI Speech chuyá»ƒn chá»¯ vĂ  cháº¥m phĂ¡t Ă¢m, má»™t LLM khĂ¡c Azure OpenAI cháº¥m FC/LR/GRA vĂ  Ä‘Ă¡nh giĂ¡ tá»«ng KP; Ä‘iá»ƒm chá»‰ trá»« khi cháº¥m xong; káº¿t quáº£ ghi kp_evidence vĂ  tĂ­nh láº¡i mastery."
status: in-progress
priority: P2
effort: "~15d (Æ°á»›c lÆ°á»£ng, chÆ°a cĂ³ sá»‘ Ä‘o)"
branch: "feat/ai-speaking-assessment"
tags: [speaking, learning-service, content-service, frontend, azure-speech, llm, mastery]
blockedBy: []
blocks: []
created: "2026-11-03T04:59:47.642Z"
revised: "2026-11-03"
createdBy: "ck:plan"
source: skill
---

# Cháº¥m Speaking báº±ng AI (Azure Speech + LLM)

SÆ¡ Ä‘á»“ luá»“ng Ä‘Ă£ duyá»‡t qua nhiá»u vĂ²ng: <https://claude.ai/artifact/MHfidz68efiLUQgHzaAkMr>. Khi lá»‡ch vá»›i sÆ¡ Ä‘á»“ thĂ¬ **plan nĂ y Ä‘Ăºng**
(sÆ¡ Ä‘á»“ váº½ trÆ°á»›c khi chá»‘t ranh giá»›i service vĂ  quyáº¿t Ä‘á»‹nh LLM).

## Má»¥c tiĂªu

Há»c viĂªn lĂ m Speaking luyá»‡n táº­p (cáº£ Ä‘á» hoáº·c má»™t part láº»), báº¥m Ná»™p, vĂ  nháº­n band 4 tiĂªu chĂ­ (Fluency & Coherence, Lexical
Resource, Grammatical Range & Accuracy, Pronunciation), nháº­n xĂ©t cĂ³ dáº«n chá»©ng, vĂ  danh sĂ¡ch tiĂªu chĂ­ cáº§n luyá»‡n. Sau khi cháº¥m, code ghi káº¿t quáº£ vĂ o
`kp_evidence` vĂ  mastery cá»§a 4 KP tiĂªu chĂ­ (D19). LLM nháº­n KP cá»§a chĂ­nh há»c viĂªn chá»‰ Ä‘á»ƒ nháº­n xĂ©t (D8, D18). ÄĂ¢y lĂ  **Ä‘iá»ƒm Æ°á»›c lÆ°á»£ng Ä‘á»ƒ luyá»‡n táº­p**, giĂ¡m kháº£o váº«n lĂ  Ä‘iá»ƒm chĂ­nh thá»©c.

KhĂ´ng lĂ m trong plan nĂ y: cháº¥m placement Speaking (Ä‘ang dĂ¹ng band cá»‘ Ä‘á»‹nh cáº¥u hĂ¬nh), luá»“ng giĂ¡m kháº£o Speaking cá»§a
assessment-service, thĂ´ng bĂ¡o Ä‘áº©y khi cháº¥m xong (v1 dĂ¹ng polling, xem CĂ¢u há»i má»Ÿ), ghi Ă¢m Speaking trong lesson.

## Quyáº¿t Ä‘á»‹nh Ä‘Ă£ chá»‘t

| # | Quyáº¿t Ä‘á»‹nh | Nguá»“n / lĂ½ do |
|---|---|---|
| D1 | **learning-service sá»Ÿ há»¯u luá»“ng cháº¥m Speaking AI**, theo khuĂ´n Writing (`SubmitFreeWritingUseCase`, `EssaySubmissionFlow`) | NĂ³ Ä‘Ă£ giá»¯ Ä‘iá»ƒm (`AccessClient.debit`), `kp_evidence`, mastery, quota LLM (`LlmUsageQuota`) vĂ  `LlmClient`. Cháº¥m á»Ÿ assessment-service rá»“i báº¯n event sang learning thĂ¬ thĂªm má»™t vĂ²ng event chá»‰ Ä‘á»ƒ ghi evidence |
| D2 | LLM lĂ  nhĂ  cung cáº¥p **khĂ¡c Azure OpenAI**, qua `LlmClient` OpenAI-compatible sáºµn cĂ³ (`LEARNING_LLM_*`); Ä‘á»•i nhĂ  cung cáº¥p chá»‰ lĂ  Ä‘á»•i biáº¿n mĂ´i trÆ°á»ng | NgÆ°á»i dĂ¹ng chá»‘t; `OpenAiCompatibleLlmClient` Ä‘Ă£ cĂ³ base-url, api-key, model |
| D3 | Azure AI Speech lĂ  báº¯t buá»™c, qua port má»›i `SpeechAnalysisClient` (adapter náº±m á»Ÿ infrastructure, port á»Ÿ application) | NgÆ°á»i dĂ¹ng chá»‘t; cĂ¹ng kiá»ƒu `LlmClient` nĂªn service khĂ´ng biáº¿t Azure |
| D4 | **Kiá»ƒm tra Ä‘iá»ƒm chá»‰ á»Ÿ backend, khi nháº­n file ghi Ă¢m** (chá»‰ xem sá»‘ dÆ°, chÆ°a trá»«; thiáº¿u thĂ¬ tá»« chá»‘i trÆ°á»›c khi file lĂªn Storage; frontend khĂ´ng cĂ³ bÆ°á»›c kiá»ƒm tra riĂªng). **Cháº¥m xong má»›i trá»«**, má»™t láº§n, idempotent theo `requestId`. Háº¿t Ä‘iá»ƒm lĂºc cháº¥m xong thĂ¬ `PAYMENT_PENDING`, káº¿t quáº£ giá»¯ láº¡i | NgÆ°á»i dĂ¹ng chá»‘t; Ä‘Ăºng vá»›i `EssaySubmissionFlow.requireBalance/charge`. SÆ¡ Ä‘á»“ ghi "á»Ÿ file Ä‘áº§u"; code kiá»ƒm tra á»Ÿ má»—i file (Ä‘á»c, ráº») vĂ  chá»‰ tá»‘i Æ°u thĂ nh má»™t láº§n náº¿u Ä‘o tháº¥y cháº­m |
| D5 | Ghi Ă¢m **giá»¯ trong trĂ¬nh duyá»‡t** Ä‘áº¿n khi báº¥m Ná»™p; file chá»‰ lĂªn Storage sau khi Ä‘á»§ Ä‘iá»ƒm. KhĂ´ng cĂ³ "bĂ i nhĂ¡p" phĂ­a server | NgÆ°á»i dĂ¹ng chá»‘t. Há»‡ quáº£: F5 máº¥t bĂ i (cáº£nh bĂ¡o rá»i trang; IndexedDB Ä‘á»ƒ sau) |
| D6 | Cháº¥m cháº¡y **ná»n** báº±ng job `@Scheduled` poller trong learning-service (Ä‘Ă£ cĂ³ nhiá»u job cĂ¹ng kiá»ƒu) vá»›i tráº¡ng thĂ¡i vĂ  phĂ¡t hiá»‡n treo; lá»—i quĂ¡ sá»‘ láº§n thĂ¬ khĂ´ng trá»« Ä‘iá»ƒm vĂ  chuyá»ƒn tráº¡ng thĂ¡i `NEEDS_EXAMINER` | Má»™t bĂ i máº¥t vĂ i chá»¥c giĂ¢y Ä‘áº¿n hÆ¡n má»™t phĂºt, quĂ¡ thá»i gian chá» cá»§a Gateway; Writing cháº¥m trong request (20s) nĂªn khĂ´ng Ă¡p dá»¥ng nguyĂªn |
| D7 | Pháº¡m vi ná»™p: `PART` hoáº·c `FULL`. Band tá»•ng **chá»‰ khi Ä‘á»§ 3 part**; part láº» chá»‰ cĂ³ nháº­n xĂ©t vĂ  band theo part | NgÆ°á»i dĂ¹ng chá»‘t |
| D8 | **Má»™t request LLM** cho cáº£ bĂ i, phiĂªn báº£n prompt `spk1`. LLM cháº¥m FC, LR, GRA theo bá»™ tiĂªu chĂ­ (band kĂ¨m dáº«n chá»©ng) vĂ  viáº¿t nháº­n xĂ©t. **LLM nháº­n KP cá»§a chĂ­nh há»c viĂªn Ä‘Ă³** (D18) **chá»‰ lĂ m gá»£i Ă½ Ä‘á»ƒ nháº­n xĂ©t** cho Ä‘Ăºng chá»— yáº¿u; nĂ³ khĂ´ng cháº¥m KP vĂ  khĂ´ng tráº£ judgment tá»«ng KP. Backend kiá»ƒm tra: Ä‘á»§ tiĂªu chĂ­, band bÆ°á»›c 0.5 trong 0â€“9, mĂ£ KP LLM nĂªu thuá»™c danh sĂ¡ch Ä‘Ă£ Ä‘Æ°a vĂ  cĂ³ Ä‘oáº¡n trĂ­ch trong transcript. Chá»¯ tá»± do **chá»‰ Ä‘á»ƒ hiá»ƒn thá»‹**, khĂ´ng bao giá» dĂ¹ng Ä‘á»ƒ cáº­p nháº­t mastery | NgÆ°á»i dĂ¹ng chá»‘t: Ä‘Æ°a KP cá»§a há»c viĂªn cho LLM Ä‘á»ƒ nháº­n xĂ©t theo chá»— yáº¿u; lá»™ trĂ¬nh váº«n do code cáº­p nháº­t sau (D19). Cháº¥m tá»«ng KP (kiá»ƒu `kpj2` cá»§a Writing) lĂ  pháº§n má»Ÿ rá»™ng sau |
| D9 | **Pronunciation band do code tĂ­nh** tá»« Ä‘iá»ƒm Azure qua báº£ng Ă¡nh xáº¡ cĂ³ phiĂªn báº£n; LLM khĂ´ng Ä‘Æ°á»£c tá»± cho band PR. Báº£ng ban Ä‘áº§u lĂ  táº¡m thá»i cho Ä‘áº¿n khi hiá»‡u chá»‰nh vá»›i Ä‘iá»ƒm giĂ¡m kháº£o (phase 7) | Azure cháº¥m theo chuáº©n báº£n ngá»¯ chá»© khĂ´ng theo band IELTS |
| D10 | Fluency: LLM nháº­n **sá»‘ Ä‘o nhá»‹p nĂ³i** (sá»‘ tá»« má»—i phĂºt, thá»i lÆ°á»£ng, sá»‘ quĂ£ng ngáº¯t dĂ i) cĂ¹ng transcript; OpenAI-kiá»ƒu LLM chá»‰ Ä‘á»c chá»¯ nĂªn khĂ´ng tá»± suy ra Ä‘Æ°á»£c | ÄĂ£ tháº£o luáº­n trong phiĂªn |
| D11 | Audio ghi báº±ng trĂ¬nh duyá»‡t lĂ  `webm/opus`; chuyá»ƒn sang WAV 16 kHz mono Ä‘á»ƒ Azure nháº­n. PhÆ°Æ¡ng Ă¡n máº·c Ä‘á»‹nh: **frontend ghi WAV** (Ä‘Ă£ chá»‘t, Ä‘Ă£ lĂ m), server chá»‰ nháº­n WAV PCM 16 kHz mono 16-bit. Giá»›i háº¡n má»™t ghi Ă¢m lĂ  **10 MB** (khoáº£ng 5 phĂºt 15 giĂ¢y á»Ÿ 32 KB/giĂ¢y; bucket `speaking-recordings` pháº£i Ä‘Æ°á»£c nĂ¢ng lĂªn Ă­t nháº¥t 10 MB trĂªn Supabase, trÆ°á»›c Ä‘Ă³ lĂ  5 MB vĂ  chá»‰ chá»©a khoáº£ng 2 phĂºt 43 giĂ¢y). KhĂ´ng cáº¯t ghi Ă¢m cá»§a há»c viĂªn: cĂ¢u quĂ¡ dĂ i thĂ¬ bá»‹ cháº·n vĂ  bĂ¡o, khĂ´ng bá»‹ cáº¯t | Xem Rá»§i ro R3; phase 1 xĂ¡c nháº­n |
| D12 | **DĂ¹ng chung bucket `speaking-recordings`** (riĂªng tÆ°, Ä‘Ă£ cĂ³) nhÆ°ng learning-service chá»‰ ghi dÆ°á»›i tiá»n tá»‘ riĂªng `practice/`; tiá»n tá»‘ `speaking/` cá»§a assessment-service khĂ´ng Ä‘á»¥ng tá»›i. KhĂ³a: `practice/{userId}/{yyyy-MM-dd}_{submissionId}/part{n}_q{nn}.wav` (ghi láº¡i sau lá»—i thĂ¬ `_r2`, `_r3`). Endpoint upload riĂªng cá»§a learning-service. (Äá»•i tá»« bucket `speaking-practice` riĂªng theo yĂªu cáº§u cá»§a ngÆ°á»i dĂ¹ng 2026-11-04.) | Má»™t bucket Ä‘Ă£ cáº¥u hĂ¬nh sáºµn (riĂªng tÆ°, 5 MB, cĂ³ `audio/wav`). TĂ¡ch tiá»n tá»‘ Ä‘á»ƒ hai service khĂ´ng ghi Ä‘Ă¨ nhau. CĂ¢y thÆ° má»¥c: ngÆ°á»i, ngĂ y, bĂ i ná»™p, part vĂ  cĂ¢u, nĂªn Ä‘á»c tĂªn lĂ  biáº¿t file cá»§a ai, cá»§a bĂ i nĂ o; xĂ³a theo háº¡n lÆ°u cÅ©ng theo tá»«ng thÆ° má»¥c bĂ i ná»™p. `put` dĂ¹ng `x-upsert: false` nĂªn ghi trĂ¹ng khĂ³a bá»‹ 409: má»—i láº§n ghi láº¡i dĂ¹ng háº­u tá»‘ má»›i. Cáº§n thĂªm `common-storage` vĂ o learning-service vĂ  cáº­p nháº­t báº£ng bucket trong `docs/system-architecture.md` (chá»§ bucket thĂ nh assessment vĂ  learning) |
| D21 | Thá»© tá»± ná»™p: **táº¡o bĂ i ná»™p trÆ°á»›c, upload sau**. `POST submissions` kiá»ƒm tra sá»‘ dÆ° (chá»‰ Ä‘á»c), consent, bĂ i Ä‘ang chá» rá»“i má»›i cho phĂ©p upload; thiáº¿u Ä‘iá»ƒm thĂ¬ 402 vĂ  chÆ°a cĂ³ file nĂ o lĂªn Storage | NgÆ°á»i dĂ¹ng yĂªu cáº§u: nháº­p key Ä‘á»§ point rá»“i má»›i lÆ°u file. CĂ³ `submissionId` tá»« Ä‘áº§u nĂªn thÆ° má»¥c D12 cĂ³ tĂªn Ä‘Ăºng ngay lĂºc ghi, vĂ  kiá»ƒm sá»‘ dÆ° chá»‰ má»™t láº§n cho cáº£ Ä‘á»£t |
| D22 | Káº¿t quáº£ cháº¥m lÆ°u **hai lá»›p**: `display` (hiá»‡n cho há»c viĂªn) vĂ  `signals` (lá»—i `SPK_*`, quan sĂ¡t KP cĂ³ dáº«n chá»©ng Ä‘Ă£ kiá»ƒm, cháº¥t lÆ°á»£ng transcript; **chá»‰ ghi láº¡i, `applied=false`**, chÆ°a ghi `kp_evidence`). Lá»™ trĂ¬nh váº«n do code cáº­p nháº­t tá»« band tiĂªu chĂ­ (D19). Äáº·c táº£ á»Ÿ [phase 5b](./phase-05b-llm-prompt-va-ket-qua-chi-tiet.md) | NgÆ°á»i dĂ¹ng muá»‘n cĂ³ dá»¯ liá»‡u Ä‘á»ƒ dĂ¹ng cáº­p nháº­t lá»™ trĂ¬nh sau. Má»Ÿ rá»™ng D8 theo hÆ°á»›ng chá»‰ lÆ°u: khĂ´ng Ä‘á»ƒ quan sĂ¡t KP cá»§a LLM tĂ¡c Ä‘á»™ng mastery trÆ°á»›c khi Ä‘o thiĂªn kiáº¿n theo danh sĂ¡ch KP (A/B á»Ÿ phase 1 vĂ  7) |
| D13 | Transcript vĂ  káº¿t quáº£ phĂ¢n tĂ­ch cĂ³ cĂ¹ng thá»i háº¡n lÆ°u vá»›i ghi Ă¢m (90 ngĂ y, cáº¥u hĂ¬nh), xĂ³a báº±ng job dá»n cĂ³ sáºµn kiá»ƒu `SpeakingRecordingRetentionScheduler` | Giá»ng nĂ³i vĂ  transcript lĂ  dá»¯ liá»‡u cĂ¡ nhĂ¢n |
| D14 | YĂªu cáº§u ná»™p pháº£i cĂ³ `consent: true`; lÆ°u `consent_at` trĂªn bĂ i ná»™p | Dá»¯ liá»‡u giá»ng nĂ³i gá»­i ra Azure vĂ  LLM; ná»™i dung vÄƒn báº£n Ä‘á»“ng Ă½ chá» phĂ¡p lĂ½ duyá»‡t (phase 1) |
| D15 | Má»—i há»c viĂªn chá»‰ cĂ³ **má»™t bĂ i Speaking AI Ä‘ang chá»** táº¡i má»™t thá»i Ä‘iá»ƒm | Chá»‘ng ná»™p nhiá»u bĂ i báº±ng má»™t sá»‘ dÆ° khi Ä‘iá»ƒm chá»‰ trá»« sau; giá»‘ng `gradingInProgress` cá»§a Writing |
| D16 | **Bá»™ tiĂªu chĂ­ (rubric) cá»§a Writing vĂ  Speaking náº±m trong content-service vĂ  Ä‘Æ°á»£c sá»­a qua trang quáº£n trá»‹** (`RubricsPage`, `/content-author/rubrics`, role `CONTENT_AUTHOR`): nhĂ¡p, phĂ¡t hĂ nh thĂ nh ACTIVE báº¥t biáº¿n, Ä‘á»•i thĂ¬ táº¡o phiĂªn báº£n má»›i. Speaking dĂ¹ng cĂ¹ng báº£ng vá»›i `task = 'SPEAKING'`; thĂªm cá»™t `rubrics.system_prompt` (nullable) vĂ  cho phĂ©p mĂ£ tiĂªu chĂ­ `FC`, `PR` | ÄĂ£ cĂ³ báº£ng, `RubricController`, `ManageRubricsUseCase` vĂ  trang quáº£n trá»‹ cho Writing; learning Ä‘á»c qua `LearningContentClient.getActiveRubric(task)`. KhĂ´ng cáº§n cá»™t "cháº¥m báº±ng Azure": PR luĂ´n do Azure vĂ  code tĂ­nh |
| D17 | **LĂºc cháº¥m, sau khi Azure xong vĂ  ngay trÆ°á»›c khi gá»i LLM**, learning-service láº¥y rubric ACTIVE vĂ  Ă¡nh xáº¡ tiĂªu chĂ­ sang KP tá»« content-service, ghĂ©p system prompt = rubric (`system_prompt`, tiĂªu chĂ­, mĂ´ táº£ theo band) + pháº§n **do code sá»Ÿ há»¯u** (Ä‘á»‹nh dáº¡ng JSON, luáº­t chá»‘ng prompt injection). BĂ i ná»™p lÆ°u `rubric_id`, `rubric_version`, `prompt_checksum` ngay khi báº¯t Ä‘áº§u bÆ°á»›c LLM, vĂ  láº§n thá»­ láº¡i dĂ¹ng Ä‘Ăºng báº£n Ä‘Ă³ | Äá»•i rubric giá»¯a chá»«ng khĂ´ng lĂ m má»™t bĂ i bá»‹ cháº¥m báº±ng hai báº£n; má»™t láº§n nháº­p sai trĂªn giao diá»‡n khĂ´ng phĂ¡ Ä‘Æ°á»£c Ä‘á»‹nh dáº¡ng JSON hay lá»›p báº£o vá»‡. Ănh xáº¡ tiĂªu chĂ­ sang KP lĂ  dá»¯ liá»‡u seed (migration), chÆ°a sá»­a qua trang rubric á»Ÿ v1 |
| D18 | **Lá»‹ch sá»­ há»c táº­p Ä‘i vĂ o cĂ¹ng request cháº¥m, khĂ´ng cĂ³ láº§n gá»i LLM thá»© hai.** Code tĂ³m táº¯t thĂ nh má»™t khá»‘i nhá» gá»“m **KP cá»§a chĂ­nh há»c viĂªn** láº¥y tá»« `kp_mastery_profiles`: chá»‰ cĂ¡c KP náº±m trong danh sĂ¡ch cho phĂ©p (cáº¥u hĂ¬nh: KP Speaking cá»§a phase 8, 4 KP tiĂªu chĂ­ Speaking, vĂ  KP ngá»¯ phĂ¡p, tá»« vá»±ng cá»§a Writing lĂ m tham kháº£o, nhĂ£n "tá»« bĂ i viáº¿t"), tier `PRIORITY` hoáº·c `CONSOLIDATE`, `evidence_count` tá»« ngÆ°á»¡ng cáº¥u hĂ¬nh (Ä‘á» xuáº¥t 3), cĂ³ báº±ng chá»©ng trong 90 ngĂ y, yáº¿u nháº¥t trÆ°á»›c, tá»‘i Ä‘a 5. Má»—i KP gá»“m `code`, `name`, `can_do`, tier, lá»—i láº·p láº¡i, xu hÆ°á»›ng; **khĂ´ng Ä‘Æ°a mastery dáº¡ng sá»‘**; **khĂ´ng Ä‘Æ°a band cÅ©** (trĂ¡nh neo Ä‘iá»ƒm), pháº§n so vá»›i cĂ¡c láº§n trÆ°á»›c do code tá»± viáº¿t. Prompt dáº·n: band chá»‰ dá»±a vĂ o transcript vĂ  sá»‘ Ä‘o, khá»‘i lá»‹ch sá»­ chá»‰ dĂ¹ng cho `personalFeedback`. JSON xuáº¥t theo thá»© tá»± `criteria`, `personalFeedback`; `personalFeedback` sai thĂ¬ bá», giá»¯ band | NgÆ°á»i dĂ¹ng chá»‘t gá»i má»™t láº§n. Rá»§i ro: lá»‹ch sá»­ lĂ m trĂ´i band, vĂ  má»™t request khĂ´ng Ä‘áº£m báº£o Ä‘Æ°á»£c sá»± Ä‘á»™c láº­p. Giáº£m báº±ng A/B (cĂ¹ng báº£n ghi cĂ³ vĂ  khĂ´ng cĂ³ khá»‘i lá»‹ch sá»­, band pháº£i giá»‘ng há»‡t) á»Ÿ phase 1 vĂ  7; náº¿u vá»¡ thĂ¬ tĂ¡ch thĂ nh hai láº§n gá»i |
| D19 | **Cáº­p nháº­t lá»™ trĂ¬nh sau khi cĂ³ nháº­n xĂ©t hoĂ n toĂ n báº±ng code, tá»« band vĂ  sá»‘ Ä‘o, khĂ´ng tá»« chá»¯ tá»± do.** (1) Má»—i tiĂªu chĂ­ FC, LR, GRA, PR ghi má»™t báº±ng chá»©ng vĂ o KP tiĂªu chĂ­ cá»§a nĂ³ (`kp_evidence`, nguá»“n `speaking_practice`), Ä‘Ăºng khi `band >= speaking.pass-band`, rá»“i tĂ­nh láº¡i `kp_mastery_profiles`. (2) "TiĂªu chĂ­ cáº§n luyá»‡n": cĂ¡c tiĂªu chĂ­ dÆ°á»›i `pass-band`, xáº¿p theo má»©c chĂªnh, kĂ¨m liĂªn káº¿t luyá»‡n táº­p Speaking sáºµn cĂ³. (3) Tá»« phĂ¡t Ă¢m yáº¿u láº¥y tháº³ng tá»« Ä‘iá»ƒm Azure theo tá»«ng tá»« Ä‘á»ƒ hiá»ƒn thá»‹. **KhĂ´ng** Ä‘á»•i `topic_progress`, khĂ´ng má»Ÿ hay khĂ³a topic, khĂ´ng sinh bĂ i Ă´n (cĂ¹ng quy táº¯c Writing: Ä‘iá»ƒm Æ°á»›c lÆ°á»£ng khĂ´ng Ä‘Æ°á»£c cháº·n lá»™ trĂ¬nh) | Lá»™ trĂ¬nh do code cáº­p nháº­t tá»« band, khĂ´ng tá»« chá»¯ cá»§a LLM. Há»‡ quáº£: báº±ng chá»©ng ghi sau khi cháº¥m chá»‰ á»Ÿ má»©c 4 tiĂªu chĂ­; KP nhá» nhÆ° `S60_PART2_LONG_TURN` chá»‰ nháº­n báº±ng chá»©ng tá»« bĂ i há»c á»Ÿ phase 8. Muá»‘n LLM cháº¥m cáº£ KP nhá» thĂ¬ thĂªm sau (kiá»ƒu `kpj2` cá»§a Writing) |
| D20 | **Luá»“ng há»c Speaking (phase 8) lĂ m trÆ°á»›c khi báº­t cháº¥m AI.** KĂ­ch hoáº¡t topic `SPEAKING_FRAMEWORK` vĂ  8 KP `S55/S60/S65/S70` trong má»™t course ACTIVE, soáº¡n bĂ i há»c vĂ  bá»™ luyá»‡n táº­p gáº¯n KP; há»c viĂªn há»c thĂ¬ mastery KP Speaking tá»± Ä‘Æ°á»£c ghi nhÆ° cĂ¡c ká»¹ nÄƒng khĂ¡c. Khá»‘i KP cá»§a D18 láº¥y tá»« Ä‘Ă³. Phase 3 Ä‘áº¿n 5 váº«n lĂ m song song Ä‘Æ°á»£c, nhÆ°ng **khĂ´ng báº­t** `learning.speaking.enabled` hay Ä‘Æ°a KP Speaking vĂ o prompt khi phase 8 chÆ°a xong | NgÆ°á»i dĂ¹ng chá»‘t. Dá»¯ liá»‡u kiá»ƒm tra 2026-11-03: 8 KP Speaking INACTIVE, topic chÆ°a thuá»™c course nĂ o, 0 lesson, 0 cĂ¢u há»i `LEARNING` cho Speaking (chá»‰ cĂ³ 56 cĂ¢u loáº¡i `SPEAKING` má»¥c Ä‘Ă­ch `EXAM`/`PLACEMENT`, tá»± cháº¥m khĂ´ng Ä‘Æ°á»£c). BĂ i táº­p tá»± cháº¥m Ä‘Æ°á»£c lĂ  tráº¯c nghiá»‡m vĂ  Ä‘iá»n chá»— trá»‘ng, nĂªn mastery chá»©ng minh kiáº¿n thá»©c vĂ  chiáº¿n lÆ°á»£c, khĂ´ng chá»©ng minh kháº£ nÄƒng nĂ³i |

Chi tiáº¿t Azure Ä‘Ă£ kiá»ƒm chá»©ng trong phiĂªn (tra cá»©u 2026-11-02): cháº¥m phĂ¡t Ă¢m qua REST tá»‘i Ä‘a **30 giĂ¢y** vĂ  cáº§n vÄƒn báº£n
tham chiáº¿u; ngá»¯ Ä‘iá»‡u (prosody) chá»‰ á»Ÿ `en-US` vĂ  tĂ­nh phĂ­ riĂªng; cháº¥m ná»™i dung trong SDK Ä‘Ă£ bá»‹ gá»¡ (2025) nĂªn pháº§n Ä‘Ă³ do LLM
lĂ m; audio dĂ i cáº§n Speech SDK. CĂ¡c Ä‘iá»ƒm cĂ²n chÆ°a xĂ¡c nháº­n (má»‘c thá»i gian tá»«ng tá»« cá»§a STT, má»™t láº§n SDK khĂ´ng ká»‹ch báº£n
cĂ³ Ä‘á»§ tá»‘t khĂ´ng) náº±m trong phase 1.

## Luá»“ng

```
FE  â‘  ghi Ă¢m tá»«ng cĂ¢u, giá»¯ trong trĂ¬nh duyá»‡t
FE  â‘¡ "Tiáº¿p tá»¥c" â–º chá»n AI â–º xĂ¡c nháº­n â–º POST submissions (requestId, scope, parts, consent)
LS  â‘¢ kiá»ƒm tra sá»‘ dÆ° (chá»‰ Ä‘á»c), consent, bĂ i Ä‘ang chá»; thiáº¿u Ä‘iá»ƒm thĂ¬ 402 vĂ  FE má»Ÿ náº¡p key; Ä‘á»§ thĂ¬ táº¡o bĂ i UPLOADING (idempotent theo requestId)
FE  â‘£ PUT tá»«ng ghi Ă¢m vĂ o bĂ i â”€â”€â–º Storage `practice/{userId}/{ngĂ y}_{submissionId}/part{n}_q{nn}.wav`; rá»“i POST complete
LS  â‘¤ Ä‘á»§ ghi Ă¢m thĂ¬ bĂ i QUEUED, tráº£ "Ä‘Ă£ nháº­n"
â”€â”€ tá»« Ä‘Ă¢y cháº¡y ná»n â”€â”€
LS  â‘¤ láº¥y cĂ¢u há»i tá»« content-service; láº¥y file tá»« Storage
LS  â‘¥ Azure: audio â”€â”€â–º transcript + nhá»‹p nĂ³i          (láº·p tá»«ng ghi Ă¢m)
LS  â‘¦ Azure: audio + transcript â”€â”€â–º Ä‘iá»ƒm phĂ¡t Ă¢m       (láº·p tá»«ng ghi Ă¢m)
LS  â‘§ lÆ°u tá»«ng bÆ°á»›c, ghĂ©p theo part
LS  â‘¨ láº¥y bá»™ tiĂªu chĂ­ ACTIVE (content-service), ghĂ©p system prompt, rá»“i LLM: transcript + nhá»‹p nĂ³i + Ä‘iá»ƒm phĂ¡t Ă¢m + tĂ³m táº¯t lá»‹ch sá»­ â”€â”€â–º band FC/LR/GRA + nháº­n xĂ©t (JSON)
LS  â‘© kiá»ƒm tra JSON, PR band tá»« báº£ng Ă¡nh xáº¡, band tá»•ng, trá»« Ä‘iá»ƒm (idempotent), ghi kp_evidence cho 4 KP tiĂªu chĂ­, tĂ­nh láº¡i mastery, dá»±ng danh sĂ¡ch tiĂªu chĂ­ cáº§n luyá»‡n
FE  â‘ª polling tráº¡ng thĂ¡i, hiá»ƒn thá»‹ káº¿t quáº£
```

`LS` lĂ  learning-service. CĂ¡c bÆ°á»›c â‘¤ trá»Ÿ xuá»‘ng Ä‘Ă¡nh sá»‘ láº¡i sau khi thĂªm bÆ°á»›c â‘¢ vĂ  â‘£ (cĂ¡c tham chiáº¿u sá»‘ bÆ°á»›c cÅ© trong phase 3 Ä‘áº¿n 5 hiá»ƒu theo thá»© tá»± ná»™i dung, khĂ´ng theo sá»‘). Tráº¡ng thĂ¡i bĂ i ná»™p: `UPLOADING â†’ QUEUED â†’ ANALYZING â†’ GRADING â†’ PAYMENT_PENDING â†’ GRADED`, vĂ  cĂ¡c nhĂ¡nh
`FAILED_RETRYABLE`, `NEEDS_EXAMINER`, `ABANDONED` (treo quĂ¡ háº¡n).

## Phases

**Code status 2026-11-04:** phases 2, 3, 4, and 8 are complete in the backend; phase 3 includes the 90-day recording retention job. Phases 5 and 5b are in progress: rubric snapshots, background grading, result/payment persistence, basic evidence updates, and a default-off feature flag are implemented. Learner history summaries and the remaining detailed result contract are not implemented. Phase 6 has the upload flow and result polling page; Speaking rubric authoring now supports `system_prompt`. UI polish and verification remain.

| Phase | Ná»™i dung | Effort | Gate |
|---|---|---|---|
| 1 | [Spike vĂ  chá»‘t quyáº¿t Ä‘á»‹nh](./phase-01-spike-v-ch-t-quy-t-nh.md) | 2d | **Pháº£i Ä‘áº¡t trÆ°á»›c khi viáº¿t code repo** |
| 2 | [Content: bá»™ tiĂªu chĂ­ Speaking, KP tiĂªu chĂ­, API ná»™i bá»™](./phase-02-content-rubric-v-kp-speaking.md) | 1d | KhĂ´ng |
| 3 | [Learning: bĂ i ná»™p, upload, kiá»ƒm tra sá»‘ dÆ°](./phase-03-learning-l-u-b-i-n-p-v-upload.md) | 2d | Phase 1 |
| 4 | [Learning: adapter Azure Speech](./phase-04-learning-adapter-azure-speech.md) | 2d | Phase 1 (duyá»‡t SDK) |
| 5 | [Learning: cháº¥m LLM, trá»« Ä‘iá»ƒm, cáº­p nháº­t lá»™ trĂ¬nh](./phase-05-learning-ch-m-llm-v-ghi-mastery.md); Ä‘áº·c táº£ prompt vĂ  káº¿t quáº£: [5b](./phase-05b-llm-prompt-va-ket-qua-chi-tiet.md) | 3d | Phase 2, 3, 4 |
| 6 | [Frontend: luá»“ng ná»™p, káº¿t quáº£ vĂ  trang quáº£n trá»‹ bá»™ tiĂªu chĂ­](./phase-06-frontend-lu-ng-n-p-v-k-t-qu.md) | 3.5d | Há»£p Ä‘á»“ng API cá»§a phase 2, 3 vĂ  5 |
| 7 | [Cháº¿ Ä‘á»™ bĂ³ng, hiá»‡u chá»‰nh, phĂ¡t hĂ nh](./phase-07-ch-b-ng-hi-u-ch-nh-v-ph-t-h-nh.md) | 1.5d + thá»i gian thu dá»¯ liá»‡u | Phase 5, 6, 8 |
| 8 | [Luá»“ng há»c Speaking: kĂ­ch hoáº¡t KP, bĂ i há»c, bá»™ luyá»‡n táº­p](./phase-08-lu-ng-h-c-speaking-kp.md) | 2d ká»¹ thuáº­t + cĂ´ng soáº¡n ná»™i dung (chÆ°a Æ°á»›c lÆ°á»£ng) | **Xong trÆ°á»›c khi báº­t cháº¥m AI (D20)** |

Thá»© tá»±: 1 â†’ (2 vĂ  8 song song vá»›i 3 rá»“i 4) â†’ 5 â†’ 6 â†’ 7; phase 8 pháº£i xong trÆ°á»›c phase 7. Phase 2 náº±m á»Ÿ content-service nĂªn Ä‘á»™c láº­p. Phase 3 vĂ  4 **cĂ¹ng sá»­a**
`services/learning-service/pom.xml` vĂ  `learning-service.yaml`, nĂªn lĂ m tuáº§n tá»± (3 rá»“i 4) hoáº·c phá»‘i há»£p file chung. Phase 6 cĂ³
thá»ƒ báº¯t Ä‘áº§u khi há»£p Ä‘á»“ng JSON cá»§a phase 3 vĂ  5 Ä‘Ă£ chá»‘t. Má»—i phase tá»± viáº¿t test vĂ  pháº£i xanh trÆ°á»›c khi sang phase sau.

## Dependencies

- KhĂ´ng cháº·n vĂ  khĂ´ng bá»‹ plan nĂ o cháº·n. Tham chiáº¿u thiáº¿t káº¿ (cĂ¹ng khuĂ´n, nĂªn xem tráº¡ng thĂ¡i Ä‘Ă£ merge trÆ°á»›c phase 5):
  `261021-0103-writing-criterion-kp-evidence` (KP tiĂªu chĂ­, nguá»“n evidence, quy táº¯c XP) vĂ 
  `261021-2016-writing-criterion-sub-kp-judgments` (má»™t request cháº¥m band vĂ  KP, luáº­t kiá»ƒm tra judgment). Cáº£ hai Ä‘ang
  `in-progress`; plan nĂ y **khĂ´ng sá»­a** file cá»§a chĂºng nhÆ°ng cĂ¹ng cháº¡m `EvidenceSource`, constraint `kp_evidence_source_check`
  vĂ  `MasteryProfileUpdater`, nĂªn cáº§n rebase cáº©n tháº­n.
- `261009-1417-practice-gated-lesson-unlock` (`in_progress`) cĂ¹ng khu vá»±c learning-service nhÆ°ng khĂ¡c tĂ­nh nÄƒng, khĂ´ng xung Ä‘á»™t
  file Ä‘Ă£ biáº¿t.

## Rá»§i ro chĂ­nh

| # | Rá»§i ro | Giáº£m |
|---|---|---|
| R1 | AI lá»‡ch giĂ¡m kháº£o, ngÆ°á»i há»c máº¥t tin | Phase 7: cháº¿ Ä‘á»™ bĂ³ng, ngÆ°á»¡ng QWK vĂ  Ä‘á»™ lá»‡ch ná»­a band, nhĂ£n "Ä‘iá»ƒm Æ°á»›c lÆ°á»£ng" |
| R2 | STT nghe sai giá»ng Viá»‡t, LLM pháº¡t oan tá»« vá»±ng vĂ  ngá»¯ phĂ¡p | Phase 1 Ä‘o; hiá»ƒn thá»‹ transcript; truyá»n Ä‘á»™ tin cáº­y tá»«ng tá»« náº¿u cĂ³ |
| R3 | Audio: giá»›i háº¡n 30 giĂ¢y cá»§a REST, Ä‘á»‹nh dáº¡ng, 5 MB | Phase 1 chá»‘t SDK hoáº·c cáº¯t Ä‘oáº¡n; frontend ghi WAV 16 kHz |
| R4 | KP Speaking hiá»‡n cĂ³ thuá»™c topic `SPEAKING_FRAMEWORK` **INACTIVE**, nĂªn khĂ´ng cĂ³ mastery | Phase 2 táº¡o KP tiĂªu chĂ­ trong topic ACTIVE nhÆ° `DEMO_WRITING_CRITERION_*` |
| R5 | Chi phĂ­ Azure (audio xá»­ lĂ½ hai láº§n) vĂ  LLM, há»c viĂªn láº¡m dá»¥ng | Quota ngĂ y (`speaking_grading`), má»™t bĂ i Ä‘ang chá», cáº£nh bĂ¡o ngĂ¢n sĂ¡ch Azure |
| R6 | Dá»¯ liá»‡u giá»ng nĂ³i lĂ  dá»¯ liá»‡u cĂ¡ nhĂ¢n gá»­i ra ngoĂ i | `consent`, region, thá»i háº¡n lÆ°u, phĂ¡p lĂ½ duyá»‡t á»Ÿ phase 1 |
| R7 | Prompt injection qua transcript; transcript chá»©a ná»™i dung bá»‹ lá»c | Transcript Ä‘áº·t trong khá»‘i dá»¯ liá»‡u cĂ³ phĂ¢n cĂ¡ch, kiá»ƒm tra JSON cháº·t, lá»—i lá»c ná»™i dung chuyá»ƒn giĂ¡m kháº£o |
| R8 | Háº¿t Ä‘iá»ƒm giá»¯a chá»«ng, treo job, ná»™p trĂ¹ng | `PAYMENT_PENDING`, phĂ¡t hiá»‡n treo, `requestId` idempotent |
| R9 | Äá»•i hoáº·c ngá»«ng API Azure/LLM | Ghim phiĂªn báº£n; lÆ°u `promptVersion`, `rubric_id`, `rubric_version`, `prompt_checksum`, `pronunciationMapVersion` cĂ¹ng káº¿t quáº£ |

## CĂ¢u há»i cĂ²n má»Ÿ (cáº§n ngÆ°á»i dĂ¹ng tráº£ lá»i)

1. ~~Bá»™ tiĂªu chĂ­ riĂªng á»Ÿ dáº¡ng nĂ o?~~ **ÄĂ£ chá»‘t (D16, D17):** nháº­p vĂ  sá»­a qua trang quáº£n trá»‹ rubric; learning láº¥y báº£n ACTIVE lĂºc cháº¥m. CĂ²n láº¡i: cáº§n ná»™i dung rubric Speaking tháº­t (FC, LR, GRA kĂ¨m mĂ´ táº£ theo band, vĂ  PR Ä‘á»ƒ hiá»ƒn thá»‹) trÆ°á»›c khi báº­t á»Ÿ phase 7.
2. **LLM cá»¥ thá»ƒ** lĂ  gĂ¬ vĂ  cĂ³ há»— trá»£ `response_format` kiá»ƒu `json_schema` khĂ´ng? `LlmClient.completeJson` hiá»‡n chá»‰ xin JSON object; plan máº·c Ä‘á»‹nh giá»¯ nguyĂªn vĂ  kiá»ƒm tra cháº·t phĂ­a server, chá»‰ thĂªm schema náº¿u nhĂ  cung cáº¥p há»— trá»£.
3. **Duyá»‡t Azure Speech SDK cho Java** (AGENTS.md má»¥c 2), hay cáº¯t audio dÆ°á»›i 30 giĂ¢y vĂ  dĂ¹ng REST?
4. **GiĂ¡ Ä‘iá»ƒm má»™t bĂ i Speaking** (cáº£ Ä‘á» vĂ  part láº») vĂ  háº¡n má»©c ngĂ y.
5. **ThĂ´ng bĂ¡o khi cháº¥m xong:** v1 chá»‰ polling trong app, hay cáº§n thĂ´ng bĂ¡o Ä‘áº©y qua notification-service?
6. **Bucket riĂªng cá»§a learning-service (D12)** hay dĂ¹ng chung `speaking-recordings` cá»§a assessment-service?
7. **Ghi Ă¢m cĂ³ cáº§n sá»‘ng sĂ³t qua F5** (IndexedDB) ngay v1 khĂ´ng?
8. **PhĂ¡p lĂ½:** vÄƒn báº£n Ä‘á»“ng Ă½ vĂ  region Azure.

## BĂ¡o cĂ¡o

KhĂ´ng cĂ³ bĂ¡o cĂ¡o nghiĂªn cá»©u riĂªng: káº¿t quáº£ tra Azure náº±m trong phiĂªn lĂ m viá»‡c vĂ  Ä‘Æ°á»£c tĂ³m táº¯t á»Ÿ "Quyáº¿t Ä‘á»‹nh Ä‘Ă£ chá»‘t".

