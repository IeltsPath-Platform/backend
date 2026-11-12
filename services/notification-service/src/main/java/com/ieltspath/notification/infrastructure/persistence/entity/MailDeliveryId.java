package com.ieltspath.notification.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/** Composite key of {@code mail_deliveries}: one row per source event and template. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailDeliveryId implements Serializable {
    private UUID eventId;
    private String template;
}
