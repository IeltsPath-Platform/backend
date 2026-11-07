package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.service.StudyUnitAuthoringService;
import com.ieltspath.library.application.port.StudyUnitRepository.Draft;
import com.ieltspath.library.application.result.StudyContentResult.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReorderStudyUnitsUseCase {
    private final StudyUnitAuthoringService service;
    public List<AuthorUnit> execute(UUID topicId, List<UUID> unitIds) { return service.reorder(topicId, unitIds); }
}
