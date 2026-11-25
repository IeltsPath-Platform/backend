package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.exception.VocabularyRejectedException;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.port.VocabularySenseLookup.SenseText;
import com.ieltspath.content.application.port.VocabularySenseWriter;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.CourseDraftErrorsException;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Issue;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.NewWord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Approves a course draft: it must pass the analysis without errors, and then the course is created in one
 * transaction by {@link CourseMaterializer}. Any failure rolls all of it back and the draft stays {@code READY}.
 * <p>
 * The words the author reviewed get their senses in the library first. The library is another service, so those words
 * stay there if the course then fails to be created; they are reviewed entries, and approving again finds them.
 */
@Service
public class ApproveCourseDraftUseCase {

    static final String VOCABULARY_SENSE_UNAVAILABLE = "VOCABULARY_SENSE_UNAVAILABLE";
    static final String VOCABULARY_REJECTED = "VOCABULARY_REJECTED";
    /** The library answers for at most this many sense ids at once. */
    private static final int SENSE_BATCH = 100;

    private final CourseDraftUseCases draftAccess;
    private final CommitCourseDraftApprovalUseCase commit;
    private final CourseDraftCodec codec;
    private final CourseDraftAnalysisService analysis;
    private final VocabularySenseLookup senses;
    private final VocabularySenseWriter words;

    public ApproveCourseDraftUseCase(CourseDraftUseCases draftAccess, CommitCourseDraftApprovalUseCase commit,
                                     CourseDraftCodec codec, CourseDraftAnalysisService analysis,
                                     VocabularySenseLookup senses, VocabularySenseWriter words) {
        this.draftAccess = draftAccess;
        this.commit = commit;
        this.codec = codec;
        this.analysis = analysis;
        this.senses = senses;
        this.words = words;
    }

    public Approved execute(UUID reviewer, UUID draftId) {
        AiDraft draft = draftAccess.ownReadyDraft(reviewer, draftId);
        CourseDraftStructure plan = codec.read(draft.getResultJson());
        requireSomethingToCreate(plan);
        CourseDraftAnalysisService.Checked checked = analysis.analyze(plan);
        requireNoErrors(checked.analysis());

        requireChosenSensesAvailable(plan);
        Map<String, UUID> newWordSenses = resolveNewWords(plan);

        return commit.execute(reviewer, draftId, draft.getResultJson(), plan, checked,
                newWordSenses, describeChosenSenses(plan));
    }

    /** A sense picked from the library must still be active, or its lesson cannot get a vocabulary block. */
    private void requireChosenSensesAvailable(CourseDraftStructure plan) {
        List<UUID> chosen = plan.lessonsInOrder().stream().flatMap(lesson -> lesson.vocabularySenseIds().stream())
                .distinct().toList();
        Set<UUID> active = new LinkedHashSet<>();
        for (int from = 0; from < chosen.size(); from += SENSE_BATCH) {
            active.addAll(senses.findActiveSenseIds(chosen.subList(from, Math.min(chosen.size(), from + SENSE_BATCH))));
        }
        List<Issue> missing = chosen.stream().filter(id -> !active.contains(id))
                .map(id -> new Issue(VOCABULARY_SENSE_UNAVAILABLE, id.toString(), null)).toList();
        if (!missing.isEmpty()) throw new CourseDraftErrorsException(missing, false);
    }

    /** The word and meaning of every picked sense, which fill the vocabulary table of each lesson's summary. */
    private Map<UUID, SenseText> describeChosenSenses(CourseDraftStructure plan) {
        List<UUID> chosen = plan.lessonsInOrder().stream().flatMap(lesson -> lesson.vocabularySenseIds().stream())
                .distinct().toList();
        Map<UUID, SenseText> described = new LinkedHashMap<>();
        for (int from = 0; from < chosen.size(); from += SENSE_BATCH) {
            senses.describeActiveSenses(chosen.subList(from, Math.min(chosen.size(), from + SENSE_BATCH)))
                    .forEach(text -> described.put(text.senseId(), text));
        }
        return described;
    }

    /** The sense of every proposed word, by lemma; a lemma that several lessons propose is resolved once. */
    private Map<String, UUID> resolveNewWords(CourseDraftStructure plan) {
        Map<String, NewWord> byLemma = new LinkedHashMap<>();
        for (LessonPlan lesson : plan.lessonsInOrder()) {
            lesson.newVocabulary().forEach(word -> byLemma.putIfAbsent(word.normalizedLemma(), word));
        }
        if (byLemma.isEmpty()) return Map.of();
        try {
            return words.resolveOrCreate(List.copyOf(byLemma.values()));
        } catch (VocabularyRejectedException refused) {
            throw new CourseDraftErrorsException(List.of(new Issue(VOCABULARY_REJECTED, refused.getMessage(), null)),
                    false);
        }
    }

    private static void requireSomethingToCreate(CourseDraftStructure plan) {
        List<Issue> missing = new ArrayList<>();
        if (plan.course() == null) missing.add(new Issue("COURSE_MISSING", null, null));
        if (plan.topics().isEmpty()) missing.add(new Issue("TOPIC_MISSING", null, null));
        if (!missing.isEmpty()) throw new CourseDraftErrorsException(missing, false);
    }

    private static void requireNoErrors(CourseDraftAnalysis result) {
        if (result.valid()) return;
        List<Issue> conflicts = result.conflicts();
        if (!conflicts.isEmpty()) throw new CourseDraftErrorsException(conflicts, true);
        throw new CourseDraftErrorsException(result.errors(), false);
    }

    /** What approval created. */
    public record Approved(UUID draftId, UUID courseId, List<UUID> topicIds, List<UUID> lessonIds,
                           List<UUID> finalTestPackageIds) {
    }
}
