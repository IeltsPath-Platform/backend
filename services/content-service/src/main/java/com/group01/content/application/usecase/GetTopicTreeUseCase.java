package com.group01.content.application.usecase;

import com.group01.content.application.result.TopicTreeResult;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetTopicTreeUseCase {

    private final TopicRepository topicRepository;

    public GetTopicTreeUseCase(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public List<TopicTreeResult> execute() {
        List<Topic> allTopics = topicRepository.findAll();

        Map<UUID, List<Topic>> childrenByParent = new HashMap<>();
        List<Topic> roots = new ArrayList<>();

        for (Topic t : allTopics) {
            if (t.getParentTopicId() == null) {
                roots.add(t);
            } else {
                childrenByParent.computeIfAbsent(t.getParentTopicId(), k -> new ArrayList<>()).add(t);
            }
        }

        roots.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

        List<TopicTreeResult> result = new ArrayList<>();
        for (Topic root : roots) {
            result.add(buildNode(root, childrenByParent));
        }
        return result;
    }

    private TopicTreeResult buildNode(Topic current, Map<UUID, List<Topic>> childrenByParent) {
        List<Topic> children = childrenByParent.getOrDefault(current.getId(), List.of());
        List<TopicTreeResult> childNodes = new ArrayList<>();
        for (Topic child : children) {
            childNodes.add(buildNode(child, childrenByParent));
        }
        childNodes.sort((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()));

        return new TopicTreeResult(
                current.getId(),
                current.getParentTopicId(),
                current.getCode(),
                current.getName(),
                current.getSortOrder(),
                current.getStatus(),
                current.getCreatedAt(),
                current.getUpdatedAt(),
                childNodes
        );
    }
}

