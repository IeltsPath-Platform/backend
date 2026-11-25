package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.QuestionType;
import java.util.UUID;

public record SectionQuestionDraftRequest(UUID packageId, UUID versionId, UUID sectionId,
                                          QuestionType questionType, int count, String instructions) {
}
