package com.group01.user.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLearningGoalRequest(
        @NotNull(message = "Mục tiêu band điểm không được để trống")
        @DecimalMin(value = "1.0", message = "Mục tiêu band điểm tối thiểu là 1.0")
        @DecimalMax(value = "9.0", message = "Mục tiêu band điểm tối đa là 9.0")
        BigDecimal targetBand,

        @Future(message = "Ngày thi phải là ngày trong tương lai")
        LocalDate examDate,

        @NotNull(message = "Thời gian học mỗi ngày không được để trống")
        @Min(value = 5, message = "Thời gian học tối thiểu là 5 phút mỗi ngày")
        Integer availableMinutesPerDay
) {
}

