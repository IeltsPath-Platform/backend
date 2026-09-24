package com.group01.content.application.command;

import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.Skill;

import java.util.UUID;

public record CreateKnowledgePointCommand(
        UUID topicId,
        String code,
        String name,
        KnowledgePointKind kind,
        LearningType learningType,
        Skill skill,
        String description
) {}

