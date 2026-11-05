package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.result.LearnerReportResult.ErrorTrapPoint;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.LessonSummary;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.PaywallGate;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The learner's knowledge tree for one course: every point with the learner's standing on it, what it builds on, and
 * whether free practice is open on it, the chặng it belongs to and the lessons that teach it. Content is asked once for
 * the tree, once for the prerequisites and once for the lessons of each chặng of the course; the profiles come in one
 * query and the unseen-set counts in one call, so the number of points does not multiply the calls.
 */
@Service
public class GetLearnerTreeUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetLearnerTreeUseCase.class);

    private final LearnerCurriculumRepository curricula;
    private final MasteryProfileRepository profiles;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final int reserve;
    private final PaywallGate paywall;
    private final LessonProgressRepository lessonProgress;

    public GetLearnerTreeUseCase(LearnerCurriculumRepository curricula, MasteryProfileRepository profiles,
                                 ReviewItemRepository reviews, PracticeAttemptRepository attempts,
                                 LearningContentClient content,
                                 PaywallGate paywall,
                                 LessonProgressRepository lessonProgress,
                                 @Value("${learning.free-practice.reserve:2}") int reserve) {
        this.curricula = curricula;
        this.profiles = profiles;
        this.reviews = reviews;
        this.attempts = attempts;
        this.content = content;
        this.paywall = paywall;
        this.lessonProgress = lessonProgress;
        this.reserve = reserve;
    }

    @Transactional(readOnly = true)
    public Result execute(UUID userId, UUID courseId) {
        List<Topic> topicSequence = content.getTopicSequence(true);
        if (topicSequence == null) topicSequence = List.of();
        List<Topic> selectedTopics = topicSequence.stream()
                .filter(topic -> topic.course() != null && courseId.equals(topic.course().courseId())).toList();
        if (selectedTopics.isEmpty()) {
            topicSequence = content.getTopicSequence();
            if (topicSequence == null) topicSequence = List.of();
            selectedTopics = topicSequence.stream().filter(topic -> topic.course() != null
                    && courseId.equals(topic.course().courseId())).toList();
        }
        final List<Topic> topics = selectedTopics;
        if (topics.isEmpty()) throw new LearningRequestException(404, "NOT_FOUND", "Course was not found");
        if (!topics.get(0).course().active()) {
            throw new LearningRequestException(403, "COURSE_NOT_OPEN", "The course is closed");
        }
        boolean taking = curricula.find(userId).topics().stream()
                .anyMatch(progress -> topics.stream().anyMatch(topic -> topic.topicId().equals(progress.topicId())));
        if (!taking) throw new LearningRequestException(403, "COURSE_NOT_JOINED", "The learner is not taking this course");

        Map<UUID, KnowledgePoint> points = new LinkedHashMap<>();
        Map<UUID, Topic> topicByPoint = new LinkedHashMap<>();
        for (Topic topic : topics) {
            for (KnowledgePoint point : topic.knowledgePoints()) {
                points.putIfAbsent(point.id(), point);
                topicByPoint.putIfAbsent(point.id(), topic);
            }
        }
        Map<UUID, Integer> topicPosition = positionsInSkill(topics);
        Map<UUID, List<TeachingLesson>> lessonsByPoint = lessonsByPoint(topics, topicPosition);
        List<UUID> ids = points.keySet().stream().sorted(Comparator.comparing(UUID::toString)).toList();
        List<UUID> parentIds = points.values().stream().filter(point -> point.parentKnowledgePointId() == null)
                .map(KnowledgePoint::id).sorted(Comparator.comparing(UUID::toString)).toList();
        Map<UUID, MasteryProfile> standing = profiles.find(userId, ids);
        Map<UUID, List<ErrorTrapPoint>> trapsByParent = new LinkedHashMap<>();
        for (KnowledgePoint point : points.values()) {
            MasteryProfile profile = standing.get(point.id());
            if (point.parentKnowledgePointId() == null || profile == null || profile.evidenceCount() == 0) continue;
            trapsByParent.computeIfAbsent(point.parentKnowledgePointId(), ignored -> new ArrayList<>())
                    .add(new ErrorTrapPoint(point.id(), point.errorCode() == null ? point.code() : point.errorCode(),
                            point.name(), profile.tier().name(),
                            profile.mastery(), profile.evidenceCount()));
        }
        Map<UUID, List<UUID>> prerequisites = content.prerequisites(parentIds);
        if (prerequisites == null) prerequisites = Map.of();
        Map<UUID, Integer> unseen = parentIds.isEmpty() ? Map.of() : content.practiceSetAvailability(parentIds, excluded(userId),
                GetReviewBasketUseCase.MIN_SET_QUESTIONS);
        Set<UUID> grandfatheredPoints = ids.isEmpty() ? Set.of()
                : lessonProgress.findGrandfatheredKnowledgePointIds(userId);

        List<Node> nodes = new ArrayList<>();
        PaywallGate.Request paywallRequest = paywall.forLearner(userId);
        for (UUID id : parentIds) {
            KnowledgePoint point = points.get(id);
            MasteryProfile profile = standing.get(id);
            boolean locked;
            try {
                locked = !paywallRequest.knowledgePointOpen(topicByPoint.get(id), point,
                        grandfatheredPoints.contains(id));
            } catch (AccessUnavailableException unavailable) {
                locked = true;
            }
            boolean practiceAvailable = unseen.getOrDefault(id, 0) > reserve && !locked;
            nodes.add(new Node(id, point.code(), point.name(), point.skill(), point.learningType(),
                    point.cefrLevel(), point.canDo(),
                    profile == null ? null : profile.mastery(),
                    profile == null ? null : profile.tier().name(),
                    profile == null ? 0 : profile.evidenceCount(),
                    profile == null ? null : profile.nextReviewAt(),
                    prerequisites.getOrDefault(id, List.of()),
                    locked, practiceAvailable, trapsByParent.getOrDefault(id, List.of()),
                    teachingTopic(topicByPoint.get(id), topicPosition), lessonsByPoint.getOrDefault(id, List.of())));
        }
        return new Result(courseId, nodes);
    }

    /** The place of each chặng inside its own skill, counted from 1: the number the course page shows on it. */
    private static Map<UUID, Integer> positionsInSkill(List<Topic> topics) {
        Map<String, Integer> counted = new HashMap<>();
        Map<UUID, Integer> positions = new HashMap<>();
        for (Topic topic : topics.stream().sorted(Comparator.comparingInt(Topic::sortOrder)).toList()) {
            String skill = topic.skill() == null ? "OTHER" : topic.skill().name();
            positions.put(topic.topicId(), counted.merge(skill, 1, Integer::sum));
        }
        return positions;
    }

    private static TeachingTopic teachingTopic(Topic topic, Map<UUID, Integer> positions) {
        return topic == null ? null : new TeachingTopic(topic.topicId(), topic.name(), positions.getOrDefault(topic.topicId(), 0));
    }

    /**
     * The lessons of the course that teach each point, in the order of their chặng and then of the lesson. A chặng
     * whose lessons cannot be read is left out: where a point is taught is an extra, and the tree is useful without it.
     */
    private Map<UUID, List<TeachingLesson>> lessonsByPoint(List<Topic> topics, Map<UUID, Integer> topicPosition) {
        Map<UUID, List<TeachingLesson>> byPoint = new LinkedHashMap<>();
        for (Topic topic : topics.stream().sorted(Comparator.comparingInt(Topic::sortOrder)).toList()) {
            List<LessonSummary> lessons;
            try {
                lessons = LessonAccess.orderedLessons(content.getTopicLessons(topic.topicId()));
            } catch (RuntimeException failure) {
                log.warn("Lessons of topic {} could not be read for the knowledge tree: {}", topic.topicId(), failure.toString());
                continue;
            }
            int position = 0;
            for (LessonSummary lesson : lessons) {
                position++;
                for (UUID pointId : lesson.knowledgePointIds()) {
                    byPoint.computeIfAbsent(pointId, ignored -> new ArrayList<>()).add(new TeachingLesson(
                            lesson.lessonId(), lesson.title(), position, topic.topicId(), topic.name(),
                            topicPosition.getOrDefault(topic.topicId(), 0)));
                }
            }
        }
        return byPoint;
    }

    /** Sets the learner has seen or that a review holds; they cannot be offered again. */
    private List<UUID> excluded(UUID userId) {
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        return excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList();
    }

    /**
     * One point of the tree. {@code mastery}, {@code tier} and {@code nextReviewAt} are null, and {@code evidenceCount}
     * zero, until the point has evidence. {@code practiceAvailable} says whether free practice can start on it.
     * {@code topic} is the chặng that owns the point and {@code lessons} are the lessons of the course that teach it; they
     * are what the learner opens to learn it, as opposed to {@code prerequisiteIds}, which only orders the points.
     */
    public record Node(UUID knowledgePointId, String code, String name, String skill, String kind, String cefrLevel,
                       String canDo, Double mastery,
                       String tier, int evidenceCount, Instant nextReviewAt, List<UUID> prerequisiteIds,
                       boolean locked, boolean practiceAvailable, List<ErrorTrapPoint> errorTraps,
                       TeachingTopic topic, List<TeachingLesson> lessons) {
        public Node {
            errorTraps = errorTraps == null ? List.of() : List.copyOf(errorTraps);
            lessons = lessons == null ? List.of() : List.copyOf(lessons);
        }
        public Node(UUID knowledgePointId, String code, String name, String skill, String kind, String cefrLevel,
                    String canDo, Double mastery, String tier, int evidenceCount, Instant nextReviewAt,
                    List<UUID> prerequisiteIds, boolean locked, boolean practiceAvailable,
                    List<ErrorTrapPoint> errorTraps) {
            this(knowledgePointId, code, name, skill, kind, cefrLevel, canDo, mastery, tier, evidenceCount,
                    nextReviewAt, prerequisiteIds, locked, practiceAvailable, errorTraps, null, List.of());
        }
        public Node(UUID knowledgePointId, String code, String name, String skill, String kind, String cefrLevel,
                    String canDo, Double mastery, String tier, int evidenceCount, Instant nextReviewAt,
                    List<UUID> prerequisiteIds, boolean practiceAvailable) {
            this(knowledgePointId, code, name, skill, kind, cefrLevel, canDo, mastery, tier, evidenceCount,
                    nextReviewAt, prerequisiteIds, false, practiceAvailable, List.of(), null, List.of());
        }
        public Node(UUID knowledgePointId, String code, String name, String skill, String kind, String cefrLevel,
                    String canDo, Double mastery, String tier, int evidenceCount, Instant nextReviewAt,
                    List<UUID> prerequisiteIds, boolean practiceAvailable, List<ErrorTrapPoint> errorTraps) {
            this(knowledgePointId, code, name, skill, kind, cefrLevel, canDo, mastery, tier, evidenceCount,
                    nextReviewAt, prerequisiteIds, false, practiceAvailable, errorTraps, null, List.of());
        }
    }

    /** The chặng that owns a point; {@code position} counts from 1 inside its skill, as the course page numbers it. */
    public record TeachingTopic(UUID topicId, String name, int position) {}

    /** A lesson that teaches a point, with its number in its chặng and the chặng it is in. */
    public record TeachingLesson(UUID lessonId, String title, int position, UUID topicId, String topicName,
                                 int topicPosition) {}

    public record Result(UUID courseId, List<Node> knowledgePoints) {}
}
