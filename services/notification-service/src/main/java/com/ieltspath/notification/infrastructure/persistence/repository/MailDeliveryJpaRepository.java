package com.ieltspath.notification.infrastructure.persistence.repository;

import com.ieltspath.notification.infrastructure.persistence.entity.MailDeliveryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MailDeliveryJpaRepository extends JpaRepository<MailDeliveryJpaEntity, UUID> { }
