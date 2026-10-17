package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.StartAssessmentAttemptCommand;
import com.ieltspath.assessment.application.port.ContentPackageProvider;
import com.ieltspath.assessment.application.result.AssessmentAttemptResult;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.vo.AttemptType;
import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.assessment.domain.exception.PackageNotAttemptableException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
        boolean practiceTest = type == AttemptType.OFFICIAL_PRACTICE;
        List<ContentPackageProvider.Section> allSections = packageVersion.sections();
        List<ContentPackageProvider.Section> selected = allSections;
        if (command.sectionIds() != null && !command.sectionIds().isEmpty()) {
            if (!practiceTest) throw new PackageNotAttemptableException("Sections can only be selected for practice tests");
            if (new HashSet<>(command.sectionIds()).size() != command.sectionIds().size()) {
                throw new PackageNotAttemptableException("Selected sections must be unique");
            }
            selected = allSections.stream().filter(s -> command.sectionIds().contains(s.sectionId())).toList();
            if (selected.size() != command.sectionIds().size()) {
                throw new PackageNotAttemptableException("A selected section does not belong to this package version");
            }
        }
        if (selected.isEmpty()) throw new PackageNotAttemptableException("Package version has no sections");
        selected = selected.stream().sorted(java.util.Comparator.comparingInt(ContentPackageProvider.Section::sortOrder)).toList();
        AttemptScope scope = practiceTest && selected.size() < allSections.size() ? AttemptScope.PART : AttemptScope.FULL;
        Skill skill = null;
        if (practiceTest) {
            try {
                skill = Skill.valueOf(selected.getFirst().skill());
            } catch (RuntimeException invalidSkill) {
                throw new PackageNotAttemptableException("Practice test section must have a skill");
            }
            Skill selectedSkill = skill;
            if (selected.stream().anyMatch(s -> !selectedSkill.name().equals(s.skill()))) {
                throw new PackageNotAttemptableException("All selected sections must use the same skill");
            }
        }
        Instant expiresAt = null;
        // Only practice tests run a section clock; every other package type keeps starting without a deadline.
        if (practiceTest && command.mode() == com.ieltspath.assessment.domain.vo.AttemptMode.TIMED) {
            if (selected.stream().anyMatch(s -> s.timeLimitSeconds() == null || s.timeLimitSeconds() <= 0)) {
                throw new PackageNotAttemptableException("Timed attempts require a time limit on every section");
            }
            long seconds = selected.stream().mapToLong(ContentPackageProvider.Section::timeLimitSeconds).sum();
            expiresAt = Instant.now().plusSeconds(seconds);
        }
        return creator.create(command, type, packageVersion, selected, scope, skill, expiresAt);
    }
}
