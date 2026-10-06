package com.group01.content.application.result;

import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;
import java.util.UUID;

/** Question metadata and stored answer specification used to validate a package before publication. */
public record PackageQuestionSpec(UUID questionVersionId, QuestionType questionType, Skill skill, String answerSpecJson) {}
