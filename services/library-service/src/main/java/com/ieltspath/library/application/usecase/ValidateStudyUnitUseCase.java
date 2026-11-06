package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.service.StudyUnitAuthoringService;
import com.ieltspath.library.application.port.StudyUnitRepository.Draft;
import com.ieltspath.library.application.result.StudyContentResult.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ValidateStudyUnitUseCase {
    private final StudyUnitAuthoringService service;
    public Validation execute(UUID unitId) { return service.validate(unitId); }
}
