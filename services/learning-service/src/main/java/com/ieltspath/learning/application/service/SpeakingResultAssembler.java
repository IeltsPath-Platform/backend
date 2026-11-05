package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.SpeakingResultValidator.Criterion;
import com.ieltspath.learning.application.service.SpeakingResultValidator.ErrorItem;
import com.ieltspath.learning.application.service.SpeakingResultValidator.KpObservation;
import com.ieltspath.learning.application.service.SpeakingResultValidator.PersonalFeedback;
import com.ieltspath.learning.application.service.SpeakingResultValidator.PronunciationNote;
import com.ieltspath.learning.application.service.SpeakingResultValidator.ValidatedGrade;
import com.ieltspath.learning.domain.service.PronunciationBandMapper;
import com.ieltspath.learning.domain.service.SpeakingAnalysisSummary;
import com.ieltspath.learning.domain.service.SpeakingRhythm;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpokenWord;
import com.ieltspath.learning.domain.vo.TranscriptQuality;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puts a checked grade together with everything the code measured into the stored result. The result has two layers
 * (plan 5b section 6.3): {@code display} is what the learner may read, {@code signals} is only recorded and never
 * changes the learning path by itself. The code, not the model, gives the Pronunciation band and the overall band, and
 * the code lowers a band when the answer was too short, off topic or not in English.
 */
@Component
public class SpeakingResultAssembler {
    public static final String SIGNALS_VERSION = "sig1";
    public static final String LABEL = "Điểm ước lượng để luyện tập";
    private static final List<String> DISPLAY_ORDER = List.of("FC", "LR", "GRA", "PR");
    private static final Set<Integer> FULL_TEST_PARTS = Set.of(1, 2, 3);

    /** The stored result and the overall band that is also kept in its own column; null without a full test. */
    public record Assembled(Map<String, Object> result, BigDecimal overallBand) { }

    private record Limit(String reason, BigDecimal cap, String message) { }

    private final SpeakingPracticeSettings settings;

    public SpeakingResultAssembler(SpeakingPracticeSettings settings) {
        this.settings = settings;
    }

    public Assembled assemble(LearningContentClient.Rubric rubric, SpeakingGradingInput input, ValidatedGrade grade) {
        SpeakingAnalysisSummary speech = SpeakingAnalysisSummary.of(
                input.answers().stream().map(SpeakingGradingInput.Answer::analysis).toList(),
                PronunciationBandMapper.INTERIM, 10);
        BigDecimal passBand = BigDecimal.valueOf(settings.passBand());
        List<Limit> limits = limits(grade, input);
        BigDecimal cap = limits.stream().map(Limit::cap).min(Comparator.naturalOrder()).orElse(null);
        boolean usable = input.quality().level() != TranscriptQuality.Level.LOW && limits.isEmpty()
                && speech.pronunciationBand() != null;

        List<Row> rows = new ArrayList<>();
        for (Criterion criterion : grade.criteria()) {
            rows.add(new Row(criterion.code(), criterion.band(), capped(criterion.band(), cap), criterion));
        }
        if (speech.pronunciationBand() != null) {
            rows.add(new Row("PR", speech.pronunciationBand(), capped(speech.pronunciationBand(), cap), null));
        }
        BigDecimal overall = overall(rows, input);

        Map<String, Object> display = display(rubric, input, grade, speech, rows, overall, limits, passBand);
        Map<String, Object> signals = signals(rubric, input, grade, speech, rows, overall, limits, passBand, usable);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("display", display);
        result.put("signals", signals);
        return new Assembled(result, overall);
    }

    /** The band the learner sees ({@code shown}) next to the band the model or the table gave ({@code given}). */
    private record Row(String code, BigDecimal given, BigDecimal shown, Criterion model) { }

    // ---- band limits and overall ------------------------------------------------------------------------------

