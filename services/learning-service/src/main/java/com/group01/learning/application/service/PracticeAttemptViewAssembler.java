package com.group01.learning.application.service;

import com.group01.learning.application.port.LearningContentClient.Item;
import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.port.LearningContentClient.Section;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.PracticeAttemptView;
import com.group01.learning.domain.aggregate.PracticeAttempt;
import com.group01.learning.domain.aggregate.WritingSubmission;
import com.group01.learning.domain.repository.WritingSubmissionRepository;
import com.group01.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PracticeAttemptViewAssembler {
    private final WritingSubmissionRepository essays;

    public PracticeAttemptViewAssembler(WritingSubmissionRepository essays) {
        this.essays = essays;
    }

    public PracticeAttemptView assemble(PracticeAttempt attempt, PackageVersion version) {
        List<Section> ordered = ItemGrading.orderedSections(version);
        boolean hasEssays = ordered.stream().flatMap(section -> section.items().stream()).anyMatch(ItemGrading::isEssay);
        Map<UUID, WritingSubmission> latest = hasEssays ? essays.latestForPractice(attempt.userId(), attempt.id())
                : Map.of();
        List<PracticeAttemptView.Section> sections = ordered.stream().map(section -> {
            List<Item> items = section.items().stream().sorted(Comparator.comparingInt(Item::sortOrder)
                    .thenComparing(item -> item.questionVersionId().toString())).toList();
            return new PracticeAttemptView.Section(section.sectionId(), section.title(), section.skill(),
                    section.passage(), audio(section),
                    items.stream().filter(item -> !ItemGrading.isEssay(item)).map(PracticeAttemptViewAssembler::question)
                            .toList(),
                    items.stream().filter(ItemGrading::isEssay).map(item -> essay(item,
                            latest.get(item.questionVersionId()))).toList());
        }).toList();
        var first = sections.isEmpty() ? null : sections.getFirst();
        return new PracticeAttemptView(attempt.id(), attempt.packageId(), attempt.packageVersionId(),
                first == null ? null : first.passage(), first == null ? null : first.audio(),
                sections.stream().flatMap(section -> section.questions().stream()).toList(), sections);
    }

    private static PracticeAttemptView.Audio audio(Section section) {
        return section.audio() == null ? null
                : new PracticeAttemptView.Audio(section.audio().mediaUrl(), section.audio().durationSeconds());
    }

    private static LessonResult.Question question(Item item) {
        return new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                item.options() == null ? null : item.options().stream().map(option -> new LessonResult.Option(
                        option.optionKey(), option.content(), option.sortOrder())).toList(), item.hint());
    }

    /** The newest submission's grade appears only once it is GRADED, as for lesson essays. */
    private static PracticeAttemptView.Essay essay(Item item, WritingSubmission submission) {
        Map<String, Object> spec = item.answerSpec();
        LessonResult.LatestSubmission latest = submission == null ? null : new LessonResult.LatestSubmission(
                submission.id(), submission.status().name(),
                submission.status() == WritingSubmissionStatus.GRADED ? submission.overallBand() : null,
                submission.status() == WritingSubmissionStatus.GRADED ? submission.passed() : null);
        return new PracticeAttemptView.Essay(item.questionVersionId(), item.sortOrder(), item.stem(),
                spec.get("task") instanceof String task ? task : null,
                spec.get("minWords") instanceof Number words ? words.intValue() : null,
                spec.get("passBand") instanceof Number band ? new BigDecimal(band.toString()) : null, latest);
    }
}
