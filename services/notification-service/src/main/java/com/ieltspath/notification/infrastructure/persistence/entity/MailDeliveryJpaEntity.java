package com.ieltspath.notification.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "mail_deliveries") @Getter @Setter @NoArgsConstructor
public class MailDeliveryJpaEntity {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "sent_at", nullable = false) private Instant sentAt;
}
