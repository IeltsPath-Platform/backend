package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import com.ieltspath.content.domain.vo.LessonResourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReorderLessonResourcesUseCase {

    private final LessonResourceRepository resources;

    public ReorderLessonResourcesUseCase(LessonResourceRepository resources) {
        this.resources = resources;
    }

    /**
     * Gives the resources of one type the orders 0..n-1 in the given sequence. The ids must be exactly the lesson's
     * resources of that type, each once. The other type is not touched.
     */
    public void execute(UUID lessonId, LessonResourceType type, List<UUID> resourceIds) {
        Map<UUID, LessonResource> ofType = resources.findByLessonId(lessonId).stream()
                .filter(resource -> resource.getResourceType() == type)
                .collect(Collectors.toMap(LessonResource::getId, Function.identity()));
        if (resourceIds == null || resourceIds.size() != ofType.size()
                || !new HashSet<>(resourceIds).equals(ofType.keySet())) {
            throw new IllegalArgumentException("resourceIds must list every " + type + " of the lesson exactly once");
        }

        resources.deferSortOrderCheck();
        int order = 0;
        for (UUID id : resourceIds) {
            LessonResource resource = ofType.get(id);
            resource.changeSortOrder(order++);
            resources.save(resource);
        }
    }
}
