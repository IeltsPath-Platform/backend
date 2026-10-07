package com.ieltspath.learning.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class AnswerSpecGraderTest {
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    @TestFactory
    Stream<DynamicTest> gradesEverySharedContractVector() throws IOException {
        Path vectorsPath = Stream.of(Path.of("docs/contracts/answer-spec-v1-vectors.json"),
                        Path.of("../../docs/contracts/answer-spec-v1-vectors.json"))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IOException("Shared answer-spec vectors were not found."));
        List<Map<String, Object>> vectors = new ObjectMapper().readValue(vectorsPath.toFile(), new TypeReference<>() {});
        return vectors.stream().map(vector -> dynamicTest((String) vector.get("name"), () -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> spec = (Map<String, Object>) vector.get("spec");
            AnswerSpecGrader.Grade result = grader.grade(spec, vector.get("response"));

            assertThat(result.gradable()).isEqualTo(vector.getOrDefault("gradable", true));
            assertThat(result.correct()).isEqualTo(vector.get("correct"));
        }));
    }

    @Test
    void exposesCanonicalCorrectAnswerForSolutions() {
        assertThat(grader.grade(Map.of("type", "CHOICE", "correct", "ii"), "II").correctAnswer()).isEqualTo("ii");
        assertThat(grader.grade(Map.of("type", "FILL", "accepted", List.of("15", "fifteenth")), "FIFTEENTH"))
                .isEqualTo(new AnswerSpecGrader.Grade(true, true, "15"));
    }

    @Test
    void normalizesOnlyWhitespaceAndCaseForFill() {
        Map<String, Object> spec = Map.of("type", "FILL", "accepted", List.of("the fifteenth"));

        assertThat(grader.grade(spec, "\t The\u00a0  FIFTEENTH\n").correct()).isTrue();
        assertThat(grader.grade(spec, "the fifteenth.").correct()).isFalse();
        assertThat(grader.grade(spec, "the fifteenths").correct()).isFalse();
    }

    @Test
    void invalidOrUnsupportedSpecsAreUngradable() {
        List<Map<String, Object>> specs = List.of(
                Map.of(), Map.of("type", "ESSAY"), Map.of("type", 1, "correct", "A"),
                Map.of("type", "CHOICE"), Map.of("correct", 1), Map.of("correct", " "),
                Map.of("type", "FILL"), Map.of("type", "FILL", "accepted", List.of()),
                Map.of("type", "FILL", "accepted", "critics"),
                Map.of("type", "FILL", "accepted", List.of("critics", 1)),
                Map.of("type", "FILL", "accepted", List.of(" ")));
        for (Map<String, Object> spec : specs) {
            assertThat(grader.grade(spec, "A")).isEqualTo(new AnswerSpecGrader.Grade(false, false, null));
        }
        assertThat(grader.grade(null, null)).isEqualTo(new AnswerSpecGrader.Grade(false, false, null));
        Map<String, Object> nullType = new HashMap<>();
        nullType.put("type", null);
        nullType.put("correct", "A");
        assertThat(grader.grade(nullType, "A").gradable()).isFalse();
    }

    @Test
    void rejectsInvalidResponseTypesInsteadOfCoercingOrMatchingThem() {
        Map<String, Object> spec = Map.of("correct", "A");
        for (Object response : List.of(1, true, List.of("A"), Map.of("answer", 1),
                Map.of("answer", List.of("A")), Map.of("answer", Map.of("answer", "A")))) {
            assertThatThrownBy(() -> grader.grade(spec, response))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer must be a string or null.");
        }
        assertThatThrownBy(() -> grader.grade(Map.of(), false)).isInstanceOf(IllegalArgumentException.class);
    }
}
