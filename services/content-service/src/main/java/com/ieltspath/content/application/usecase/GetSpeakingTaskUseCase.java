package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.SpeakingTaskReader;
import com.ieltspath.content.application.port.LearnerEntitlement;
import com.ieltspath.content.application.exception.PremiumRequiredException;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;

/** The questions of one Speaking part for the practice workspace; a Speaking question has no answer to hide. */
@Service
public class GetSpeakingTaskUseCase {
    private final SpeakingTaskReader reader;
    private final LearnerEntitlement entitlement;

    @Autowired
    public GetSpeakingTaskUseCase(SpeakingTaskReader reader, LearnerEntitlement entitlement) {
        this.reader = reader;
        this.entitlement = entitlement;
    }

    public GetSpeakingTaskUseCase(SpeakingTaskReader reader) {
        this(reader, () -> false);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SpeakingTaskReader.SpeakingTask execute(UUID packageVersionId, UUID sectionId, boolean learner) {
        SpeakingTaskReader.SpeakingTask task = reader.find(packageVersionId, sectionId)
                .orElseThrow(() -> new PackageVersionNotFoundException(packageVersionId));
        if (learner && task.requiredFeatureKey() != null && !entitlement.premium()) {
            throw new PremiumRequiredException();
        }
        return task;
    }

}
