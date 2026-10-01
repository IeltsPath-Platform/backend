package com.group01.learningsupport.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "streaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreakJpaEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "current_days", nullable = false)
    private int currentDays;

    @Column(name = "longest_days", nullable = false)
    private int longestDays;

    @Column(name = "last_qualified_date")
    private LocalDate lastQualifiedDate;

    @Column(nullable = false, length = 100)
    private String timezone;
}
