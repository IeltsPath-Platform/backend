package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks what the model said before any of it is kept. A grade the model has to be trusted for (the three bands) is
 * all-or-nothing: a missing, non-numeric or off-scale band makes the whole answer invalid, because a guessed band would
 * be charged for. Everything else is optional colour: an item that cannot be checked (a quote that is not in the
 * transcript, a code that was not offered) is dropped alone and the bands stay.
 */
@Component
public class SpeakingResultValidator {
    private static final BigDecimal NINE = BigDecimal.valueOf(9);
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final int MAX_QUOTE = 160;
    private static final Set<String> RELIABILITY = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> STANCES = Set.of("STRENGTH", "WEAKNESS");

    public record Evidence(String quote, String note) { }

    public record Criterion(String code, BigDecimal band, String comment, List<String> strengths, List<String> weaknesses,
                            List<Evidence> evidence) { }

    public record PartNote(int partNo, String note) { }

    /** A missing flag does not accuse the learner of anything: it reads as on topic, in English and long enough. */
    public record Flags(boolean onTopic, boolean englishOnly, boolean tooShort) { }

    public record Reliability(String level, String reason) { }

    public record ErrorItem(String code, Integer partNo, String excerpt, String suggestion) { }

    public record KpObservation(String kpCode, String stance, Integer partNo, String excerpt, String reason) { }

    public record PriorityKp(String kpCode, String excerpt) { }

    /** Advice on one weak word, optionally on one of its sounds. Both come from the speech service's own list. */
    public record PronunciationNote(String word, String sound, String tip) { }

    public record PersonalFeedback(String note, List<PriorityKp> priorityKps, List<String> studyPlan) { }

    public record ValidatedGrade(List<Criterion> criteria, List<PartNote> partNotes, Flags flags, Reliability reliability,
                                 String summary, List<String> focus, List<ErrorItem> errors,
                                 List<KpObservation> kpObservations, PersonalFeedback personalFeedback,
                                 List<PronunciationNote> pronunciationNotes) { }

    /** The model's answer cannot be used as a grade. */
    public static class InvalidSpeakingGradeException extends RuntimeException { }

    private final ObjectMapper json;

    public SpeakingResultValidator(ObjectMapper json) {
        this.json = json;
    }

    /**
     * @param allowedKpCodes the knowledge point codes the prompt offered (catalog and history)
     * @throws InvalidSpeakingGradeException when the answer is not a JSON object or its bands cannot be trusted
     */
    public ValidatedGrade validate(String raw, SpeakingGradingInput input, Set<String> allowedKpCodes) {
        JsonNode root = parse(raw);
        String transcript = SpeakingGradingInput.normalize(input.transcript());
        Set<Integer> parts = input.parts();
        return new ValidatedGrade(criteria(root, transcript), partNotes(root.path("partNotes"), parts), flags(root.path("flags")),
                reliability(root.path("transcriptReliability")), clean(root.path("summary").asText(""), 500),
                texts(root.path("focus"), 4, 200), errors(root.path("errors"), input, transcript, parts),
                observations(root.path("kpObservations"), allowedKpCodes, transcript, parts),
                personalFeedback(root.path("personalFeedback"), allowedKpCodes, transcript),
                pronunciationNotes(root.path("pronunciationNotes"), input));
    }

    private JsonNode parse(String raw) {
        try {
            JsonNode value = json.readTree(raw);
            if (value != null && value.isObject()) return value;
        } catch (Exception ignored) {
            // falls through to the same refusal as any other unusable answer
        }
        throw new InvalidSpeakingGradeException();
    }

    private static List<Criterion> criteria(JsonNode root, String transcript) {
        JsonNode array = root.path("criteria");
        if (!array.isArray()) throw new InvalidSpeakingGradeException();
        Map<String, JsonNode> byCode = new LinkedHashMap<>();
        for (JsonNode item : array) {
            String code = item.path("code").asText("");
            if (SpeakingPromptAssembler.GRADED_CRITERIA.contains(code)) byCode.putIfAbsent(code, item);
        }
        List<Criterion> criteria = new ArrayList<>();
        for (String code : SpeakingPromptAssembler.GRADED_CRITERIA) {
            JsonNode item = byCode.get(code);
            if (item == null) throw new InvalidSpeakingGradeException();
            criteria.add(new Criterion(code, band(item.path("band")), clean(item.path("comment").asText(""), 600),
                    texts(item.path("strengths"), 4, 200), texts(item.path("weaknesses"), 4, 200),
                    evidence(item.path("evidence"), transcript)));
        }
        return criteria;
    }

