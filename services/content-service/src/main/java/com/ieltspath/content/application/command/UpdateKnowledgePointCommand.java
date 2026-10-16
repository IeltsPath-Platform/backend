package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;

import java.util.UUID;

public record UpdateKnowledgePointCommand(UUID topicId, String code, String name, KnowledgePointKind kind,
                                         LearningType learningType, Skill skill, String description) {}