    private List<Limit> limits(ValidatedGrade grade, SpeakingGradingInput input) {
        List<Limit> limits = new ArrayList<>();
        if (!grade.flags().englishOnly()) {
            limits.add(new Limit("NOT_ENGLISH", BigDecimal.valueOf(settings.notEnglishBandCap()),
                    "Bài nói có đoạn không phải tiếng Anh nên band bị giới hạn."));
        }
        if (!grade.flags().onTopic()) {
            limits.add(new Limit("OFF_TOPIC", BigDecimal.valueOf(settings.offTopicBandCap()),
                    "Bài nói lạc đề so với câu hỏi nên band bị giới hạn."));
        }
        if (grade.flags().tooShort() || input.totalWords() < settings.minTotalWords()) {
            limits.add(new Limit("TOO_SHORT", BigDecimal.valueOf(settings.tooShortBandCap()),
                    "Bài nói quá ngắn để đánh giá đầy đủ nên band bị giới hạn."));
        }
        return limits;
    }

    private static BigDecimal capped(BigDecimal band, BigDecimal cap) {
        return cap == null ? band : band.min(cap);
    }

    /** Mean of the four bands to the nearest half band (a quarter or three quarters rounds up); only for a full test. */
    private static BigDecimal overall(List<Row> rows, SpeakingGradingInput input) {
        boolean fullTest = input.scope() == SpeakingScope.FULL && input.parts().equals(FULL_TEST_PARTS);
        if (!fullTest || rows.size() != 4) return null;
        BigDecimal sum = rows.stream().map(Row::shown).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(4), 3, RoundingMode.HALF_UP);
        return mean.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }

    // ---- display ----------------------------------------------------------------------------------------------

    private Map<String, Object> display(LearningContentClient.Rubric rubric, SpeakingGradingInput input,
                                        ValidatedGrade grade, SpeakingAnalysisSummary speech, List<Row> rows,
                                        BigDecimal overall, List<Limit> limits, BigDecimal passBand) {
        Map<String, Object> display = new LinkedHashMap<>();
        display.put("label", LABEL);
        display.put("scope", input.scope().name());
        display.put("overallBand", overall);
        display.put("criteria", rows.stream().map(row -> criterionView(rubric, row, speech)).toList());
        display.put("summary", grade.summary());
        display.put("focus", grade.focus());
        display.put("partNotes", grade.partNotes().stream()
                .map(note -> Map.<String, Object>of("partNo", note.partNo(), "note", note.note())).toList());
        display.put("bandLimits", limits.stream()
                .map(limit -> Map.<String, Object>of("reason", limit.reason(), "cap", limit.cap(), "message", limit.message()))
                .toList());
        display.put("transcripts", input.answers().stream().sorted(Comparator
                        .comparingInt(SpeakingGradingInput.Answer::partNo).thenComparingInt(SpeakingGradingInput.Answer::questionNo))
                .map(answer -> Map.<String, Object>of("partNo", answer.partNo(), "questionNo", answer.questionNo(),
                        "question", answer.questionText() == null ? "" : answer.questionText(),
                        "transcript", answer.analysis().transcript(), "words", wordViews(answer.analysis().words()))).toList());
        display.put("transcriptQuality", transcriptQuality(input.quality(), grade));
        display.put("rhythm", rhythm(speech.rhythm(), input));
        display.put("pronunciation", pronunciation(speech, grade.pronunciationNotes()));
        display.put("errors", grade.errors().stream().map(error -> errorView(error, input)).toList());
        display.put("personalFeedback", personalFeedback(grade, rows, rubric));
        display.put("criteriaToPractise", rows.stream().filter(row -> row.shown().compareTo(passBand) < 0)
                .sorted(Comparator.comparing(Row::shown))
                .map(row -> Map.<String, Object>of("code", row.code(), "name", criterionName(rubric, row.code()),
                        "gap", passBand.subtract(row.shown()))).toList());
        return display;
    }

    private static Map<String, Object> criterionView(LearningContentClient.Rubric rubric, Row row,
                                                     SpeakingAnalysisSummary speech) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("code", row.code());
        view.put("name", criterionName(rubric, row.code()));
        view.put("band", row.shown());
        if (row.shown().compareTo(row.given()) < 0) view.put("limited", true);
        if (row.model() != null) {
            view.put("comment", row.model().comment());
            view.put("strengths", row.model().strengths());
            view.put("weaknesses", row.model().weaknesses());
            view.put("evidence", row.model().evidence().stream()
                    .map(item -> Map.<String, Object>of("quote", item.quote(), "note", item.note())).toList());
        } else {
            view.put("comment", pronunciationComment(speech));
            view.put("strengths", List.of());
            view.put("weaknesses", List.of());
            view.put("evidence", List.of());
            view.put("source", "speech-analysis");
        }
        return view;
    }

    private static String criterionName(LearningContentClient.Rubric rubric, String code) {
        return rubric.criteria().stream().filter(item -> code.equals(item.code())).map(
                LearningContentClient.RubricCriterion::name).findFirst().orElse(code);
    }

    private static String pronunciationComment(SpeakingAnalysisSummary speech) {
        StringBuilder comment = new StringBuilder(String.format(java.util.Locale.ROOT,
                "Band Phát âm được ước tính tự động từ phân tích giọng nói (độ chính xác %.0f, độ trôi chảy %.0f trên thang 100).",
                speech.accuracy(), speech.fluency()));
        if (!speech.weakWords().isEmpty()) {
            comment.append(" Một số từ cần luyện thêm: ")
                    .append(String.join(", ", speech.weakWords().stream().limit(5).map(SpokenWord::text).toList())).append('.');
        }
        return comment.toString();
    }

    private static Map<String, Object> transcriptQuality(TranscriptQuality quality, ValidatedGrade grade) {
        Map<String, Object> view = new LinkedHashMap<>();
        boolean low = quality.level() == TranscriptQuality.Level.LOW;
        view.put("level", quality.level().name());
        view.put("meanConfidence", quality.meanConfidence() == null ? null : round(quality.meanConfidence(), 2));
        view.put("warning", low);
        view.put("message", low ? "Bản ghi có thể chưa được nhận dạng chính xác; điểm chỉ mang tính tham khảo." : "");
        if (grade.reliability() != null && !grade.reliability().reason().isBlank()) {
            view.put("note", grade.reliability().reason());
        }
        return view;
    }

    private static Map<String, Object> rhythm(SpeakingRhythm combined, SpeakingGradingInput input) {
        Map<String, Object> view = rhythmView(combined);
        List<Map<String, Object>> byPart = new ArrayList<>();
        for (int partNo : input.parts()) {
            SpeakingRhythm part = SpeakingRhythm.combine(input.answers().stream()
                    .filter(answer -> answer.partNo() == partNo).map(answer -> SpeakingRhythm.of(answer.analysis())).toList());
            Map<String, Object> row = rhythmView(part);
            row.put("partNo", partNo);
            byPart.add(row);
        }
        view.put("byPart", byPart);
        return view;
    }

    private static Map<String, Object> rhythmView(SpeakingRhythm rhythm) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("wordCount", rhythm.wordCount());
        view.put("wordsPerMinute", round(rhythm.wordsPerMinute(), 0));
        view.put("longPauses", rhythm.longPauses());
        view.put("longestPauseMs", rhythm.longestPauseMs());
        view.put("pauseRatio", round(rhythm.pauseRatio(), 2));
        return view;
    }

    private static Map<String, Object> pronunciation(SpeakingAnalysisSummary speech, List<PronunciationNote> notes) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("accuracy", round(speech.accuracy(), 1));
        view.put("fluency", round(speech.fluency(), 1));
        view.put("pronunciation", round(speech.pronunciation(), 1));
        view.put("prosody", speech.prosody() == null ? null : round(speech.prosody(), 1));
        view.put("weakWords", weakWords(speech, notes));
        view.put("weakSounds", weakSounds(speech, notes));
        return view;
    }

    /** Every word of an answer with its score and sounds, so the learner can see how each one went. */
    private static List<Map<String, Object>> wordViews(List<SpokenWord> words) {
        return words.stream().map(word -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("text", word.text());
            row.put("accuracy", round(word.accuracy(), 0));
            row.put("errorType", word.hasError() ? word.errorType() : null);
            row.put("sounds", soundViews(word));
            return row;
        }).toList();
    }

    private static List<Map<String, Object>> soundViews(SpokenWord word) {
        return word.phonemes().stream().map(sound -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", sound.symbol());
            row.put("accuracy", round(sound.accuracy(), 0));
            row.put("heardAs", sound.heardAs());
            return row;
        }).toList();
    }

    /** The weak words with their sounds (the service's scores) and, where the model wrote one, the tip for the word. */
    private static List<Map<String, Object>> weakWords(SpeakingAnalysisSummary speech, List<PronunciationNote> notes) {
        return speech.weakWords().stream().map(word -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("word", word.text());
            row.put("accuracy", round(word.accuracy(), 1));
            row.put("errorType", word.errorType());
            row.put("sounds", soundViews(word));
            row.put("tip", notes.stream().filter(note -> note.word().equalsIgnoreCase(word.text()))
                    .map(PronunciationNote::tip).findFirst().orElse(null));
            return row;
        }).toList();
    }

    /** The sounds weak across the speech, each with the words it was weak in and, where the model wrote one, a tip. */
    private static List<Map<String, Object>> weakSounds(SpeakingAnalysisSummary speech, List<PronunciationNote> notes) {
        return speech.weakSounds().stream().map(sound -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", sound.symbol());
            row.put("count", sound.count());
            row.put("accuracy", round(sound.meanAccuracy(), 0));
            row.put("heardAs", sound.heardAs());
            row.put("words", sound.words());
            row.put("tip", notes.stream().filter(note -> sound.symbol().equals(note.sound()))
                    .map(PronunciationNote::tip).findFirst().orElse(null));
            return row;
        }).toList();
    }
    private static Map<String, Object> errorView(ErrorItem error, SpeakingGradingInput input) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("code", error.code());
        view.put("name", input.errorCodes().stream().filter(option -> option.code().equals(error.code()))
                .map(SpeakingGradingInput.ErrorOption::name).findFirst().orElse(error.code()));
        view.put("partNo", error.partNo());
        view.put("excerpt", error.excerpt());
        view.put("suggestion", error.suggestion());
        return view;
    }

    /** The model's advice when it gave a usable one, otherwise a stock note built from the lowest band. */
    private static Map<String, Object> personalFeedback(ValidatedGrade grade, List<Row> rows,
                                                        LearningContentClient.Rubric rubric) {
        PersonalFeedback advice = grade.personalFeedback();
        Map<String, Object> view = new LinkedHashMap<>();
        if (advice != null && !advice.note().isBlank()) {
            view.put("note", advice.note());
            view.put("priorityKps", advice.priorityKps().stream()
                    .map(item -> Map.<String, Object>of("kpCode", item.kpCode(), "excerpt", item.excerpt())).toList());
            view.put("studyPlan", advice.studyPlan());
            return view;
        }
        Row weakest = rows.stream().min(Comparator.comparing(Row::shown)).orElse(null);
        view.put("note", weakest == null ? "" : "Tiêu chí nên ưu tiên luyện tiếp là "
                + criterionName(rubric, weakest.code()) + " (band " + weakest.shown().toPlainString() + ").");
        view.put("priorityKps", List.of());
        view.put("studyPlan", grade.focus());
        return view;
    }

    // ---- signals ----------------------------------------------------------------------------------------------

    private Map<String, Object> signals(LearningContentClient.Rubric rubric, SpeakingGradingInput input,
                                        ValidatedGrade grade, SpeakingAnalysisSummary speech, List<Row> rows,
                                        BigDecimal overall, List<Limit> limits, BigDecimal passBand, boolean usable) {
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("signalsVersion", SIGNALS_VERSION);
        signals.put("scope", input.scope().name());
        signals.put("partsCovered", List.copyOf(input.parts()));
        signals.put("transcriptQuality", input.quality().level().name());
        signals.put("asrConfidence", input.quality().meanConfidence());
        signals.put("transcriptReliability", grade.reliability() == null ? null : grade.reliability().level());
        signals.put("flags", Map.of("onTopic", grade.flags().onTopic(), "englishOnly", grade.flags().englishOnly(),
                "tooShort", grade.flags().tooShort() || input.totalWords() < settings.minTotalWords()));
        signals.put("usableAsEvidence", usable);
        signals.put("criteria", rows.stream().map(row -> signalRow(rubric, row, passBand, speech)).toList());
        signals.put("overallBand", overall);
        signals.put("bandLimits", limits.stream().map(Limit::reason).toList());
        signals.put("errorCounts", errorCounts(grade.errors()));
        signals.put("errors", grade.errors().stream().map(error -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", error.code());
            row.put("partNo", error.partNo());
            row.put("excerpt", error.excerpt());
            return row;
        }).toList());
        Map<String, UUID> kpIds = input.learner().idsByCode();
        signals.put("kpObservations", grade.kpObservations().stream().map(item -> observation(item, kpIds)).toList());
        signals.put("weakWords", speech.weakWords().stream().map(word -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("word", word.text());
            row.put("accuracy", round(word.accuracy(), 1));
            return row;
        }).toList());
        signals.put("weakSounds", speech.weakSounds().stream().map(sound -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", sound.symbol());
            row.put("count", sound.count());
            row.put("accuracy", round(sound.meanAccuracy(), 0));
            row.put("heardAs", sound.heardAs());
            return row;
        }).toList());
        signals.put("rhythm", rhythmView(speech.rhythm()));
        signals.put("pronunciationTableVersion", speech.bandTableVersion());
        return signals;
    }

    private static Map<String, Object> signalRow(LearningContentClient.Rubric rubric, Row row, BigDecimal passBand,
                                                 SpeakingAnalysisSummary speech) {
        Map<String, Object> signal = new LinkedHashMap<>();
        signal.put("code", row.code());
        signal.put("band", row.shown());
        if (row.shown().compareTo(row.given()) < 0) signal.put("bandBeforeLimit", row.given());
        signal.put("passBand", passBand);
        signal.put("passed", row.shown().compareTo(passBand) >= 0);
        signal.put("gap", passBand.subtract(row.shown()).max(BigDecimal.ZERO));
        rubric.criteria().stream().filter(item -> row.code().equals(item.code())).findFirst()
                .ifPresent(item -> signal.put("kpId", item.knowledgePointId()));
        if (row.model() == null) {
            signal.put("bandSource", "pronunciation-table");
            signal.put("tableVersion", speech.bandTableVersion());
        }
        return signal;
    }

    private static Map<String, Integer> errorCounts(List<ErrorItem> errors) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        errors.forEach(error -> counts.merge(error.code(), 1, Integer::sum));
        return counts;
    }

    /** Recorded only: the learning path is updated from the criterion bands, never from these observations. */
    private static Map<String, Object> observation(KpObservation item, Map<String, UUID> kpIds) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("kpCode", item.kpCode());
        row.put("kpId", kpIds.get(item.kpCode()));
        row.put("stance", item.stance());
        row.put("partNo", item.partNo());
        row.put("excerpt", item.excerpt());
        row.put("excerptVerified", true);
        row.put("reason", item.reason());
        row.put("applied", false);
        return row;
    }

    private static double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
