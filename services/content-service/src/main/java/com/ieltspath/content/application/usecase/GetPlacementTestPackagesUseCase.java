package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.TopicTestPackageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetPlacementTestPackagesUseCase {
    private final LearningContentReader reader;

    public GetPlacementTestPackagesUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public List<TopicTestPackageResult> execute() {
        return reader.placementTestPackages();
    }
}
