package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.aggregate.AiDraft;

public record SectionQuestionDraftDetail(AiDraft draft, boolean sourceChanged, String sectionState) {}
