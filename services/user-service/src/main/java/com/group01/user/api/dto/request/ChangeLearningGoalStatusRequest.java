package com.group01.user.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeLearningGoalStatusRequest(
        @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(regexp = "ACTIVE|PAUSED|ACHIEVED|ABANDONED", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Trạng thái phải là ACTIVE, PAUSED, ACHIEVED hoặc ABANDONED")
        String status
) {
}

