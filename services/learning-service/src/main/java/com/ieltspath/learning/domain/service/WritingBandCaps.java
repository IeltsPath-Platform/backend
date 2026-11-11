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
    public enum Reason { TOO_SHORT, OFF_TOPIC, T1_NO_OVERVIEW, PARTIAL_ANSWER, NO_POSITION }

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
