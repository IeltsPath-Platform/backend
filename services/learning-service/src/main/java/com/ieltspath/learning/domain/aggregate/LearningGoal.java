package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LearningGoal {
    private static final BigDecimal MIN_TARGET_BAND = new BigDecimal("1.0");
    private static final BigDecimal MAX_TARGET_BAND = new BigDecimal("9.0");
    private static final int MIN_MINUTES_PER_DAY = 5;

    private UUID id;
    private UUID userId;
    private BigDecimal targetBand;
    private LocalDate examDate;
    private Integer availableMinutesPerDay;
    private GoalStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateGoal(BigDecimal targetBand, LocalDate examDate, Integer availableMinutesPerDay) {
        if (targetBand == null || targetBand.compareTo(MIN_TARGET_BAND) < 0 || targetBand.compareTo(MAX_TARGET_BAND) > 0) {
            throw new IllegalArgumentException("Target band phải nằm trong khoảng [1.0, 9.0]");
        }
        if (availableMinutesPerDay == null || availableMinutesPerDay < MIN_MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Thời gian học tối thiểu là " + MIN_MINUTES_PER_DAY + " phút mỗi ngày");
        }
        this.targetBand = targetBand;
        this.examDate = examDate;
        this.availableMinutesPerDay = availableMinutesPerDay;
        touch();
    }

    public void complete() {
        if (this.status == GoalStatus.ACHIEVED) {
            return;
        }
        this.status = GoalStatus.ACHIEVED;
        this.endedAt = LocalDateTime.now();
        touch();
    }

    public void abandon() {
        if (this.status == GoalStatus.ABANDONED) {
            return;
        }
        this.status = GoalStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
        touch();
    }

    public void pause() {
        if (this.status != GoalStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể tạm dừng mục tiêu đang hoạt động");
        }
        this.status = GoalStatus.PAUSED;
        touch();
    }

    public void resume() {
        if (this.status != GoalStatus.PAUSED) {
            throw new IllegalStateException("Chỉ có thể tiếp tục mục tiêu đang tạm dừng");
        }
        this.status = GoalStatus.ACTIVE;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}

