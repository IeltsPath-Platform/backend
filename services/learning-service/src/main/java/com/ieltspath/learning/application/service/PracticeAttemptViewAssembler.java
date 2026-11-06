package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PracticeAttemptViewAssembler {
    private final WritingSubmissionRepository essays;
    private final SpeakingSubmissionRepository speaking;
    private final SpeakingPracticeSettings speakingSettings;

    public PracticeAttemptViewAssembler(WritingSubmissionRepository essays, SpeakingSubmissionRepository speaking,
                                        SpeakingPracticeSettings speakingSettings) {
        this.essays = essays;
        this.speaking = speaking;
        this.speakingSettings = speakingSettings;
    }

    public PracticeAttemptView assemble(PracticeAttempt attempt, PackageVersion version) {
        List<Section> ordered = ItemGrading.orderedSections(version);
        boolean hasEssays = ordered.stream().flatMap(section -> section.items().stream()).anyMatch(ItemGrading::isEssay);
        Map<UUID, WritingSubmission> latest = hasEssays ? essays.latestForPractice(attempt.userId(), attempt.id())
                : Map.of();
        boolean hasRecorded = ordered.stream().flatMap(section -> section.items().stream())
                .anyMatch(SpeakingTestQuestions::isRecordedAnswer);
        SpeakingSubmission recordedAnswer = hasRecorded
                ? speaking.findLatestForAttempt(attempt.userId(), attempt.id()).orElse(null) : null;
        List<PracticeAttemptView.Section> sections = ordered.stream().map(section -> {
            List<Item> items = section.items().stream().sorted(Comparator.comparingInt(Item::sortOrder)
                    .thenComparing(item -> item.questionVersionId().toString())).toList();
            return new PracticeAttemptView.Section(section.sectionId(), section.title(), section.skill(),
                    section.passage(), audio(section),
                    items.stream().filter(item -> !ItemGrading.isOpenAnswer(item)).map(PracticeAttemptViewAssembler::question)
                            .toList(),
                    items.stream().filter(ItemGrading::isEssay).map(item -> essay(item,
                            latest.get(item.questionVersionId()))).toList(),
                    items.stream().filter(SpeakingTestQuestions::isRecordedAnswer)
                            .map(item -> recorded(item, recordedAnswer)).toList());
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

    /**
     * A question before its answer carries no hint: the hint is revealed on the submission result, after a wrong answer
     * (see {@link com.ieltspath.learning.domain.service.LessonHintPolicy}).
     */
    private static LessonResult.Question question(Item item) {
        return new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                item.options() == null ? null : item.options().stream().map(option -> new LessonResult.Option(
                        option.optionKey(), option.content(), option.sortOrder())).toList(), null);
    }

    /** The band appears only once the answer is GRADED, as for essays; it passes at the item's band or the practice default. */
    private PracticeAttemptView.Recorded recorded(Item item, SpeakingSubmission submission) {
        Map<String, Object> spec = item.answerSpec();
        BigDecimal passBand = spec.get("passBand") instanceof Number band ? new BigDecimal(band.toString())
                : BigDecimal.valueOf(speakingSettings.passBand());
        boolean graded = submission != null && submission.status() == SpeakingSubmissionStatus.GRADED;
        BigDecimal band = graded ? SpeakingPracticeBand.of(submission) : null;
        LessonResult.LatestSubmission latest = submission == null ? null : new LessonResult.LatestSubmission(
                submission.id(), submission.status().name(), band, band != null ? band.compareTo(passBand) >= 0 : null);
        return new PracticeAttemptView.Recorded(item.questionVersionId(), item.sortOrder(), item.stem(),
                spec.get("durationSeconds") instanceof Number seconds ? seconds.intValue() : null, passBand, latest);
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
                spec.get("passBand") instanceof Number band ? new BigDecimal(band.toString()) : null, latest,
                item.guide(), item.images().stream()
                        .map(image -> new LessonResult.Image(image.mediaUrl(), image.altText())).toList(),
                item.explanation());
    }
}
