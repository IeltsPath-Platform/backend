package com.group01.user.domain.aggregate;

import com.group01.user.domain.vo.GoalStatus;
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
        if (targetBand == null || targetBand.compareTo(BigDecimal.ZERO) <= 0 || targetBand.compareTo(BigDecimal.valueOf(9.0)) > 0) {
            throw new IllegalArgumentException("Target band phải nằm trong khoảng (0.0, 9.0]");
        }
        if (availableMinutesPerDay == null || availableMinutesPerDay <= 0) {
            throw new IllegalArgumentException("Thời gian học mỗi ngày phải lớn hơn 0");
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

