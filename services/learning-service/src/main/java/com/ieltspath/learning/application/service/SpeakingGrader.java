package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.application.service.SpeakingPromptAssembler.Graded;
import com.ieltspath.learning.application.service.SpeakingPromptAssembler.Task;
import com.ieltspath.learning.application.service.SpeakingResultValidator.InvalidSpeakingGradeException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Grades one Speaking submission with the language model: builds the prompts, asks, checks the answers and puts the
 * stored result together.
 *
 * <p>A grade is several short calls, not one long one, because the wait is the time the model takes to write its answer.
 * FC, LR, GRA and the part notes are asked at the same time; the overall advice is asked last, from the bands the first
 * calls decided, so the advice cannot contradict a band. The answers are merged into the one object that
 * {@link SpeakingResultValidator} already checks. A call whose answer is not usable is asked again once with a reminder of
 * the format; a second failure is reported as {@link InvalidSpeakingGradeException} so the submission goes to an examiner
 * and the learner is not charged. A model that cannot be reached is not retried here: the caller schedules the retry.
 */
@Service
public class SpeakingGrader {
    /**
     * One call writes a part of the grade, not all of it, so it needs far less than a whole grade did; the wait is kept
     * short enough that a grade that is stuck on a retry still finishes before another worker takes the submission over.
     */
    /**
     * One grade is five calls and any one that fails spoils it, so a call that meets a 5xx or a dropped connection (Google's
     * "high demand" 503) is asked again by the shared retry. This is the only place that turns the retry on.
     */
    static final LlmClient.Budget BUDGET = new LlmClient.Budget(8_000, 60, true);
    private static final String REMINDER = "\n\nYour previous answer could not be used. Reply with one valid JSON object "
            + "only, in exactly the shape described above, with every band a number from 0 to 9 in steps of 0.5.";
    private static final List<Task> FIRST_ROUND = List.of(Task.FC, Task.LR, Task.GRA, Task.OVERVIEW);
    /** Waiting threads cost almost nothing, and the number of calls at once is limited by the model bulkhead. */
    private static final ExecutorService CALLS = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("speaking-grade-", 0).factory());

    private final LlmClient llm;
    private final SpeakingPromptAssembler prompts;
    private final SpeakingResultValidator validator;
    private final SpeakingResultAssembler results;
    private final ObjectMapper json;

    public SpeakingGrader(LlmClient llm, SpeakingPromptAssembler prompts, SpeakingResultValidator validator,
                          SpeakingResultAssembler results, ObjectMapper json) {
        this.llm = llm;
        this.prompts = prompts;
        this.validator = validator;
        this.results = results;
        this.json = json;
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
        Map<Task, CompletableFuture<JsonNode>> started = new EnumMap<>(Task.class);
        for (Task task : FIRST_ROUND) {
            started.put(task, CompletableFuture.supplyAsync(() -> ask(prepared, input, task, prepared.userPrompt()), CALLS));
        }
        Map<Task, JsonNode> answers = new EnumMap<>(Task.class);
        RuntimeException failure = null;
        for (Task task : FIRST_ROUND) {
            try {
                answers.put(task, started.get(task).join());
            } catch (CompletionException exception) {
                // Every call is waited for, so none is left running unseen; the first failure is the one reported.
                RuntimeException cause = exception.getCause() instanceof RuntimeException runtime ? runtime : exception;
                if (failure == null) failure = cause; else failure.addSuppressed(cause);
            }
        }
        if (failure != null) throw failure;

        JsonNode overall = ask(prepared, input, Task.SYNTHESIS,
                prompts.withGraded(prepared.userPrompt(), graded(answers)));
        ObjectNode merged = merge(answers, overall);
        return results.assemble(prepared.rubric(), input,
                validator.validate(merged.toString(), input, input.learner().allowedKpCodes()));
    }

    /** One call, asked again once with a reminder when its answer is not usable. */
    private JsonNode ask(SpeakingPromptAssembler.Prepared prepared, SpeakingGradingInput input, Task task, String user) {
        String system = prompts.system(prepared.rubric(), input, task);
        InvalidSpeakingGradeException failure;
        try {
            return checked(task, llm.completeJson(system, user, BUDGET));
        } catch (InvalidSpeakingGradeException first) {
            failure = first;
        }
        try {
            return checked(task, llm.completeJson(system + REMINDER, user, BUDGET));
        } catch (InvalidSpeakingGradeException second) {
            second.addSuppressed(failure);
            throw second;
        }
    }

    /** The answer as an object, and for a criterion call with its criterion and a band that can be trusted. */
    private JsonNode checked(Task task, String raw) {
        JsonNode root = validator.parse(raw);
        if (task.criterion() != null) {
            JsonNode entry = root.path("criteria").path(0);
            if (!task.criterion().equals(entry.path("code").asText(""))) throw new InvalidSpeakingGradeException();
            SpeakingResultValidator.band(entry.path("band"));
        }
        return root;
    }

    /** What the last call is told: the band, the comment and the weaknesses each criterion call decided. */
    private List<Graded> graded(Map<Task, JsonNode> answers) {
        List<Graded> graded = new ArrayList<>();
        for (String code : SpeakingPromptAssembler.GRADED_CRITERIA) {
            JsonNode entry = answers.get(Task.valueOf(code)).path("criteria").path(0);
            List<String> weaknesses = new ArrayList<>();
            entry.path("weaknesses").forEach(item -> {
                String text = SpeakingResultValidator.clean(item.asText(""), 200);
                if (!text.isBlank() && weaknesses.size() < 4) weaknesses.add(text);
            });
            graded.add(new Graded(code, SpeakingResultValidator.band(entry.path("band")),
                    SpeakingResultValidator.clean(entry.path("comment").asText(""), 400), weaknesses));
        }
        return graded;
    }

    /**
     * The answers of the calls as the one object the validator reads. The errors of the three criterion calls are taken in
     * turn (the worst of each first), so the validator's limit cuts the least serious ones and not one criterion's.
     */
    private ObjectNode merge(Map<Task, JsonNode> answers, JsonNode overall) {
        ObjectNode merged = json.createObjectNode();
        ArrayNode criteria = merged.putArray("criteria");
        List<JsonNode> errorLists = new ArrayList<>();
        for (String code : SpeakingPromptAssembler.GRADED_CRITERIA) {
            JsonNode answer = answers.get(Task.valueOf(code));
            criteria.add(answer.path("criteria").path(0));
            errorLists.add(answer.path("errors"));
        }
        merged.set("errors", interleave(errorLists));
        JsonNode overview = answers.get(Task.OVERVIEW);
        for (String field : List.of("partNotes", "flags", "transcriptReliability", "kpObservations", "pronunciationNotes")) {
            if (overview.has(field)) merged.set(field, overview.get(field));
        }
        for (String field : List.of("summary", "focus", "personalFeedback")) {
            if (overall.has(field)) merged.set(field, overall.get(field));
        }
        return merged;
    }

    ArrayNode interleave(List<JsonNode> lists) {
        ArrayNode merged = json.createArrayNode();
        Set<String> seen = new HashSet<>();
        for (int index = 0; ; index++) {
            boolean any = false;
            for (JsonNode list : lists) {
                if (!list.isArray() || index >= list.size()) continue;
                any = true;
                JsonNode error = list.get(index);
                // The same words named twice for the same mistake are one finding.
                String key = error.path("code").asText("") + "|" + SpeakingGradingInput.normalize(error.path("excerpt").asText(""));
                if (seen.add(key)) merged.add(error);
            }
            if (!any) return merged;
        }
    }
}
