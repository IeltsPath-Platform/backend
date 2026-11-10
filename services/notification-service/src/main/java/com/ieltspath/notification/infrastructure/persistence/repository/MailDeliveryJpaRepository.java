package com.ieltspath.notification.infrastructure.persistence.repository;

import com.ieltspath.notification.infrastructure.persistence.entity.MailDeliveryId;
import com.ieltspath.notification.infrastructure.persistence.entity.MailDeliveryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailDeliveryJpaRepository extends JpaRepository<MailDeliveryJpaEntity, MailDeliveryId> { }
