package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AuthorPackageReader;
import com.ieltspath.content.application.result.AuthorPackageDetail;
import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAuthorPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;
    private final AuthorPackageReader reader;
    private final MediaReferencePolicy mediaPolicy;

    public GetAuthorPackageUseCase(ContentPackageRepository contentPackageRepository, AuthorPackageReader reader,
                                   MediaReferencePolicy mediaPolicy) {
        this.contentPackageRepository = contentPackageRepository;
        this.reader = reader;
        this.mediaPolicy = mediaPolicy;
    }

    public AuthorPackageDetail execute(UUID packageId) {
        AuthorPackageResult pkg = AuthorPackageResult.of(contentPackageRepository.findById(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId)));
        return new AuthorPackageDetail(pkg,
                reader.extras(packageId).withAudioUrls(reference -> mediaPolicy.resolve(AssetType.AUDIO, reference)));
    }
}
