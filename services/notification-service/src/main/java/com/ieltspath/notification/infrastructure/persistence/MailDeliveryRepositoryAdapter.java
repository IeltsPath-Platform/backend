package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.application.MailDeliveryRepository;
import com.ieltspath.notification.infrastructure.persistence.entity.MailDeliveryId;
import com.ieltspath.notification.infrastructure.persistence.entity.MailDeliveryJpaEntity;
import com.ieltspath.notification.infrastructure.persistence.repository.MailDeliveryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MailDeliveryRepositoryAdapter implements MailDeliveryRepository {
    private final MailDeliveryJpaRepository repository;

    @Override
    public boolean exists(UUID eventId, String template) {
        return repository.existsById(new MailDeliveryId(eventId, template));
    }

    @Override
    @Transactional
    public void recordSent(UUID eventId, String template, Instant sentAt) {
        MailDeliveryJpaEntity row = new MailDeliveryJpaEntity();
        row.setEventId(eventId);
        row.setTemplate(template);
        row.setSentAt(sentAt);
        repository.save(row);
    }
}
