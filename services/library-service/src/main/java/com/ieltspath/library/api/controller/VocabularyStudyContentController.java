package com.ieltspath.library.api.controller;

import com.ieltspath.library.application.result.StudyContentResult.*;
import com.ieltspath.library.application.usecase.ListTopicStudyUnitsUseCase;
import com.ieltspath.library.application.usecase.GetStudyUnitContentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor @RequestMapping("/api/content/vocabulary")
public class VocabularyStudyContentController {
    private final ListTopicStudyUnitsUseCase list;
    private final GetStudyUnitContentUseCase get;
    @GetMapping("/topics/{topicId}/units") public TopicUnits list(@PathVariable UUID topicId) { return list.execute(topicId); }
    @GetMapping("/units/{unitId}") public UnitContent get(@PathVariable UUID unitId) { return get.execute(unitId); }
}
