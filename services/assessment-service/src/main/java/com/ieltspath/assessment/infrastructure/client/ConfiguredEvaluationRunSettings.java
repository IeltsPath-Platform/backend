package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.application.port.EvaluationRunSettings;
import com.ieltspath.assessment.infrastructure.config.AssessmentLlmProperties;
import org.springframework.stereotype.Component;

/** Evaluation run settings taken from the LLM configuration and the prompt templates of this service. */
@Component
public class ConfiguredEvaluationRunSettings implements EvaluationRunSettings {
    private final AssessmentLlmProperties llm;

    public ConfiguredEvaluationRunSettings(AssessmentLlmProperties llm) {
        this.llm = llm;
    }

    @Override
    public String model() {
        return llm.getModel();
    }

    @Override
    public String reasoningEffort() {
        String effort = llm.getReasoningEffort();
        return effort == null || effort.isBlank() ? null : effort;
    }

    @Override
    public String defaultTemplateVersion() {
        return RubricPromptBuilder.TEMPLATE_VERSION;
    }

    @Override
    public boolean isKnownTemplate(String templateVersion) {
        return RubricPromptBuilder.isKnownTemplate(templateVersion);
    }

    @Override
    public String promptVersion(ActiveRubricProvider.Rubric rubric, String templateVersion) {
        return RubricPromptBuilder.promptVersion(rubric, templateVersion);
    }
}
