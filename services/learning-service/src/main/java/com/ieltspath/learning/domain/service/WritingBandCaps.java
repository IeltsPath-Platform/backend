package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingTask;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hard caps on Writing criterion bands. Pure: no Spring, no clock, no I/O.
 *
 * <p>The caps are the centre's rules, not the model's judgement. A cap only lowers a band. A null cap switches its
 * rule off, which is how the caps stay inert until an examiner has set the thresholds.
 */
public final class WritingBandCaps {
    private WritingBandCaps() {}

    /** Stable reason codes; {@code TOO_SHORT} and {@code OFF_TOPIC} share their names with Speaking. */
    public enum Reason {
        TOO_SHORT, OFF_TOPIC, T1_NO_OVERVIEW, PARTIAL_ANSWER, NO_POSITION,
        DENSE_GRAMMAR, DENSE_IMPEDING_GRAMMAR, DENSE_VOCABULARY, DENSE_COHERENCE, WRONG_DATA, WRONG_DATA_OVERVIEW
    }

    /**
     * One applied cap. {@code criterion} is the criterion it lowers (TA or TR, and the same code for every Writing
     * rule); {@code cap} is the highest band the criterion may show.
     */
    public record Limit(Reason reason, String criterion, BigDecimal cap, String message) {}

    /** Flags the grader raises from the model's reply, each with a verified excerpt; all false when none was given. */
    public record Flags(boolean offTopic, boolean noOverview, boolean partialAnswer, boolean noPosition) {
        public static final Flags NONE = new Flags(false, false, false, false);
    }

    /** One threshold per rule; null disables the rule. */
    public record Caps(BigDecimal tooShort, BigDecimal offTopic, BigDecimal noOverview,
                       BigDecimal partialAnswer, BigDecimal noPosition) {
        public static final Caps NONE = new Caps(null, null, null, null, null);
    }

    /**
     * What counting an essay and its corrections gives: sentences in all and the sentences that carry a grammar, a
     * vocabulary or a coherence error (a wrong linking word, an unclear reference), the sentences whose grammar error
     * leaves the reader guessing the meaning, and for Task 1 the statements about the chart that were judged and the
     * ones judged wrong.
     */
    public record Density(int sentences, int grammarSentences, int vocabularySentences, int coherenceSentences,
                          int impedingGrammarSentences, int dataStatements, int wrongDataStatements,
                          boolean overviewWrong) {
        public static final Density NONE = new Density(0, 0, 0, 0, 0, 0, 0, false);
    }

    /**
     * Shares of the essay at which a density rule fires, and the cap each rule sets; a null cap switches its rule off.
     * {@code wrongDataOverviewCap} applies when the overview is wrong as well as most statements.
     */
    public record DensityRules(BigDecimal grammarShare, BigDecimal vocabularyShare, BigDecimal coherenceShare,
                               BigDecimal impedingGrammarShare, BigDecimal wrongDataShare, BigDecimal grammarCap,
                               BigDecimal vocabularyCap, BigDecimal coherenceCap, BigDecimal impedingGrammarCap,
                               BigDecimal wrongDataCap, BigDecimal wrongDataOverviewCap) {
        public static final DensityRules NONE = new DensityRules(null, null, null, null, null, null, null, null, null,
                null, null);
    }

