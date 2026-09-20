package com.group01.user.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateLearnerProfileRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
        String displayName,

        @Size(max = 500, message = "Đường dẫn ảnh đại diện không hợp lệ")
        String avatarReference,

        @Size(max = 1000, message = "Tiểu sử không được vượt quá 1000 ký tự")
        String bio,

        @DecimalMin(value = "0.0", message = "Band điểm phải từ 0.0 trở lên")
        @DecimalMax(value = "9.0", message = "Band điểm tối đa là 9.0")
        BigDecimal selfReportedBand,

        @Size(max = 50, message = "Múi giờ không được vượt quá 50 ký tự")
        String timezone,

        @Pattern(regexp = "PUBLIC|PRIVATE", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Quyền hiển thị phải là PUBLIC hoặc PRIVATE")
        String visibility
) {
}

