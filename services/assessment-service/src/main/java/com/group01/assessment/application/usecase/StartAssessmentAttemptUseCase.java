package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.port.ContentPackageProvider;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.vo.AttemptType;
import org.springframework.stereotype.Service;

/**
 * Starts an attempt from a published Content package version. Deliberately not transactional: Content is read
 * before a database connection is taken, and {@link AttemptCreator} writes the snapshot in its own transaction.
 */
@Service
public class StartAssessmentAttemptUseCase {
    private final ContentPackageProvider content;
    private final AttemptCreator creator;

    public StartAssessmentAttemptUseCase(ContentPackageProvider content, AttemptCreator creator) {
        this.content = content;
        this.creator = creator;
    }

    public AssessmentAttemptResult execute(StartAssessmentAttemptCommand command) {
        var packageVersion = content.findPackageVersion(command.packageVersionId())
                .orElseThrow(() -> new AssessmentNotFoundException("Package version not found"));
        AttemptType type = AttemptType.forContentPackageType(packageVersion.packageType());
        return creator.create(command, type, packageVersion);
    }
}
