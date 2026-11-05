package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.SpeechUnavailableException;
import com.ieltspath.learning.domain.service.LearnerIpa;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpeechSegment;
import com.ieltspath.learning.domain.vo.SpokenPhoneme;
import com.ieltspath.learning.domain.vo.SpokenWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the detailed JSON Azure Speech returns for each stretch of speech and turns it into {@link SpeechAnalysis}. This
 * is the one place that knows the shape of that JSON. Times in it are in units of 100 nanoseconds.
 */
public final class AzureSpeechResultParser {
    private static final long UNITS_PER_MILLISECOND = 10_000;

    private final ObjectMapper json;

    public AzureSpeechResultParser(ObjectMapper json) {
        this.json = json;
    }

    /** {@code results} is a JSON array of per-segment results, or one result; segments that were not speech are skipped. */
    public SpeechAnalysis parse(String results) {
        JsonNode root;
        try {
            root = json.readTree(results);
        } catch (Exception exception) {
            throw new SpeechUnavailableException("SPEECH_RESULT_UNREADABLE", false);
        }
        List<SpeechSegment> segments = new ArrayList<>();
        if (root != null && root.isArray()) {
            root.forEach(node -> segment(node).ifPresent(segments::add));
        } else if (root != null) {
            segment(root).ifPresent(segments::add);
        }
        return new SpeechAnalysis(segments);
    }

    /**
     * The sounds of a word. {@code NBestPhonemes} lists the sounds the service thinks it heard, best first; when the best
     * of them is not the expected sound, that is what the learner said instead. A word scored only at word level has no
     * {@code Phonemes}, which gives an empty list.
     */
    private static List<SpokenPhoneme> phonemes(JsonNode node) {
        List<SpokenPhoneme> sounds = new ArrayList<>();
        for (JsonNode phoneme : node) {
            String symbol = LearnerIpa.of(phoneme.path("Phoneme").asText(""));
            if (symbol.isBlank()) continue;
            JsonNode assessment = phoneme.path("PronunciationAssessment");
            sounds.add(new SpokenPhoneme(symbol, assessment.path("AccuracyScore").asDouble(), heardInstead(symbol, assessment.path("NBestPhonemes"))));
        }
        return sounds;
    }

    private static String heardInstead(String expected, JsonNode candidates) {
        String best = null;
        double bestScore = -1;
        for (JsonNode candidate : candidates) {
            double score = candidate.path("Score").asDouble(-1);
            if (score > bestScore) { bestScore = score; best = candidate.path("Phoneme").asText(""); }
        }
        String heard = LearnerIpa.of(best);
        return heard == null || heard.isBlank() || heard.equals(expected) ? null : heard;
    }

    private java.util.Optional<SpeechSegment> segment(JsonNode node) {
        if (!"Success".equals(node.path("RecognitionStatus").asText())) return java.util.Optional.empty();
        JsonNode best = node.path("NBest").path(0);
        JsonNode scores = best.path("PronunciationAssessment");
        if (best.isMissingNode() || scores.isMissingNode()) return java.util.Optional.empty();

        List<SpokenWord> words = new ArrayList<>();
        for (JsonNode word : best.path("Words")) {
            words.add(new SpokenWord(word.path("Word").asText(),
                    word.path("Offset").asLong() / UNITS_PER_MILLISECOND,
                    word.path("Duration").asLong() / UNITS_PER_MILLISECOND,
                    word.path("PronunciationAssessment").path("AccuracyScore").asDouble(),
                    word.path("PronunciationAssessment").path("ErrorType").asText("None"), phonemes(word.path("Phonemes"))));
        }
        String text = node.path("DisplayText").asText(best.path("Display").asText(""));
        Double prosody = scores.hasNonNull("ProsodyScore") ? scores.get("ProsodyScore").asDouble() : null;
        Double confidence = best.hasNonNull("Confidence") ? best.get("Confidence").asDouble() : null;
        return java.util.Optional.of(new SpeechSegment(text, scores.path("AccuracyScore").asDouble(),
                scores.path("FluencyScore").asDouble(), prosody, scores.path("CompletenessScore").asDouble(),
                scores.path("PronScore").asDouble(), confidence, words));
    }
}
