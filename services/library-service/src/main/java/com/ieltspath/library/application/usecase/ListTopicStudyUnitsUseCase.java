package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.service.StudyUnitContentService;
import com.ieltspath.library.application.port.StudyUnitRepository.Draft;
import com.ieltspath.library.application.result.StudyContentResult.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ListTopicStudyUnitsUseCase {
    private final StudyUnitContentService service;
    public TopicUnits execute(UUID topicId) { return service.list(topicId); }
}