    /**
     * The caps for an essay whose errors are spread through its sentences: GRA, LR and CC from the shares of sentences
     * with a grammar, a vocabulary or a coherence error, and for Task 1 TA from the share of chart statements that are
     * wrong. A rule needs both its cap and its share set, and something to count; otherwise it adds nothing.
     */
    public static List<Limit> evaluateDensity(WritingTask task, Density density, DensityRules rules) {
        List<Limit> limits = new ArrayList<>();
        if (density.sentences() > 0 && rules.grammarCap() != null && reaches(density.grammarSentences(), density.sentences(), rules.grammarShare())) {
            limits.add(new Limit(Reason.DENSE_GRAMMAR, "GRA", rules.grammarCap(),
                    "Lỗi ngữ pháp có ở " + density.grammarSentences() + "/" + density.sentences() + " câu nên GRA bị giới hạn ở "
                            + rules.grammarCap().stripTrailingZeros().toPlainString()));
        }
        if (density.sentences() > 0 && rules.impedingGrammarCap() != null
                && reaches(density.impedingGrammarSentences(), density.sentences(), rules.impedingGrammarShare())) {
            limits.add(new Limit(Reason.DENSE_IMPEDING_GRAMMAR, "GRA", rules.impedingGrammarCap(),
                    "Lỗi ngữ pháp làm khó hiểu nghĩa có ở " + density.impedingGrammarSentences() + "/" + density.sentences()
                            + " câu nên GRA bị giới hạn ở " + rules.impedingGrammarCap().stripTrailingZeros().toPlainString()));
        }
        if (density.sentences() > 0 && rules.vocabularyCap() != null && reaches(density.vocabularySentences(), density.sentences(), rules.vocabularyShare())) {
            limits.add(new Limit(Reason.DENSE_VOCABULARY, "LR", rules.vocabularyCap(),
                    "Lỗi từ vựng có ở " + density.vocabularySentences() + "/" + density.sentences() + " câu nên LR bị giới hạn ở "
                            + rules.vocabularyCap().stripTrailingZeros().toPlainString()));
        }
        if (density.sentences() > 0 && rules.coherenceCap() != null && reaches(density.coherenceSentences(), density.sentences(), rules.coherenceShare())) {
            limits.add(new Limit(Reason.DENSE_COHERENCE, "CC", rules.coherenceCap(),
                    "Lỗi liên kết (từ nối, đại từ thay thế) có ở " + density.coherenceSentences() + "/" + density.sentences()
                            + " câu nên CC bị giới hạn ở " + rules.coherenceCap().stripTrailingZeros().toPlainString()));
        }
        if (task == WritingTask.TASK_1 && density.dataStatements() > 0
                && reaches(density.wrongDataStatements(), density.dataStatements(), rules.wrongDataShare())) {
            if (rules.wrongDataCap() != null) {
                limits.add(new Limit(Reason.WRONG_DATA, "TA", rules.wrongDataCap(),
                        density.wrongDataStatements() + "/" + density.dataStatements() + " câu nói về biểu đồ sai nên TA bị giới hạn ở "
                                + rules.wrongDataCap().stripTrailingZeros().toPlainString()));
            }
            if (rules.wrongDataOverviewCap() != null && density.overviewWrong()) {
                limits.add(new Limit(Reason.WRONG_DATA_OVERVIEW, "TA", rules.wrongDataOverviewCap(),
                        "Tổng quan sai và phần lớn câu nói về biểu đồ sai nên TA bị giới hạn ở "
                                + rules.wrongDataOverviewCap().stripTrailingZeros().toPlainString()));
            }
        }
        return List.copyOf(limits);
    }

    /** True when {@code part} is at least {@code share} of {@code whole}; a null share never reaches. */
    private static boolean reaches(int part, int whole, BigDecimal share) {
        return share != null && whole > 0 && BigDecimal.valueOf(part).compareTo(share.multiply(BigDecimal.valueOf(whole))) >= 0;
    }

    /**
     * The caps that apply to an essay with {@code words} words against a prompt that asks for {@code minWords}.
     * A rule that does not apply, or whose cap is null, adds nothing. The result is in the order of the rules.
     */
    public static List<Limit> evaluate(WritingTask task, int words, int minWords, Flags flags, Caps caps) {
        List<Limit> limits = new ArrayList<>();
        String scored = task.criteria().getFirst(); // TA for Task 1, TR for Task 2
        if (caps.tooShort() != null && words < minWords) {
            limits.add(new Limit(Reason.TOO_SHORT, scored, caps.tooShort(),
                    "Bài chưa đủ " + minWords + " từ nên " + scored + " bị giới hạn ở " + caps.tooShort().stripTrailingZeros().toPlainString()));
        }
        if (caps.offTopic() != null && flags.offTopic()) {
            limits.add(new Limit(Reason.OFF_TOPIC, scored, caps.offTopic(),
                    "Bài lạc đề nên " + scored + " bị giới hạn ở " + caps.offTopic().stripTrailingZeros().toPlainString()));
        }
        if (task == WritingTask.TASK_1 && caps.noOverview() != null && flags.noOverview()) {
            limits.add(new Limit(Reason.T1_NO_OVERVIEW, "TA", caps.noOverview(),
                    "Thiếu đoạn tổng quan nên TA bị giới hạn ở " + caps.noOverview().stripTrailingZeros().toPlainString()));
        }
        if (task == WritingTask.TASK_2 && caps.partialAnswer() != null && flags.partialAnswer()) {
            limits.add(new Limit(Reason.PARTIAL_ANSWER, "TR", caps.partialAnswer(),
                    "Chưa trả lời đủ yêu cầu của đề nên TR bị giới hạn ở " + caps.partialAnswer().stripTrailingZeros().toPlainString()));
        }
        if (task == WritingTask.TASK_2 && caps.noPosition() != null && flags.noPosition()) {
            limits.add(new Limit(Reason.NO_POSITION, "TR", caps.noPosition(),
                    "Đề hỏi ý kiến nhưng bài chưa nêu quan điểm nên TR bị giới hạn ở " + caps.noPosition().stripTrailingZeros().toPlainString()));
        }
        return List.copyOf(limits);
    }

    /**
     * The band each criterion shows after the caps: the lower of its own band and every cap that names it. A band
     * already at or under its caps is returned unchanged.
     */
    public static Map<String, BigDecimal> apply(Map<String, BigDecimal> bands, List<Limit> limits) {
        Map<String, BigDecimal> capped = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : bands.entrySet()) {
            BigDecimal band = entry.getValue();
            for (Limit limit : limits) {
                if (limit.criterion().equals(entry.getKey()) && band.compareTo(limit.cap()) > 0) {
                    band = limit.cap();
                }
            }
            capped.put(entry.getKey(), band);
        }
        return capped;
    }
}