    /** A JSON number from 0 to 9 in steps of 0.5; anything else, including a missing field, is not a band. */
    private static BigDecimal band(JsonNode node) {
        if (!node.isNumber()) throw new InvalidSpeakingGradeException();
        BigDecimal band = node.decimalValue();
        if (band.compareTo(BigDecimal.ZERO) < 0 || band.compareTo(NINE) > 0 || band.remainder(HALF).signum() != 0) {
            throw new InvalidSpeakingGradeException();
        }
        return band.stripTrailingZeros().scale() < 1 ? band.setScale(1) : band;
    }

    private static List<Evidence> evidence(JsonNode node, String transcript) {
        List<Evidence> kept = new ArrayList<>();
        if (!node.isArray()) return kept;
        for (JsonNode item : node) {
            String quote = item.path("quote").asText("").strip();
            if (!quoted(quote, transcript)) continue;
            kept.add(new Evidence(quote, clean(item.path("note").asText(""), 200)));
            if (kept.size() == 5) break;
        }
        return kept;
    }

    private static List<PartNote> partNotes(JsonNode node, Set<Integer> parts) {
        List<PartNote> kept = new ArrayList<>();
        if (!node.isArray()) return kept;
        for (JsonNode item : node) {
            if (!item.path("partNo").isInt() || !parts.contains(item.path("partNo").asInt())) continue;
            String note = clean(item.path("note").asText(""), 300);
            if (!note.isBlank()) kept.add(new PartNote(item.path("partNo").asInt(), note));
        }
        return kept;
    }

    private static Flags flags(JsonNode node) {
        return new Flags(node.path("onTopic").asBoolean(true), node.path("englishOnly").asBoolean(true),
                node.path("tooShort").asBoolean(false));
    }

    private static Reliability reliability(JsonNode node) {
        String level = node.path("level").asText("").toUpperCase(java.util.Locale.ROOT);
        return RELIABILITY.contains(level) ? new Reliability(level, clean(node.path("reason").asText(""), 200)) : null;
    }

    private static List<ErrorItem> errors(JsonNode node, SpeakingGradingInput input, String transcript, Set<Integer> parts) {
        List<ErrorItem> kept = new ArrayList<>();
        if (!node.isArray()) return kept;
        Set<String> allowed = input.errorCodeSet();
        for (JsonNode item : node) {
            String code = item.path("code").asText("");
            int partNo = partNo(item, parts);
            if (!allowed.contains(code) || partNo == BAD_PART) continue;
            String excerpt = item.path("excerpt").asText("").strip();
            if (!quoted(excerpt, transcript)) continue;
            kept.add(new ErrorItem(code, partNo == NO_PART ? null : partNo, excerpt, clean(item.path("suggestion").asText(""), 300)));
            if (kept.size() == 10) break;
        }
        return kept;
    }

    private static List<KpObservation> observations(JsonNode node, Set<String> allowedKps, String transcript, Set<Integer> parts) {
        List<KpObservation> kept = new ArrayList<>();
        if (!node.isArray()) return kept;
        for (JsonNode item : node) {
            String code = item.path("kpCode").asText("");
            String stance = item.path("stance").asText("").toUpperCase(java.util.Locale.ROOT);
            int partNo = partNo(item, parts);
            if (!allowedKps.contains(code) || !STANCES.contains(stance) || partNo == BAD_PART) continue;
            String excerpt = item.path("excerpt").asText("").strip();
            if (!quoted(excerpt, transcript)) continue;
            kept.add(new KpObservation(code, stance, partNo == NO_PART ? null : partNo, excerpt, clean(item.path("reason").asText(""), 200)));
            if (kept.size() == 8) break;
        }
        return kept;
    }

