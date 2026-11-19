package com.ieltspath.content.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTopicRequest(
        UUID parentTopicId,

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 255, message = "Tên không được dài quá 255 ký tự")
        String name,

        int sortOrder,
        ContentStatus status,
        @DecimalMin(value = "0.0", message = "Band thấp nhất không được nhỏ hơn 0.0")
        @DecimalMax(value = "9.0", message = "Band thấp nhất không được lớn hơn 9.0")
        BigDecimal bandMin,
        @DecimalMin(value = "0.0", message = "Band cao nhất không được nhỏ hơn 0.0")
        @DecimalMax(value = "9.0", message = "Band cao nhất không được lớn hơn 9.0")
        BigDecimal bandMax,
        // Null keeps the current skill; a change is refused once the topic has published lessons.
        Skill skill,
        UUID courseId,
        @Size(max = 2000) String description
) {
    public UpdateTopicRequest(UUID parentTopicId, String name, int sortOrder, ContentStatus status,
                              BigDecimal bandMin, BigDecimal bandMax, Skill skill) {
        this(parentTopicId, name, sortOrder, status, bandMin, bandMax, skill, null, null);
    }

    public UpdateTopicRequest(UUID parentTopicId, String name, int sortOrder, ContentStatus status,
                              BigDecimal bandMin, BigDecimal bandMax, Skill skill, UUID courseId) {
        this(parentTopicId, name, sortOrder, status, bandMin, bandMax, skill, courseId, null);
    }
}

