package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.application.service.SpeakingResultValidator.InvalidSpeakingGradeException;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeakingGraderTest {
    private static final String LONG_TEXT = String.join(" ", Collections.nCopies(3, SpeakingTestData.SENTENCE));
    private static final ObjectMapper JSON = new ObjectMapper();
    private final SpeakingGradingInput input = SpeakingTestData.input(SpeakingScope.PART, new int[]{1},
            SpeakingTestData.analysis(LONG_TEXT, 85, 0.95));

    private static String criterion(String code, String band, String errors) {
        return "{\"criteria\":[{\"code\":\"" + code + "\"" + (band == null ? "" : ",\"band\":" + band)
                + ",\"comment\":\"ok\",\"strengths\":[\"a\"],\"weaknesses\":[\"b\"],"
                + "\"evidence\":[{\"quote\":\"my teacher was very kind\",\"note\":\"clear\"}]}],\"errors\":[" + errors + "]}";
    }

    private static final String ERROR = "{\"code\":\"SPK_NO_LINKER\",\"partNo\":1,\"excerpt\":\"she taught me English\",\"suggestion\":\"add because\"}";
    private static final String OVERVIEW = "{\"partNotes\":[{\"partNo\":1,\"note\":\"n\"}],"
            + "\"flags\":{\"onTopic\":true,\"englishOnly\":true,\"tooShort\":false},"
            + "\"transcriptReliability\":{\"level\":\"HIGH\",\"reason\":\"clean\"},\"kpObservations\":[]}";
    private static final String SYNTHESIS = "{\"summary\":\"Good overall.\",\"focus\":[\"Use linkers\"]}";

    private static Scripted good() {
        return new Scripted()
                .answer("FC", criterion("FC", "6.0", ERROR)).answer("LR", criterion("LR", "6.5", ""))
                .answer("GRA", criterion("GRA", "5.5", "")).answer("OVERVIEW", OVERVIEW).answer("SYNTHESIS", SYNTHESIS);
    }

    @Test
    void aGradeIsFiveShortCallsAndTheFirstFourAreAskedAtTheSameTime() {
        Scripted llm = good().expectConcurrentFirstRound();

        var assembled = grader(llm).grade(prepared(llm), input);

        assertThat(llm.calls).containsExactlyInAnyOrder("FC", "LR", "GRA", "OVERVIEW", "SYNTHESIS");
        assertThat(llm.calls.get(4)).isEqualTo("SYNTHESIS");
        assertThat(llm.firstRoundWasConcurrent).isTrue();
        assertThat(assembled.result()).containsKeys("display", "signals");
    }

    @Test
    void theLastCallIsToldTheBandsTheOthersDecidedAndNothingElseChangesThem() {
        Scripted llm = good();

        grader(llm).grade(prepared(llm), input);

        String user = llm.users.get("SYNTHESIS");
        assertThat(user).contains("<graded>").contains("FC 6: ok").contains("LR 6.5: ok").contains("GRA 5.5: ok")
                .contains("  - b");
        assertThat(llm.users.get("FC")).doesNotContain("<graded>");
    }

    @Test
    void anUnusableCriterionAnswerIsAskedForOnceMoreWithAReminderAndTheOtherCallsAreNotRepeated() {
        Scripted llm = good().answerFirst("FC", "not json");

        grader(llm).grade(prepared(llm), input);

        assertThat(llm.calls).filteredOn("FC"::equals).hasSize(2);
        assertThat(llm.calls).filteredOn("LR"::equals).hasSize(1);
        assertThat(llm.systems.get("FC").get(1)).contains("Your previous answer could not be used");
        assertThat(llm.systems.get("FC").get(0)).doesNotContain("Your previous answer could not be used");
    }

    @Test
    void aMissingBandCountsAsUnusableNotAsZero() {
        Scripted llm = good().answerFirst("GRA", criterion("GRA", null, ""));

        grader(llm).grade(prepared(llm), input);

        assertThat(llm.calls).filteredOn("GRA"::equals).hasSize(2);
    }

    @Test
    void aCriterionCallThatAnswersForAnotherCriterionIsUnusable() {
        Scripted llm = good().answerFirst("LR", criterion("FC", "6.0", ""));

        grader(llm).grade(prepared(llm), input);

        assertThat(llm.calls).filteredOn("LR"::equals).hasSize(2);
    }

    @Test
    void twoUnusableAnswersToOneCallEndInAnInvalidGradeSoNobodyIsCharged() {
        Scripted llm = good().answerFirst("FC", "nope").answerFirst("FC", "still nope");

        assertThatThrownBy(() -> grader(llm).grade(prepared(llm), input)).isInstanceOf(InvalidSpeakingGradeException.class);
        assertThat(llm.calls).doesNotContain("SYNTHESIS");
    }

    @Test
    void anUnreachableModelIsNotRetriedHere() {
        LlmClient down = new LlmClient() {
            public boolean configured() { return true; }
            public String completeJson(String system, String user) { throw new LlmUnavailableException(503); }
        };

        assertThatThrownBy(() -> grader(down).grade(prepared(down), input)).isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    void everyCallGetsTheBudgetOfOneShortAnswer() {
        Scripted llm = good();

        grader(llm).grade(prepared(llm), input);

        assertThat(llm.budgets).hasSize(5).containsOnly(SpeakingGrader.BUDGET);
        assertThat(SpeakingGrader.BUDGET.maxTokens()).isGreaterThan(4096);
        // A stuck call is asked twice, in two rounds, and still ends before another worker may take the submission over.
        assertThat(SpeakingGrader.BUDGET.timeoutSeconds() * 4).isLessThanOrEqualTo(300);
    }

    @Test
    void theErrorsOfTheCriteriaAreTakenInTurnAndTheSameFindingIsKeptOnce() throws Exception {
        JsonNode fc = JSON.readTree("[" + ERROR + ",{\"code\":\"SPK_A\",\"excerpt\":\"fc2\"},{\"code\":\"SPK_A\",\"excerpt\":\"fc3\"}]");
        JsonNode lr = JSON.readTree("[{\"code\":\"SPK_B\",\"excerpt\":\"lr1\"}]");
        JsonNode gra = JSON.readTree("[" + ERROR + ",{\"code\":\"SPK_C\",\"excerpt\":\"gra2\"}]");

        var merged = grader(good()).interleave(List.of(fc, lr, gra));

        assertThat(merged).extracting(node -> node.path("excerpt").asText())
                .containsExactly("she taught me English", "lr1", "fc2", "gra2", "fc3");
    }

    private SpeakingGrader grader(LlmClient llm) {
        return new SpeakingGrader(llm, new SpeakingPromptAssembler(), new SpeakingResultValidator(JSON),
                new SpeakingResultAssembler(SpeakingSettingsFixture.enabled()), JSON);
    }

    private SpeakingPromptAssembler.Prepared prepared(LlmClient llm) {
        return grader(llm).prepare(SpeakingTestData.rubric(), input);
    }

    /** Answers by which call is asked, told apart by the first lines of its system prompt. */
    private static final class Scripted implements LlmClient {
        private final Map<String, Deque<String>> first = new HashMap<>();
        private final Map<String, String> usual = new HashMap<>();
        final List<String> calls = Collections.synchronizedList(new ArrayList<>());
        final Map<String, List<String>> systems = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, String> users = new java.util.concurrent.ConcurrentHashMap<>();
        final List<Budget> budgets = Collections.synchronizedList(new ArrayList<>());
        private CountDownLatch together;
        volatile boolean firstRoundWasConcurrent;

        Scripted answer(String task, String json) { usual.put(task, json); return this; }

        /** Answers given in order before the usual one. */
        Scripted answerFirst(String task, String json) {
            first.computeIfAbsent(task, key -> new ArrayDeque<>()).add(json);
            return this;
        }

        /** Each of the first four calls waits for the others, which only happens when they are asked together. */
        Scripted expectConcurrentFirstRound() { together = new CountDownLatch(4); return this; }

        public boolean configured() { return true; }

        public String completeJson(String system, String user) { throw new AssertionError("a budget is always given"); }

        @Override
        public String completeJson(String system, String user, Budget budget) {
            String task = task(system);
            budgets.add(budget);
            systems.computeIfAbsent(task, key -> Collections.synchronizedList(new ArrayList<>())).add(system);
            users.put(task, user);
            if (together != null && !task.equals("SYNTHESIS") && together.getCount() > 0) {
                together.countDown();
                try {
                    firstRoundWasConcurrent = together.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            synchronized (calls) {
                calls.add(task);
                Deque<String> queued = first.get(task);
                return queued != null && !queued.isEmpty() ? queued.poll() : usual.get(task);
            }
        }

        private static String task(String system) {
            for (String code : List.of("FC", "LR", "GRA")) {
                if (system.contains("You are grading one criterion of an IELTS Speaking practice test: " + code)) return code;
            }
            return system.contains("part-by-part notes") ? "OVERVIEW" : "SYNTHESIS";
        }
    }
}
