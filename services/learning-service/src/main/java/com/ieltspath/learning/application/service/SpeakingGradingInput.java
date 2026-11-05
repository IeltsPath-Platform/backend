package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.service.SpeakingHistorySummarizer.LearnerContext;
import com.ieltspath.learning.domain.service.SpeakingAnalysisSummary;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.TranscriptQuality;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Everything the grader is given about one submission: the answers by part and question with what was heard, the
 * measures taken from the speech, how far the transcript can be trusted, what is known of the learner and which codes
 * the model may name.
 *
 * @param errorCodes the Speaking error codes the model may use, with their names
 */
public record SpeakingGradingInput(SpeakingScope scope, List<Answer> answers, TranscriptQuality quality,
                                   SpeakingAnalysisSummary summary, LearnerContext learner, List<ErrorOption> errorCodes,
                                   int maxInputChars) {

    public record Answer(int partNo, int questionNo, String questionText, SpeechAnalysis analysis) { }

    public record ErrorOption(String code, String name) { }

    public SpeakingGradingInput {
        answers = List.copyOf(answers);
        errorCodes = List.copyOf(errorCodes);
    }

    /** The parts that were answered, in order. */
    public Set<Integer> parts() {
        return answers.stream().map(Answer::partNo).collect(Collectors.toCollection(TreeSet::new));
    }

    public Set<String> errorCodeSet() {
        return errorCodes.stream().map(ErrorOption::code).collect(Collectors.toSet());
    }

    /** Every recognised word of the submission, joined; what a quote must be found in. */
    public String transcript() {
        return answers.stream().map(answer -> answer.analysis().transcript()).collect(Collectors.joining(" "));
    }

    /** The transcript of one part. */
    public String transcriptOfPart(int partNo) {
        return answers.stream().filter(answer -> answer.partNo() == partNo)
                .map(answer -> answer.analysis().transcript()).collect(Collectors.joining(" "));
    }

    /** The weak words that carry sound scores: what the model is shown and the only words it may write notes about. */
    public List<com.ieltspath.learning.domain.vo.SpokenWord> detailWords() {
        return summary.weakWords().stream().filter(word -> !word.phonemes().isEmpty()).limit(DETAIL_WORDS).toList();
    }

    public static final int DETAIL_WORDS = 12;

    public int totalWords() {
        return answers.stream().mapToInt(answer -> answer.analysis().words().size()).sum();
    }

    /** Lower case and single spaces, so a quote matches the transcript whatever the model did with case and blanks. */
    public static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }
}
