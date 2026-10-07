package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import java.util.UUID;

/** Question metadata and stored answer specification used to validate a package before publication. */
public record PackageQuestionSpec(UUID questionVersionId, QuestionType questionType, Skill skill, String answerSpecJson) {}
