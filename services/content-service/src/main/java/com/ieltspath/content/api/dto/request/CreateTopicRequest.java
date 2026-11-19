package com.ieltspath.content.api.dto.request;

import java.math.BigDecimal;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTopicRequest(
        UUID parentTopicId,

        @NotBlank(message = "Mã không được để trống")
        @Size(max = 100, message = "Mã không được dài quá 100 ký tự")
        String code,

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 255, message = "Tên không được dài quá 255 ký tự")
        String name,

        int sortOrder,
        @DecimalMin(value = "0.0", message = "Band thấp nhất không được nhỏ hơn 0.0")
        @DecimalMax(value = "9.0", message = "Band thấp nhất không được lớn hơn 9.0")
        BigDecimal bandMin,
        @DecimalMin(value = "0.0", message = "Band cao nhất không được nhỏ hơn 0.0")
        @DecimalMax(value = "9.0", message = "Band cao nhất không được lớn hơn 9.0")
        BigDecimal bandMax,
        // The skill the topic's lessons teach; optional, never ALL.
        Skill skill,
        UUID courseId,
        @Size(max = 2000) String description
) {
    public CreateTopicRequest(UUID parentTopicId, String code, String name, int sortOrder,
                              BigDecimal bandMin, BigDecimal bandMax, Skill skill) {
        this(parentTopicId, code, name, sortOrder, bandMin, bandMax, skill, null, null);
    }

    public CreateTopicRequest(UUID parentTopicId, String code, String name, int sortOrder,
                              BigDecimal bandMin, BigDecimal bandMax, Skill skill, UUID courseId) {
        this(parentTopicId, code, name, sortOrder, bandMin, bandMax, skill, courseId, null);
    }
}

