---
phase: 4
title: "Learning adapter Azure Speech"
status: completed
priority: P2
effort: "2d"
dependencies: [1]
---

# Phase 4: Learning adapter Azure Speech

## Overview

Port `SpeechAnalysisClient` vĂ  adapter Azure: chuyá»ƒn giá»ng nĂ³i thĂ nh chá»¯, cháº¥m phĂ¡t Ă¢m, cá»™ng bá»™ tĂ­nh nhá»‹p nĂ³i vĂ  báº£ng Ă¡nh xáº¡ Ä‘iá»ƒm phĂ¡t Ă¢m sang band. Táº¥t cáº£ logic tĂ­nh toĂ¡n lĂ  mĂ£ thuáº§n, test khĂ´ng cáº§n Azure.

## Requirements

- Functional: tá»« má»™t file WAV ra transcript, má»‘c thá»i gian tá»«ng tá»« (náº¿u cĂ³), Ä‘iá»ƒm phĂ¡t Ă¢m tá»•ng vĂ  tá»«ng tá»«; tĂ­nh sá»‘ tá»« má»—i phĂºt, thá»i lÆ°á»£ng nĂ³i, sá»‘ quĂ£ng ngáº¯t dĂ i; Ă¡nh xáº¡ ra PR band cĂ³ phiĂªn báº£n.
- Non-functional: khĂ´ng log audio, transcript, khĂ³a; dĂ¹ng `ResilientCall` (`common-resilience` Ä‘Ă£ lĂ  phá»¥ thuá»™c cá»§a learning-service); lá»—i táº¡m thá»i nĂ©m `SpeechUnavailableException` Ä‘á»ƒ job thá»­ láº¡i; adapter áº©n sau port nĂªn Ä‘á»•i cĂ¡ch gá»i (SDK hay REST) khĂ´ng cháº¡m logic khĂ¡c.

## Architecture

```
application/port/SpeechAnalysisClient   configured(), transcribe(audio), assess(audio, referenceText)   // hoáº·c analyze() náº¿u cĂ¡ch A
domain/vo/                              WordTiming, SpeechTranscript, PronunciationResult (nullable prosody)
domain/service/RhythmMetrics            thuáº§n: tá»« má»‘c tá»«ng tá»« ra wpm, thá»i lÆ°á»£ng, quĂ£ng ngáº¯t
domain/service/PronunciationBandMapper  thuáº§n: Ä‘iá»ƒm Azure ra band theo báº£ng cĂ³ phiĂªn báº£n
infrastructure/client/AzureSpeechClient adapter (phá»¥ thuá»™c káº¿t quáº£ phase 1)
```

- CĂ¡ch gá»i theo quyáº¿t Ä‘á»‹nh cá»§a phase 1: SDK Java (cáº§n duyá»‡t theo AGENTS.md; bá»c SDK sau má»™t lá»›p má»ng Ä‘á»ƒ test khĂ´ng cáº§n thÆ° viá»‡n native), hoáº·c REST náº¿u giá»¯ audio dÆ°á»›i 30 giĂ¢y cho cháº¥m phĂ¡t Ă¢m.
- Audio pháº£i lĂ  WAV PCM 16 kHz mono (D11): kiá»ƒm tra header vĂ  tá»« chá»‘i Ä‘á»‹nh dáº¡ng khĂ¡c báº±ng thĂ´ng bĂ¡o rĂµ rĂ ng, khĂ´ng chuyá»ƒn Ä‘á»•i á»Ÿ server.
- Äiá»ƒm tá»•ng há»£p phĂ¡t Ă¢m: káº¿t há»£p accuracy, fluency, prosody (chá»‰ cĂ³ khi locale `en-US`; thiáº¿u thĂ¬ chia láº¡i trá»ng sá»‘). Báº£ng ngÆ°á»¡ng sang band Ä‘áº·t trong cáº¥u hĂ¬nh, cĂ³ `version`, **ban Ä‘áº§u lĂ  báº£n táº¡m** cho Ä‘áº¿n khi hiá»‡u chá»‰nh á»Ÿ phase 7.
- NgÆ°á»¡ng quĂ£ng ngáº¯t (giĂ¢y) lĂ  cáº¥u hĂ¬nh vá»›i giĂ¡ trá»‹ máº·c Ä‘á»‹nh táº¡m, chÆ°a cĂ³ cÆ¡ sá»Ÿ thá»±c nghiá»‡m.
- Náº¿u STT khĂ´ng tráº£ má»‘c thá»i gian tá»«ng tá»«, láº¥y tá»« káº¿t quáº£ cháº¥m phĂ¡t Ă¢m (Azure tráº£ offset vĂ  Ä‘á»™ dĂ i tá»«ng tá»«); phase 1 xĂ¡c nháº­n.

