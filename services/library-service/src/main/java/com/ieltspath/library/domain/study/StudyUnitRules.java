package com.ieltspath.library.domain.study;

import java.util.*;

/** Publication rules are independent of persistence and HTTP. */
public final class StudyUnitRules {
    private StudyUnitRules() {}
    public record Issue(String code, String message, UUID senseId) {}
    public record Validation(List<Issue> errors, List<Issue> warnings) {
        public boolean canPublish() { return errors.isEmpty(); }
    }
    public static Validation validate(List<StudyWordSnapshot> words, List<StudyWordSnapshot> pool) {
        var errors = new ArrayList<Issue>();
        var warnings = new ArrayList<Issue>();
        long valid = words.stream().filter(w -> w.active() && !StudyText.normalize(w.vietnameseMeaning()).isEmpty()).count();
        if (valid < 4) errors.add(new Issue("TOO_FEW_WORDS", "Cần ít nhất 4 từ hợp lệ.", null));
        Set<String> meanings = new HashSet<>();
        Set<UUID> items = new HashSet<>();
        Set<String> distractors = new HashSet<>();
        java.util.stream.Stream.concat(words.stream(), pool.stream()).filter(StudyWordSnapshot::active)
                .map(w -> StudyText.normalize(w.vietnameseMeaning())).filter(s -> !s.isEmpty()).forEach(distractors::add);
        for (var word : words) {
            UUID id = word.vocabularySenseId();
            if (!word.active()) errors.add(new Issue("INACTIVE_SENSE", "Từ hoặc nghĩa đã ngừng sử dụng.", id));
            String meaning = StudyText.normalize(word.vietnameseMeaning());
            if (meaning.isEmpty()) errors.add(new Issue("MISSING_MEANING", "Từ chưa có nghĩa tiếng Việt.", id));
            else if (!meanings.add(meaning)) errors.add(new Issue("DUPLICATE_MEANING", "Hai từ có nghĩa tiếng Việt trùng nhau.", id));
            if (!items.add(word.vocabularyItemId())) warnings.add(new Issue("SAME_WORD_TWICE", "Bài có nhiều nghĩa của cùng một từ.", id));
            if (distractors.size() < 4) warnings.add(new Issue("FEW_DISTRACTORS", "Chưa đủ ba phương án nhiễu.", id));
            warn(warnings, blank(word.ipaUk()) && blank(word.ipaUs()), "MISSING_IPA", "Từ chưa có phiên âm.", id);
            warn(warnings, blank(word.audioUkUrl()) && blank(word.audioUsUrl()), "MISSING_AUDIO", "Từ chưa có phát âm.", id);
            warn(warnings, blank(word.imageUrl()), "MISSING_IMAGE", "Từ chưa có ảnh.", id);
            warn(warnings, blank(word.exampleTranslationVi()), "MISSING_EXAMPLE_TRANSLATION", "Câu ví dụ chưa có bản dịch.", id);
            warn(warnings, blank(word.exampleSentence()) || !StudyText.lemmaPattern(word.lemma()).matcher(word.exampleSentence()).find(),
                    "EXAMPLE_WITHOUT_LEMMA", "Câu ví dụ không chứa từ cần học.", id);
        }
        return new Validation(List.copyOf(errors), List.copyOf(warnings));
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void warn(List<Issue> issues, boolean condition, String code, String message, UUID id) {
        if (condition) issues.add(new Issue(code, message, id));
    }
}
