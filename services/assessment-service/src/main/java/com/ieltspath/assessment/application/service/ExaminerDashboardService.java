package com.ieltspath.assessment.application.service;

import com.ieltspath.assessment.application.exception.UserServiceUnavailableException;
import com.ieltspath.assessment.application.port.ExaminerAssignmentStore;
import com.ieltspath.assessment.application.port.ExaminerDashboardStore;
import com.ieltspath.assessment.application.port.ExaminerDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/**
 * What an examiner sees at a glance: their queue by state, how many submissions they took today against their daily
 * limit, and how many they finished today and this week. Days and weeks are counted in the same time zone as the daily
 * limit (a week starts on Monday).
 */
@Service
public class ExaminerDashboardService {
    private final ExaminerDashboardStore dashboard;
    private final ExaminerAssignmentStore assignments;
    private final ExaminerDirectory directory;
    private final ZoneId zone;

    public ExaminerDashboardService(ExaminerDashboardStore dashboard, ExaminerAssignmentStore assignments,
                                    ExaminerDirectory directory,
                                    @Value("${assessment.examiner.day-timezone:Asia/Ho_Chi_Minh}") String timezone) {
        this.dashboard = dashboard;
        this.assignments = assignments;
        this.directory = directory;
        this.zone = ZoneId.of(timezone);
    }

    /**
     * @param dailyLimit how many new submissions the examiner takes a day; null when User Service cannot be reached, so the
     *                   rest of the dashboard still shows
     */
    public record Summary(int queued, int processing, int dueSoon, int overdue, int completedToday, int completedThisWeek,
                          int receivedToday, Integer dailyLimit) {}

    @Transactional(readOnly = true)
    public Summary summary(UUID examinerId) {
        LocalDate today = LocalDate.now(zone);
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant();
        Instant now = Instant.now();
        var counts = dashboard.counts(examinerId, now.minus(ExaminerAssignmentPolicy.REMINDER_AFTER),
                now.minus(ExaminerAssignmentPolicy.DEADLINE), todayStart, weekStart);
        return new Summary(counts.queued(), counts.processing(), counts.dueSoon(), counts.overdue(), counts.completedToday(),
                counts.completedThisWeek(), assignments.receivedSince(examinerId, todayStart), dailyLimit(examinerId));
    }

    private Integer dailyLimit(UUID examinerId) {
        try {
            return directory.find(examinerId).map(ExaminerDirectory.Examiner::dailyLimit).orElse(null);
        } catch (UserServiceUnavailableException unavailable) {
            return null;
        }
    }
}
