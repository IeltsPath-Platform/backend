package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.application.service.SpeakingResultValidator.InvalidSpeakingGradeException;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeakingGraderTest {
    private static final String LONG_TEXT = String.join(" ", java.util.Collections.nCopies(3, SpeakingTestData.SENTENCE));
    private final SpeakingGradingInput input = SpeakingTestData.input(SpeakingScope.PART, new int[]{1},
            SpeakingTestData.analysis(LONG_TEXT, 85, 0.95));

    @Test
    void anUnusableFirstAnswerIsAskedForOnceMoreWithAReminder() {
        Scripted llm = new Scripted("not json", SpeakingTestData.goodAnswer("6.0", "6.0", "6.0"));

        var assembled = grader(llm).grade(prepared(llm), input);

        assertThat(llm.systemPrompts).hasSize(2);
        assertThat(llm.systemPrompts.get(1)).contains("Your previous answer could not be used");
        assertThat(assembled.result()).containsKeys("display", "signals");
    }

    @Test
    void aMissingBandCountsAsUnusableNotAsZero() {
        String noBand = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replace("\"code\":\"FC\",\"band\":6.0,", "\"code\":\"FC\",");
        Scripted llm = new Scripted(noBand, SpeakingTestData.goodAnswer("6.0", "6.0", "6.0"));

        grader(llm).grade(prepared(llm), input);

        assertThat(llm.systemPrompts).hasSize(2);
    }

    @Test
    void twoUnusableAnswersEndInAnInvalidGradeSoNobodyIsCharged() {
        Scripted llm = new Scripted("nope", "still nope");

        assertThatThrownBy(() -> grader(llm).grade(prepared(llm), input)).isInstanceOf(InvalidSpeakingGradeException.class);
        assertThat(llm.systemPrompts).hasSize(2);
    }

    @Test
    void anUnreachableModelIsNotRetriedHere() {
        LlmClient down = new LlmClient() {
            public boolean configured() { return true; }
            public String completeJson(String system, String user) { throw new LlmUnavailableException(503); }
        };

        assertThatThrownBy(() -> grader(down).grade(prepared(down), input)).isInstanceOf(LlmUnavailableException.class);
    }

    private SpeakingGrader grader(LlmClient llm) {
        return new SpeakingGrader(llm, new SpeakingPromptAssembler(), new SpeakingResultValidator(new ObjectMapper()),
                new SpeakingResultAssembler(SpeakingSettingsFixture.enabled()));
    }

    private SpeakingPromptAssembler.Prepared prepared(LlmClient llm) {
        return grader(llm).prepare(SpeakingTestData.rubric(), input);
    }

    private static final class Scripted implements LlmClient {
        private final List<String> answers;
        final List<String> systemPrompts = new ArrayList<>();

        Scripted(String... answers) { this.answers = List.of(answers); }

        public boolean configured() { return true; }

        public String completeJson(String system, String user) {
            systemPrompts.add(system);
            return answers.get(Math.min(systemPrompts.size(), answers.size()) - 1);
        }
    }
}
