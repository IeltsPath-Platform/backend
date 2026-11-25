package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.CefrLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateKnowledgePointRequest(
        @NotNull(message = "Cần chọn chủ đề")
        UUID topicId,

        @Size(max = 100, message = "Mã không được dài quá 100 ký tự")
        String code,

        Integer codeLevel,

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 255, message = "Tên không được dài quá 255 ký tự")
        String name,

        @NotNull(message = "Cần chọn loại")
        KnowledgePointKind kind,

        @NotNull(message = "Cần chọn loại học (learningType)")
        LearningType learningType,

        Skill skill,
        String description,
        CefrLevel cefrLevel,

        @NotBlank(message = "Cần nhập mô tả can-do")
        String canDo,
        String scopeIn,
        String scopeOut
) {}
