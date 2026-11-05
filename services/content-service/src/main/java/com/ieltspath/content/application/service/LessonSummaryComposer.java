package com.ieltspath.content.application.service;

import com.ieltspath.content.domain.vo.LessonSummaryMarkdown;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adds the "Vocabulary Core" section to a lesson's Markdown summary from the words the lesson teaches, so the table the
 * learner reads is the vocabulary tab and not a second copy a model wrote. A summary that already has the section keeps
 * it as written, and a lesson with no words is left alone.
 */
final class LessonSummaryComposer {

    static final String HEADING = "## Vocabulary Core";
    private static final Pattern HAS_SECTION = Pattern.compile("(?m)^##\\s+Vocabulary Core\\s*$");

    /** A word of the lesson and its Vietnamese meaning, which may be missing. */
    record Word(String lemma, String meaning) {
    }

    private LessonSummaryComposer() {
    }

    static String withVocabularyCore(String markdown, List<Word> words) {
        if (markdown != null && HAS_SECTION.matcher(markdown).find()) return markdown;
        // One row per word, the first spelling and the first meaning that was given.
        Map<String, Word> rows = new LinkedHashMap<>();
        for (Word word : words) {
            if (word.lemma() == null || word.lemma().isBlank()) continue;
            rows.merge(word.lemma().strip().toLowerCase(Locale.ROOT), word,
                    (kept, extra) -> isBlank(kept.meaning()) ? new Word(kept.lemma(), extra.meaning()) : kept);
        }
        if (rows.isEmpty()) return markdown;

        StringBuilder section = new StringBuilder(HEADING).append("\n\n| Từ | Nghĩa |\n| --- | --- |\n");
        rows.values().forEach(word -> section.append("| ").append(cell(word.lemma())).append(" | ")
                .append(isBlank(word.meaning()) ? "—" : cell(word.meaning())).append(" |\n"));
        String base = markdown == null ? "" : markdown.strip();
        String composed = base.isEmpty() ? section.toString().strip() : base + "\n\n" + section.toString().strip();
        // A summary the author filled to the limit keeps its own text rather than failing the whole approval.
        return composed.length() > LessonSummaryMarkdown.MAX_LENGTH ? markdown : composed;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /** A table cell is one line, and a pipe in it must not start a new column. */
    private static String cell(String text) {
        return text.strip().replaceAll("\\s+", " ").replace("|", "\\|");
    }
}
