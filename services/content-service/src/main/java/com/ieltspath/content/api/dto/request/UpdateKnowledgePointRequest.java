package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.CefrLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateKnowledgePointRequest(
        @NotNull(message = "Cần chọn chủ đề") UUID topicId,
        @NotBlank(message = "Mã không được để trống") @Size(max = 100, message = "Mã không được dài quá 100 ký tự")
        String code,
        @NotBlank(message = "Tên không được để trống") @Size(max = 255, message = "Tên không được dài quá 255 ký tự")
        String name,
        @NotNull(message = "Cần chọn loại") KnowledgePointKind kind,
        LearningType learningType,
        Skill skill,
        String description,
        CefrLevel cefrLevel,
        String canDo,
        String scopeIn,
        String scopeOut
) {}
