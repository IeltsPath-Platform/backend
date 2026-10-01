package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateContentPackageCommand;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.exception.DuplicateCodeException;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.PackageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateContentPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public CreateContentPackageUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public ContentPackageResult execute(CreateContentPackageCommand command) {
        if (command.packageType() == PackageType.TOPIC_TEST) {
            // A final test must belong to a topic; only the curriculum seed creates them.
            throw new IllegalArgumentException("TOPIC_TEST packages cannot be created through this API");
        }
        if (contentPackageRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("ContentPackage", command.code());
        }

        ContentPackage pkg = ContentPackage.create(
                command.code(),
                command.title(),
                command.packageType(),
                command.requiredFeatureKey()
        );

        ContentPackage saved = contentPackageRepository.save(pkg);
        return new ContentPackageResult(
                saved.getId(),
                saved.getCode(),
                saved.getTitle(),
                saved.getPackageType(),
                saved.getRequiredFeatureKey(),
                saved.getStatus(),
                saved.getCurrentPublishedVersionId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