## Related Code Files

- Create: `application/port/SpeechAnalysisClient.java`, `application/exception/SpeechUnavailableException.java`
- Create: `domain/vo/WordTiming.java`, `SpeechTranscript.java`, `PronunciationResult.java`
- Create: `domain/service/RhythmMetrics.java`, `PronunciationBandMapper.java`
- Create: `infrastructure/client/AzureSpeechClient.java` (+ lá»›p bá»c SDK náº¿u duyá»‡t)
- Modify: `services/learning-service/pom.xml` (SDK náº¿u duyá»‡t), `infra/config-server/config-repo/learning-service.yaml` (khá»‘i `learning.speech.*`: key, region, locale, timeout, báº£ng Ă¡nh xáº¡, ngÆ°á»¡ng nhá»‹p nĂ³i), `AGENTS.md` (báº£ng cĂ´ng nghá»‡: Azure AI Speech, SDK), `docs/system-architecture.md`

## Implementation Steps

1. Test thuáº§n trÆ°á»›c: `RhythmMetrics` (quĂ£ng ngáº¯t, wpm, cĂ¢u ngáº¯n, im láº·ng hoĂ n toĂ n) vĂ  `PronunciationBandMapper` (biĂªn cá»§a tá»«ng ngÆ°á»¡ng, thiáº¿u prosody), rá»“i code.
2. Äá»‹nh nghÄ©a port vĂ  value object theo káº¿t quáº£ phase 1 (cĂ¡ch A hay B).
3. Viáº¿t adapter; test báº±ng lá»›p bá»c giáº£ hoáº·c server HTTP giáº£, khĂ´ng gá»i Azure tháº­t trong CI; kiá»ƒm tra Ă¡nh xáº¡ lá»—i nhĂ  cung cáº¥p sang `SpeechUnavailableException`.
4. Cáº¥u hĂ¬nh báº±ng biáº¿n mĂ´i trÆ°á»ng; cáº­p nháº­t AGENTS.md vĂ  tĂ i liá»‡u kiáº¿n trĂºc (chá»‰ sau khi ngÆ°á»i dĂ¹ng duyá»‡t SDK).

## Success Criteria

- [ ] Port khĂ´ng Ä‘á»ƒ lá»™ kiá»ƒu cá»§a Azure ra ngoĂ i infrastructure.
- [ ] `RhythmMetrics` vĂ  `PronunciationBandMapper` cĂ³ test biĂªn vĂ  xanh.
- [ ] Adapter test khĂ´ng cáº§n máº¡ng, khĂ´ng cáº§n khĂ³a.
- [ ] KhĂ³a chá»‰ Ä‘á»c tá»« biáº¿n mĂ´i trÆ°á»ng; log khĂ´ng chá»©a audio, transcript hay khĂ³a (test kháº³ng Ä‘á»‹nh báº±ng cĂ¡ch báº¯t log).
- [ ] AGENTS.md vĂ  tĂ i liá»‡u kiáº¿n trĂºc Ä‘Æ°á»£c cáº­p nháº­t Ä‘Ăºng vá»›i quyáº¿t Ä‘á»‹nh Ä‘Ă£ duyá»‡t.

## Risk Assessment

- SDK chÆ°a Ä‘Æ°á»£c duyá»‡t: dĂ¹ng REST vĂ  giá»›i háº¡n cháº¥m phĂ¡t Ă¢m á»Ÿ Ä‘oáº¡n dÆ°á»›i 30 giĂ¢y (Part 2 pháº£i cáº¯t Ä‘oáº¡n, Ä‘iá»ƒm á»Ÿ chá»— cáº¯t kĂ©m chĂ­nh xĂ¡c); ghi rĂµ háº¡n cháº¿ trong há»£p Ä‘á»“ng.
- SDK cháº¡y native trong Docker: kiá»ƒm tra image Temurin cĂ³ thÆ° viá»‡n cáº§n thiáº¿t trÆ°á»›c khi merge.
- Azure Ä‘á»•i API hoáº·c model: ghim phiĂªn báº£n, lÆ°u phiĂªn báº£n nhĂ  cung cáº¥p cĂ¹ng káº¿t quáº£.

