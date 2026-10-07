package com.ieltspath.user.domain.aggregate;

import com.ieltspath.user.domain.exception.InvalidDataException;

import com.ieltspath.user.domain.DomainChecks;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Streak {
    private final UUID userId;
    private final int currentDays;
    private final int longestDays;
    private final LocalDate lastQualifiedDate;
    private final String timezone;

    public static Streak of(UUID userId, int currentDays, int longestDays, LocalDate lastQualifiedDate, String timezone) {
        if (currentDays < 0 || longestDays < 0) {
            throw new InvalidDataException("streak không hợp lệ");
        }
        return new Streak(
                DomainChecks.userId(userId),
                currentDays,
                longestDays,
                lastQualifiedDate,
                DomainChecks.required(timezone, 100, "timezone")
        );
    }
}
