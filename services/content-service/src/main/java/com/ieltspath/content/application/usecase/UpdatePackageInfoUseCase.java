package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdatePackageInfoCommand;
import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.TestSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Changes the title, access level, source or band pool of a package; the code and the type never change. */
@Service
@Transactional
public class UpdatePackageInfoUseCase {

    private final ContentPackageRepository packages;

    public UpdatePackageInfoUseCase(ContentPackageRepository packages) {
        this.packages = packages;
    }

    public AuthorPackageResult execute(UpdatePackageInfoCommand command) {
        ContentPackage pkg = packages.findByIdForUpdate(command.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(command.packageId()));
        if (command.title() != null) {
            pkg.rename(command.title());
        }
        if (command.accessChange()) {
            pkg.changeRequiredFeatureKey(command.requiredFeatureKey());
        }
        if (command.sourceCode() != null) {
            pkg.changeSource(command.sourceCode().isBlank() ? null : validSource(command.sourceCode().strip()));
        }
        if (command.placementPool() != null) {
            pkg.changePlacementPool(command.placementPool());
        }
        return AuthorPackageResult.of(packages.save(pkg));
    }

    private static String validSource(String sourceCode) {
        try {
            return TestSource.valueOf(sourceCode).name();
        } catch (RuntimeException invalidSource) {
            throw new IllegalArgumentException("Nguồn đề không được hỗ trợ");
        }
    }
}
