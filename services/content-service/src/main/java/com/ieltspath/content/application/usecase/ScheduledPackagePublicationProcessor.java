package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.PublishContentPackageCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ScheduledPackagePublicationProcessor {
    private final PublishContentPackageUseCase publish;

    public ScheduledPackagePublicationProcessor(PublishContentPackageUseCase publish) {
        this.publish = publish;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(UUID packageId, UUID versionId) {
        publish.execute(new PublishContentPackageCommand(packageId, versionId, null));
    }
}
