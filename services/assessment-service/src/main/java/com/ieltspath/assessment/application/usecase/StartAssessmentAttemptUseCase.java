package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.StartAssessmentAttemptCommand;
import com.ieltspath.assessment.application.port.ContentPackageProvider;
import com.ieltspath.assessment.application.port.LearnerEntitlement;
import com.ieltspath.assessment.application.exception.PremiumRequiredException;
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
    private final LearnerEntitlement entitlement;

    public StartAssessmentAttemptUseCase(ContentPackageProvider content, AttemptCreator creator,
                                         LearnerEntitlement entitlement) {
        this.content = content;
        this.creator = creator;
        this.entitlement = entitlement;
    }

    public AssessmentAttemptResult execute(StartAssessmentAttemptCommand command) {
        var packageVersion = content.findPackageVersion(command.packageVersionId())
                .orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy phiên bản gói đề thi."));
        AttemptType type = AttemptType.forContentPackageType(packageVersion.packageType());
        boolean practiceTest = type == AttemptType.OFFICIAL_PRACTICE;
        if (practiceTest && packageVersion.requiredFeatureKey() != null && !entitlement.premium()) {
            throw new PremiumRequiredException();
        }
        List<ContentPackageProvider.Section> allSections = packageVersion.sections();
        List<ContentPackageProvider.Section> selected = allSections;
        if (command.sectionIds() != null && !command.sectionIds().isEmpty()) {
            if (!practiceTest) throw new PackageNotAttemptableException("Chỉ bài luyện tập mới hỗ trợ chọn từng phần thi.");
            if (new HashSet<>(command.sectionIds()).size() != command.sectionIds().size()) {
                throw new PackageNotAttemptableException("Các phần thi được chọn không được trùng lặp.");
            }
            selected = allSections.stream().filter(s -> command.sectionIds().contains(s.sectionId())).toList();
            if (selected.size() != command.sectionIds().size()) {
                throw new PackageNotAttemptableException("Có phần thi được chọn không thuộc phiên bản gói đề này.");
            }
        }
        if (selected.isEmpty()) throw new PackageNotAttemptableException("Phiên bản gói đề thi chưa có phần thi nào.");
        selected = selected.stream().sorted(java.util.Comparator.comparingInt(ContentPackageProvider.Section::sortOrder)).toList();
        AttemptScope scope = practiceTest && selected.size() < allSections.size() ? AttemptScope.PART : AttemptScope.FULL;
        Skill skill = null;
        if (practiceTest) {
            try {
                skill = Skill.valueOf(selected.getFirst().skill());
            } catch (RuntimeException invalidSkill) {
                throw new PackageNotAttemptableException("Phần thi của bài luyện tập phải có kỹ năng.");
            }
            Skill selectedSkill = skill;
            if (selected.stream().anyMatch(s -> !selectedSkill.name().equals(s.skill()))) {
                throw new PackageNotAttemptableException("Các phần thi được chọn phải thuộc cùng một kỹ năng.");
            }
        }
        Instant expiresAt = null;
        // Only practice tests run a section clock; every other package type keeps starting without a deadline.
        if (practiceTest && command.mode() == com.ieltspath.assessment.domain.vo.AttemptMode.TIMED) {
            if (selected.stream().anyMatch(s -> s.timeLimitSeconds() == null || s.timeLimitSeconds() <= 0)) {
                throw new PackageNotAttemptableException("Bài làm tính giờ yêu cầu mỗi phần thi phải có giới hạn thời gian.");
            }
            long seconds = selected.stream().mapToLong(ContentPackageProvider.Section::timeLimitSeconds).sum();
            expiresAt = Instant.now().plusSeconds(seconds);
        }
        return creator.create(command, type, packageVersion, selected, scope, skill, expiresAt,
                type == AttemptType.PLACEMENT ? packageVersion.placementPool() : null);
    }
}
