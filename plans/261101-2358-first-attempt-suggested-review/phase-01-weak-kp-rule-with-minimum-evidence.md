---
phase: 1
title: "Weak KP rule with minimum evidence"
status: pending
priority: P1
effort: "1h"
dependencies: []
---

# Phase 1: Weak KP rule with minimum evidence

Work package A. Repo: backend. Paths are under `services/learning-service/src/`.

## Overview

A knowledge point (KP) counts as weak only with at least 2 wrong answers in the set and under 70%.

## Tests first

Add to `test/java/com/ieltspath/learning/domain/service/PracticeReviewRuleTest.java` (exists; keep its current test,
which still passes because its weak KP has 2 wrong of 3):

```java
    private static List<PracticeReviewRule.ItemOutcome> outcomes(UUID kp, boolean... correct) {
        List<PracticeReviewRule.ItemOutcome> items = new ArrayList<>();
        for (boolean value : correct) items.add(new PracticeReviewRule.ItemOutcome(Set.of(kp), value));
        return items;
    }

    @Test
    void aPointNeedsTwoWrongAnswersAndUnderSeventyPercent() {
        var rule = new PracticeReviewRule();
        UUID kp = UUID.randomUUID();
        Map<UUID, Integer> spare = Map.of(kp, 1);
        assertTrue(rule.derive(0.0, true, outcomes(kp, false), Set.of(), spare).isEmpty());          // 0/1
        assertTrue(rule.derive(0.5, true, outcomes(kp, true, false), Set.of(), spare).isEmpty());    // 1/2
        assertTrue(rule.derive(0.66, true, outcomes(kp, true, true, false), Set.of(), spare).isEmpty()); // 2/3
        assertEquals(2, rule.derive(0.0, true, outcomes(kp, false, false), Set.of(), spare).getFirst().wrong()); // 0/2
        assertEquals(2, rule.derive(0.33, true, outcomes(kp, true, false, false), Set.of(), spare).getFirst().wrong()); // 1/3
        assertEquals(3, rule.derive(0.4, true, outcomes(kp, true, true, false, false, false), Set.of(), spare)
                .getFirst().wrong());                                                                 // 2/5
    }

    @Test
    void seventyPercentOnAPointIsNotWeakEvenWithManyWrongAnswers() {
        var rule = new PracticeReviewRule();
        UUID kp = UUID.randomUUID();
        boolean[] sevenOfTen = {true, true, true, true, true, true, true, false, false, false};
        assertTrue(rule.derive(0.5, true, outcomes(kp, sevenOfTen), Set.of(), Map.of(kp, 1)).isEmpty());
    }
```

Imports needed: `java.util.ArrayList`.

## Implementation

`main/java/com/ieltspath/learning/domain/service/PracticeReviewRule.java` becomes:

```java
public final class PracticeReviewRule {
    /** A knowledge point needs this many wrong answers in one set before it counts as weak; one slip is not enough. */
    public static final int MIN_WRONG_ANSWERS = 2;
    private static final double PASS = 0.70;

    public List<Need> derive(double percent, boolean countedAsEvidence, List<ItemOutcome> items,
                             Set<UUID> pendingKps, Map<UUID, Integer> availablePackages) {
        if (!countedAsEvidence || percent >= PASS) return List.of();
        Map<UUID, Counts> counts = new HashMap<>();
        for (ItemOutcome item : items) {
            for (UUID kp : item.knowledgePointIds()) {
                Counts old = counts.getOrDefault(kp, new Counts(0, 0));
                counts.put(kp, new Counts(old.correct() + (item.correct() ? 1 : 0), old.total() + 1));
            }
        }
        return counts.entrySet().stream()
                .filter(entry -> !pendingKps.contains(entry.getKey()))
                .filter(entry -> availablePackages.getOrDefault(entry.getKey(), 0) > 0)
                .filter(entry -> entry.getValue().wrong() >= MIN_WRONG_ANSWERS)
                .map(entry -> new Need(entry.getKey(),
                        (double) entry.getValue().correct() / entry.getValue().total(), entry.getValue().wrong()))
                .filter(need -> need.kpPercent() < PASS)
                .sorted(Comparator.comparing(need -> need.knowledgePointId().toString()))
                .toList();
    }

    public record ItemOutcome(Set<UUID> knowledgePointIds, boolean correct) {}
    public record Need(UUID knowledgePointId, double kpPercent, int wrong) {}
    private record Counts(int correct, int total) {
        int wrong() { return total - correct; }
    }
}
```

`derive` has two call sites, both in `SubmitPracticeAttemptUseCase.reviewNeeds` (lines ~273 and ~279); they only read
`knowledgePointId()` and `kpPercent()`, so they compile unchanged.

## Verify

`mvn -q -pl services/learning-service -am test -Dtest=PracticeReviewRuleTest -Dsurefire.failIfNoSpecifiedTests=false`

## Success Criteria

- [ ] Both new tests and the existing one pass.
