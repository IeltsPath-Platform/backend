package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.application.service.SpeakingResultValidator.InvalidSpeakingGradeException;
import org.springframework.stereotype.Service;

/**
 * Grades one Speaking submission with the language model: builds the prompts, asks, checks the answer and puts the
 * stored result together. An answer that is not usable as a grade is asked for once more with a reminder of the format;
 * a second failure is reported as {@link InvalidSpeakingGradeException} so the submission goes to an examiner and the
 * learner is not charged. A model that cannot be reached is not retried here: the caller schedules the retry.
 */
@Service
public class SpeakingGrader {
    private static final String REMINDER = "\n\nYour previous answer could not be used. Reply with one valid JSON object "
            + "only, with exactly one entry for each of FC, LR and GRA, and each band a number from 0 to 9 in steps of 0.5.";

    private final LlmClient llm;
    private final SpeakingPromptAssembler prompts;
    private final SpeakingResultValidator validator;
    private final SpeakingResultAssembler results;

    public SpeakingGrader(LlmClient llm, SpeakingPromptAssembler prompts, SpeakingResultValidator validator,
                          SpeakingResultAssembler results) {
        this.llm = llm;
        this.prompts = prompts;
        this.validator = validator;
        this.results = results;
    }

    public boolean available() {
        return llm.configured();
    }

    /** @throws SpeakingPromptAssembler.UnusableRubricException the rubric cannot grade Speaking
     *  @throws SpeakingPromptAssembler.TranscriptTooLongException the prompt would exceed the configured size */
    public SpeakingPromptAssembler.Prepared prepare(LearningContentClient.Rubric rubric, SpeakingGradingInput input) {
        return prompts.assemble(rubric, input);
    }

    public SpeakingResultAssembler.Assembled grade(SpeakingPromptAssembler.Prepared prepared, SpeakingGradingInput input) {
        var allowedKps = input.learner().allowedKpCodes();
        InvalidSpeakingGradeException failure;
        try {
            return assemble(prepared, input, llm.completeJson(prepared.systemPrompt(), prepared.userPrompt()), allowedKps);
        } catch (InvalidSpeakingGradeException first) {
            failure = first;
        }
        try {
            return assemble(prepared, input, llm.completeJson(prepared.systemPrompt() + REMINDER, prepared.userPrompt()),
                    allowedKps);
        } catch (InvalidSpeakingGradeException second) {
            second.addSuppressed(failure);
            throw second;
        }
    }

    private SpeakingResultAssembler.Assembled assemble(SpeakingPromptAssembler.Prepared prepared, SpeakingGradingInput input,
                                                       String raw, java.util.Set<String> allowedKps) {
        return results.assemble(prepared.rubric(), input, validator.validate(raw, input, allowedKps));
    }
}
