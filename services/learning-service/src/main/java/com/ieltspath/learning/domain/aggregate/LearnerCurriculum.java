package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.service.TopicStatusDeriver;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.TopicStatus;

import java.time.Instant;
import java.util.*;

/**
 * A learner's ordered topics. Content decides the order; the learner passes topics one way. The first unpassed topic in
 * order is IN_PROGRESS and the rest LOCKED ({@link TopicStatusDeriver}).
 */
public final class LearnerCurriculum {
    private static final TopicStatusDeriver STATUSES = new TopicStatusDeriver();

    private final UUID userId;
    private final Map<UUID, TopicProgress> topics = new LinkedHashMap<>();

    private LearnerCurriculum(UUID userId, Collection<TopicProgress> topics) {
        this.userId = Objects.requireNonNull(userId, "userId");
        topics.forEach(topic -> this.topics.put(topic.topicId(), topic));
    }

    public static LearnerCurriculum restore(UUID userId, Collection<TopicProgress> topics) {
        return new LearnerCurriculum(userId, topics);
    }

    public UUID userId() { return userId; }

    public List<TopicProgress> topics() { return List.copyOf(topics.values()); }

    /** Takes the Content order (1..n in list order). Topics Content no longer lists lose their order but keep a pass. */
    public void reorder(List<TopicPlacement> placements) {
        topics.values().forEach(topic -> topic.order(null));
        for (int index = 0; index < placements.size(); index++) {
            TopicPlacement placement = placements.get(index);
            topics.computeIfAbsent(placement.topicId(), id -> new TopicProgress(id, null, null))
                    .place(index + 1, placement.courseId(), placement.skills(), placement.hasTopicTest());
        }
    }

    public void reorder(Collection<UUID> orderedTopicIds) {
        reorder(orderedTopicIds.stream().map(id -> new TopicPlacement(id, null, Set.<LearningSkill>of(), true)).toList());
    }

    public record TopicPlacement(UUID topicId, UUID courseId, Set<LearningSkill> skills, boolean hasTopicTest) {
        public TopicPlacement {
            skills = skills == null ? Set.of() : Set.copyOf(skills);
        }
        public TopicPlacement(UUID topicId, UUID courseId, LearningSkill skill, boolean hasTopicTest) {
            this(topicId, courseId, skill == null ? Set.<LearningSkill>of() : Set.of(skill), hasTopicTest);
        }
        public TopicPlacement(UUID topicId, LearningSkill skill, boolean hasTopicTest) {
            this(topicId, null, skill, hasTopicTest);
        }
    }

    public Optional<TopicProgress> topic(UUID topicId) { return Optional.ofNullable(topics.get(topicId)); }

    /** True when this call passed the topic; a topic not in the curriculum yet is recorded without an order. */
    public boolean pass(UUID topicId, Instant now) {
        return topics.computeIfAbsent(topicId, id -> new TopicProgress(id, null, null)).pass(now);
    }

    public Map<UUID, TopicStatus> statuses() { return STATUSES.derive(topics()); }

    public TopicStatus status(UUID topicId) { return statuses().getOrDefault(topicId, TopicStatus.LOCKED); }
}
