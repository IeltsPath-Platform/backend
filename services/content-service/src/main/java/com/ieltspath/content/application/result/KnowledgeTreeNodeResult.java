package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record KnowledgeTreeNodeResult(UUID id, String code, String name, Skill skill, KnowledgePointKind kind,
                                      LearningType learningType, BigDecimal band, UUID courseId, UUID topicId,
                                      List<UUID> lessonIds, ContentStatus status) {}
