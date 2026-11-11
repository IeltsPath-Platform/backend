package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.application.MailDeliveryRepository;
import com.ieltspath.notification.infrastructure.persistence.entity.MailDeliveryId;
import com.ieltspath.notification.infrastructure.persistence.entity.MailDeliveryJpaEntity;
import com.ieltspath.notification.infrastructure.persistence.repository.MailDeliveryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MailDeliveryRepositoryAdapter implements MailDeliveryRepository {
    private final MailDeliveryJpaRepository repository;
    private final JdbcTemplate jdbc;

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

    @Override
    @Transactional
    public boolean reserveDailyNonSecuritySlot(UUID userId, LocalDate date, UUID eventId, String template, Instant reservedAt) {
        return jdbc.update("""
                INSERT INTO mail_daily_cap_reservations (user_id, sent_date, event_id, template, slot, reserved_at)
                SELECT ?, ?, ?, ?, slots.slot, ? FROM generate_series(1, 3) AS slots(slot)
                WHERE NOT EXISTS (
                    SELECT 1 FROM mail_daily_cap_reservations r
                    WHERE r.user_id = ? AND r.sent_date = ? AND r.slot = slots.slot
                )
                ORDER BY slots.slot LIMIT 1
                ON CONFLICT DO NOTHING
                """, userId, date, eventId, template, reservedAt, userId, date) > 0;
    }

    @Override
    @Transactional
    public void releaseDailyNonSecuritySlot(UUID userId, LocalDate date, UUID eventId, String template) {
        jdbc.update("DELETE FROM mail_daily_cap_reservations WHERE user_id=? AND sent_date=? AND event_id=? AND template=?",
                userId, date, eventId, template);
    }
}
