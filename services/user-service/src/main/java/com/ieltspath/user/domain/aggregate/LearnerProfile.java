package com.ieltspath.user.domain.aggregate;

import com.ieltspath.user.domain.vo.ProfileVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LearnerProfile {
    private UUID userId;
    private String displayName;
    private String avatarReference;
    private String bio;
    private BigDecimal selfReportedBand;
    private String timezone;
    private ProfileVisibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateDetails(String displayName, String avatarReference, String bio, BigDecimal selfReportedBand, String timezone) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống");
        }
        this.displayName = displayName.trim();
        this.avatarReference = avatarReference;
        this.bio = bio;
        this.selfReportedBand = selfReportedBand;
        if (timezone != null && !timezone.isBlank()) {
            String normalizedTimezone = timezone.trim();
            if (isValidTimezone(normalizedTimezone)) this.timezone = normalizedTimezone;
        }
        touch();
    }

    private static boolean isValidTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
            return true;
        } catch (DateTimeException ignored) {
            return false;
        }
    }

    public void changeVisibility(ProfileVisibility visibility) {
        if (visibility == null) {
            throw new IllegalArgumentException("Quyền hiển thị không được để trống");
        }
        this.visibility = visibility;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
