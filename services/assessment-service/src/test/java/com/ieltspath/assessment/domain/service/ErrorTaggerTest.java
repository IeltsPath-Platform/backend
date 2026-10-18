package com.ieltspath.assessment.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class ErrorTaggerTest {
    private final ErrorTagger tagger = new ErrorTagger();

    @TestFactory
    Stream<DynamicTest> tagsEverySharedErrorTagVector() throws IOException {
        Path vectorsPath = Stream.of(Path.of("docs/contracts/answer-spec-error-tags-v1-vectors.json"),
                        Path.of("../../docs/contracts/answer-spec-error-tags-v1-vectors.json"))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IOException("Shared error-tag vectors were not found."));
        List<Map<String, Object>> vectors = new ObjectMapper().readValue(vectorsPath.toFile(), new TypeReference<>() {});
        return vectors.stream().map(vector -> dynamicTest((String) vector.get("name"), () -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> spec = (Map<String, Object>) vector.get("spec");
            String tag = tagger.tag(spec, vector.get("response"), (Boolean) vector.get("correct"));

            assertThat(tag).isEqualTo(vector.get("expectedErrorCode"));
        }));
    }
}
