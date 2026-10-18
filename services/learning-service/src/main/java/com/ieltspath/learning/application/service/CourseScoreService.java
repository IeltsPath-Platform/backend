package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.CourseScoreData;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.service.CourseScoreCalculator;
import com.ieltspath.learning.domain.service.CourseScoreCalculator.Components;
import com.ieltspath.learning.domain.service.CourseScoreCalculator.Score;
import com.ieltspath.learning.domain.vo.CourseScoreConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The learner's score in a course as it stands: Practice, milestone practice tests, topic tests (mid-course) and the
 * newest course test, put together by the configuration.
 */
@Service
public class CourseScoreService {
    public static final String WEIGHT_PROCESS = "weightProcess";
    public static final String WEIGHT_MID = "weightMid";
    public static final String WEIGHT_FINAL = "weightFinal";
    public static final String SHARE_PRACTICE = "sharePractice";
    public static final String SHARE_PRACTICE_TEST = "sharePracticeTest";

    private final CourseScoreData data;
    private final LearnerCurriculumRepository curricula;
    private final CourseMilestones milestones;
    private final CourseScoreCalculator calculator = new CourseScoreCalculator();

    public CourseScoreService(CourseScoreData data, LearnerCurriculumRepository curricula,
                              CourseMilestones milestones) {
        this.data = data;
        this.curricula = curricula;
        this.milestones = milestones;
    }

    public Evaluation evaluate(UUID userId, UUID courseId, CourseScoreConfig config) {
        // Practice and the course test exist in every course; a missing result is a 0, not a missing component.
        Double practice = data.practiceAverage(userId, courseId).orElse(0.0);

        int milestoneCount = milestones.count(courseId);
        List<Double> finished = data.practiceTestPercents(userId, courseId);
        Double practiceTest = milestoneCount == 0 ? null
                : finished.stream().mapToDouble(Double::doubleValue).sum() / Math.max(milestoneCount, finished.size());

        List<TopicProgress> tested = curricula.find(userId).topics().stream()
                .filter(topic -> courseId.equals(topic.courseId()) && topic.sequenceOrder() != null
                        && topic.hasTopicTest()).toList();
        Double mid = tested.isEmpty() ? null : tested.stream()
                .mapToDouble(topic -> topic.firstTestPercent() == null ? 0 : topic.firstTestPercent() / 100.0)
                .average().orElse(0);

        Optional<CourseScoreData.FinalTest> latest = data.latestFinal(userId, courseId);
        Double finalTest = latest.map(CourseScoreData.FinalTest::percent).orElse(0.0);

        Components components = new Components(practice, practiceTest, mid, finalTest);
        return new Evaluation(components, calculator.calculate(config, components), latest);
    }

    public boolean passes(CourseScoreConfig config, double total) {
        return calculator.passes(config, total);
    }

    /** The components in the form the course outcome stores them. */
    public static Map<String, Double> asMap(Evaluation evaluation) {
        Map<String, Double> map = new LinkedHashMap<>();
        Components components = evaluation.components();
        Score score = evaluation.score();
        if (components.practice() != null) map.put("practice", components.practice());
        if (components.practiceTest() != null) map.put("practiceTest", components.practiceTest());
        if (score.process() != null) map.put("process", score.process());
        if (score.mid() != null) map.put("mid", score.mid());
        if (score.finalTest() != null) map.put("final", score.finalTest());
        // The weights and threshold the decision used, so a decided course never mixes them with a later configuration.
        map.put(WEIGHT_PROCESS, score.processWeight());
        map.put(WEIGHT_MID, score.midWeight());
        map.put(WEIGHT_FINAL, score.finalWeight());
        map.put(SHARE_PRACTICE, score.practiceShare());
        map.put(SHARE_PRACTICE_TEST, score.practiceTestShare());
        return map;
    }

    public record Evaluation(Components components, Score score, Optional<CourseScoreData.FinalTest> latestFinal) {}
}
