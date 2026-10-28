package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.exception.VocabularyRejectedException;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.port.VocabularySenseWriter;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.CourseDraftErrorsException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Issue;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.NewWord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
    private final AiDraftRepository drafts;
    private final CourseDraftCodec codec;
    private final CourseDraftAnalysisService analysis;
    private final CourseMaterializer materializer;
    private final VocabularySenseLookup senses;
    private final VocabularySenseWriter words;
    private final Clock clock;

    public ApproveCourseDraftUseCase(CourseDraftUseCases draftAccess, AiDraftRepository drafts, CourseDraftCodec codec,
                                     CourseDraftAnalysisService analysis, CourseMaterializer materializer,
                                     VocabularySenseLookup senses, VocabularySenseWriter words, Clock clock) {
        this.draftAccess = draftAccess;
        this.drafts = drafts;
        this.codec = codec;
        this.analysis = analysis;
        this.materializer = materializer;
        this.senses = senses;
        this.words = words;
        this.clock = clock;
    }

    @Transactional
    public Approved execute(UUID reviewer, UUID draftId) {
        AiDraft draft = draftAccess.ownReadyDraft(reviewer, draftId);
        CourseDraftStructure plan = codec.read(draft.getResultJson());
        requireSomethingToCreate(plan);
        CourseDraftAnalysisService.Checked checked = analysis.analyze(plan);
        requireNoErrors(checked.analysis());

        requireChosenSensesAvailable(plan);
        Map<String, UUID> newWordSenses = resolveNewWords(plan);

        CourseMaterializer.Created created = materializer.create(plan, checked.facts().resolvedQuestionRefs(),
                newWordSenses);

        draft.approve(reviewer, clock.instant());
        drafts.save(draft);
        return new Approved(draftId, created.courseId(), created.topicIds(), created.lessonIds(),
                created.finalTestPackageIds());
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
