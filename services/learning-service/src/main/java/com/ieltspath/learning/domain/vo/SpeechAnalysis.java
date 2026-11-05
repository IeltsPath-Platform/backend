package com.ieltspath.learning.domain.vo;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * What the speech service made of one recording, in this system's own terms: the transcript and the pronunciation
 * scores, segment by segment. Nothing here names the service that produced it.
 */
public record SpeechAnalysis(List<SpeechSegment> segments) {
    public SpeechAnalysis {
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    public String transcript() {
        return String.join(" ", segments.stream().map(SpeechSegment::text).filter(text -> !text.isBlank()).toList());
    }

    public List<SpokenWord> words() {
        return segments.stream().flatMap(segment -> segment.words().stream()).toList();
    }

    public boolean isEmpty() {
        return words().isEmpty();
    }

    /** Mean ASR confidence weighted by recognized words; null when the provider did not return confidence. */
    public Double meanConfidence() {
        double total = 0;
        double weight = 0;
        for (SpeechSegment segment : segments) {
            if (segment.confidence() == null) continue;
            int words = Math.max(1, segment.words().size());
            total += segment.confidence() * words;
            weight += words;
        }
        return weight == 0 ? null : total / weight;
    }

    public double lowPronunciationWordRatio() {
        List<SpokenWord> words = words();
        return words.isEmpty() ? 0 : (double) words.stream().filter(word -> word.accuracy() < 60).count() / words.size();
    }

    /** Mean of the segments' accuracy, each weighted by its number of words; 0 when nothing was heard. */
    public double accuracy() {
        return weighted(SpeechSegment::accuracy);
    }

    public double fluency() {
        return weighted(SpeechSegment::fluency);
    }

    public double pronunciation() {
        return weighted(SpeechSegment::pronunciation);
    }

    /** Null when no segment has a prosody score. */
    public Double prosody() {
        double total = 0;
        double weight = 0;
        for (SpeechSegment segment : segments) {
            if (segment.prosody() == null) continue;
            total += segment.prosody() * segment.words().size();
            weight += segment.words().size();
        }
        return weight == 0 ? null : total / weight;
    }

    private double weighted(ToDoubleFunction<SpeechSegment> score) {
        double total = 0;
        double weight = 0;
        for (SpeechSegment segment : segments) {
            total += score.applyAsDouble(segment) * segment.words().size();
            weight += segment.words().size();
        }
        return weight == 0 ? 0 : total / weight;
    }
}