    /** The advice written for this learner. A wrong shape drops this part alone; the code then writes a stock note. */
    private static PersonalFeedback personalFeedback(JsonNode node, Set<String> allowedKps, String transcript) {
        if (!node.isObject()) return null;
        String note = clean(node.path("note").asText(""), 300);
        List<PriorityKp> priority = new ArrayList<>();
        if (node.path("priorityKps").isArray()) {
            for (JsonNode item : node.path("priorityKps")) {
                String code = item.path("kpCode").asText("");
                String excerpt = item.path("excerpt").asText("").strip();
                if (allowedKps.contains(code) && quoted(excerpt, transcript)) priority.add(new PriorityKp(code, excerpt));
                if (priority.size() == 3) break;
            }
        }
        List<String> plan = texts(node.path("studyPlan"), 4, 200);
        return note.isBlank() && plan.isEmpty() && priority.isEmpty() ? null : new PersonalFeedback(note, priority, plan);
    }

    /**
     * Notes on pronunciation. A note is kept only for a word the speech service listed as weak and, when it names a sound,
     * only a sound that word has: the model explains the service's findings, it does not add its own.
     */
    private static List<PronunciationNote> pronunciationNotes(JsonNode node, SpeakingGradingInput input) {
        List<PronunciationNote> kept = new ArrayList<>();
        if (!node.isArray()) return kept;
        Map<String, Set<String>> soundsByWord = new LinkedHashMap<>();
        input.detailWords().forEach(word -> soundsByWord.computeIfAbsent(word.text().toLowerCase(java.util.Locale.ROOT),
                key -> new java.util.HashSet<>()).addAll(word.phonemes().stream()
                .map(com.ieltspath.learning.domain.vo.SpokenPhoneme::symbol).toList()));
        Set<String> seen = new java.util.HashSet<>();
        for (JsonNode item : node) {
            String word = item.path("word").asText("").strip();
            String sound = item.path("sound").asText("").strip();
            String tip = clean(item.path("tip").asText(""), 300);
            Set<String> sounds = soundsByWord.get(word.toLowerCase(java.util.Locale.ROOT));
            if (tip.isBlank() || sounds == null || (!sound.isEmpty() && !sounds.contains(sound))) continue;
            if (seen.add(word.toLowerCase(java.util.Locale.ROOT) + "/" + sound)) kept.add(new PronunciationNote(word, sound, tip));
            if (kept.size() == 6) break;
        }
        return kept;
    }
    private static final int NO_PART = 0;
    private static final int BAD_PART = -1;

    /** {@link #NO_PART} when the item names no part, the part number when it names a real one, {@link #BAD_PART} otherwise. */
    private static int partNo(JsonNode item, Set<Integer> parts) {
        JsonNode node = item.path("partNo");
        if (node.isMissingNode() || node.isNull()) return NO_PART;
        return node.isInt() && parts.contains(node.asInt()) ? node.asInt() : BAD_PART;
    }

    /** A quote counts only if it is short enough and appears in the transcript, ignoring case and blanks. */
    private static boolean quoted(String quote, String normalizedTranscript) {
        if (quote == null || quote.isBlank() || quote.length() > MAX_QUOTE) return false;
        return normalizedTranscript.contains(SpeakingGradingInput.normalize(quote));
    }

    private static List<String> texts(JsonNode node, int max, int limit) {
        List<String> kept = new ArrayList<>();
        if (!node.isArray()) return kept;
        for (JsonNode item : node) {
            String text = clean(item.asText(""), limit);
            if (!text.isBlank()) kept.add(text);
            if (kept.size() == max) break;
        }
        return kept;
    }

    /** Text for display: no markup, no links, cut to the limit at the end of a sentence where there is one. */
    static String clean(String value, int limit) {
        if (value == null) return "";
        String safe = value.replaceAll("<[^>]*>", "").replaceAll("https?://\\S+", "").replaceAll("\\s+", " ").strip();
        if (safe.length() <= limit) return safe;
        String cut = safe.substring(0, limit);
        int end = Math.max(cut.lastIndexOf(". "), Math.max(cut.lastIndexOf("! "), cut.lastIndexOf("? ")));
        return end >= limit * 6 / 10 ? cut.substring(0, end + 1) : cut.strip();
    }
}
