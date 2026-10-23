package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Puts all topics of one parent (the roots when there is none) in a new order in one transaction, so an author can
 * move a topic without the intermediate states a swap of two saves would show or refuse.
 *
 * <p>The list must name every sibling exactly once. The group keeps the sort orders it already uses, handed out in
 * the new sequence, so other groups and the gaps between numbers are left alone. A group whose sort orders were not
 * distinct is numbered one after another from its smallest order, which also clears the tie.
 */
@Service
@Transactional
public class ReorderTopicsUseCase {

    private final TopicRepository topics;

    public ReorderTopicsUseCase(TopicRepository topics) {
        this.topics = topics;
    }

    public void execute(UUID parentTopicId, List<UUID> orderedTopicIds) {
        if (orderedTopicIds == null || orderedTopicIds.isEmpty()) {
            throw new IllegalArgumentException("topicIds must name at least one topic");
        }
        if (new HashSet<>(orderedTopicIds).size() != orderedTopicIds.size()) {
            throw new IllegalArgumentException("topicIds must not repeat a topic");
        }
        if (parentTopicId != null) {
            topics.findById(parentTopicId).orElseThrow(() -> new TopicNotFoundException(parentTopicId));
        }

        List<Topic> siblings = parentTopicId == null ? topics.findRootTopics() : topics.findByParentTopicId(parentTopicId);
        Map<UUID, Topic> byId = siblings.stream().collect(Collectors.toMap(Topic::getId, Function.identity()));
        Set<UUID> requested = new HashSet<>(orderedTopicIds);
        if (!requested.equals(byId.keySet())) {
            throw new IllegalArgumentException("topicIds must name every topic of the parent, and only those");
        }

        List<Integer> positions = positions(siblings);
        for (int index = 0; index < orderedTopicIds.size(); index++) {
            Topic topic = byId.get(orderedTopicIds.get(index));
            int position = positions.get(index);
            if (topic.getSortOrder() != position) {
                topic.reorder(position);
                topics.save(topic);
            }
        }
    }

    /** The sort orders to hand out, ascending: the group's own when distinct, else consecutive from the smallest. */
    private static List<Integer> positions(List<Topic> siblings) {
        List<Integer> existing = siblings.stream().map(Topic::getSortOrder).sorted().toList();
        if (new HashSet<>(existing).size() == existing.size()) {
            return existing;
        }
        int first = existing.get(0);
        return java.util.stream.IntStream.range(0, existing.size()).map(offset -> first + offset).boxed().toList();
    }
}
