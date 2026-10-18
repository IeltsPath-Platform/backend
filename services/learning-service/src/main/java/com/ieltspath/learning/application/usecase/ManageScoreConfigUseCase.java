package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.ScoreConfigResult;
import com.ieltspath.learning.domain.repository.CourseScoreConfigRepository;
import com.ieltspath.learning.domain.vo.CourseScoreConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Admin view and update of the score weights and thresholds. A change applies to scores that are not decided yet; a
 * course already passed keeps its decision.
 */
@Service
public class ManageScoreConfigUseCase {
    private final CourseScoreConfigRepository configs;

    public ManageScoreConfigUseCase(CourseScoreConfigRepository configs) {
        this.configs = configs;
    }

    /** The configuration of the course, or the default when {@code courseId} is null. */
    @Transactional(readOnly = true)
    public ScoreConfigResult get(UUID courseId) {
        return configs.find(courseId).map(config -> result(config, false))
                .orElseGet(() -> result(configs.effective(courseId).forCourse(courseId), true));
    }

    @Transactional
    public ScoreConfigResult update(UUID adminId, UUID courseId, BigDecimal processWeight, BigDecimal midWeight,
                                    BigDecimal finalWeight, BigDecimal practiceShare, BigDecimal practiceTestShare,
                                    BigDecimal passThreshold, BigDecimal midRemediationBelow) {
        CourseScoreConfig config;
        try {
            config = new CourseScoreConfig(courseId, processWeight, midWeight, finalWeight, practiceShare,
                    practiceTestShare, passThreshold, midRemediationBelow);
        } catch (IllegalArgumentException exception) {
            throw new LearningRequestException(400, "INVALID_SCORE_CONFIG", exception.getMessage());
        }
        configs.save(config, adminId);
        return result(config, false);
    }

    private static ScoreConfigResult result(CourseScoreConfig config, boolean inherited) {
        return new ScoreConfigResult(config.courseId(), inherited, config.processWeight(), config.midWeight(),
                config.finalWeight(), config.practiceShare(), config.practiceTestShare(), config.passThreshold(),
                config.midRemediationBelow());
    }
}
