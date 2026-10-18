package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.CourseScoreResult;
import com.ieltspath.learning.application.service.CourseMilestones;
import com.ieltspath.learning.application.service.CourseScoreService;
import com.ieltspath.learning.domain.repository.CourseOutcomeRepository;
import com.ieltspath.learning.domain.repository.CourseScoreConfigRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.RemediationFlagRepository;
import com.ieltspath.learning.domain.vo.CourseOutcome;
import com.ieltspath.learning.domain.vo.CourseScoreConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The learner's score in one course: components, weights, total, threshold, decision and remediation. */
@Service
public class GetCourseScoreUseCase {
    private final LearnerCurriculumRepository curricula;
    private final CourseScoreConfigRepository configs;
    private final CourseScoreService scores;
    private final CourseMilestones milestones;
    private final CourseOutcomeRepository outcomes;
    private final RemediationFlagRepository flags;

    public GetCourseScoreUseCase(LearnerCurriculumRepository curricula, CourseScoreConfigRepository configs,
                                 CourseScoreService scores, CourseOutcomeRepository outcomes,
                                 RemediationFlagRepository flags, CourseMilestones milestones) {
        this.milestones = milestones;
        this.curricula = curricula;
        this.configs = configs;
        this.scores = scores;
        this.outcomes = outcomes;
        this.flags = flags;
    }

    @Transactional(readOnly = true)
    public CourseScoreResult execute(UUID userId, UUID courseId) {
        boolean known = curricula.find(userId).topics().stream().anyMatch(topic -> courseId.equals(topic.courseId()));
        if (!known) throw new LearningRequestException(404, "COURSE_NOT_FOUND", "Course was not found");
        CourseScoreConfig config = configs.effective(courseId);
        var evaluation = scores.evaluate(userId, courseId, config, milestones.countForRequest(courseId));
        var score = evaluation.score();
        var outcome = outcomes.find(userId, courseId);

        // A passed course keeps the decision it was passed with; everything else follows the current configuration.
        boolean decided = outcome.filter(value -> value.status() == CourseOutcome.Status.PASSED).isPresent();
        Map<String, Double> stored = decided ? outcome.get().components() : null;
        var components = decided
                ? new CourseScoreResult.Components(stored.get("practice"), stored.get("practiceTest"),
                        stored.get("process"), stored.get("mid"), stored.get("final"))
                : new CourseScoreResult.Components(evaluation.components().practice(),
                        evaluation.components().practiceTest(), score.process(), score.mid(), score.finalTest());
        String status = outcome.map(value -> value.status() == CourseOutcome.Status.PASSED ? "PASSED"
                        : value.retestReady() ? "RETEST_READY" : "REMEDIATION")
                .orElse("IN_PROGRESS");
        List<CourseScoreResult.Flag> remediation = flags.findByCourse(userId, courseId).stream()
                .map(flag -> new CourseScoreResult.Flag(flag.kind().name(), flag.topicId(), flag.open(),
                        flag.createdAt())).toList();
        // A decided course shows the weights its decision used, so total, components and weights agree.
        var weights = decided && stored.containsKey(CourseScoreService.WEIGHT_PROCESS)
                ? new CourseScoreResult.Weights(stored.get(CourseScoreService.WEIGHT_PROCESS),
                        stored.get(CourseScoreService.WEIGHT_MID), stored.get(CourseScoreService.WEIGHT_FINAL),
                        stored.get(CourseScoreService.SHARE_PRACTICE), stored.get(CourseScoreService.SHARE_PRACTICE_TEST))
                : new CourseScoreResult.Weights(score.processWeight(), score.midWeight(), score.finalWeight(),
                        score.practiceShare(), score.practiceTestShare());
        return new CourseScoreResult(courseId, status, components, weights,
                decided ? outcome.get().total() : score.total(), config.passThreshold().doubleValue(),
                config.midRemediationBelow().doubleValue(),
                outcome.map(CourseOutcome::topErrorCodes).orElse(List.of()), remediation,
                outcome.map(CourseOutcome::decidedAt).orElse(null));
    }
}
