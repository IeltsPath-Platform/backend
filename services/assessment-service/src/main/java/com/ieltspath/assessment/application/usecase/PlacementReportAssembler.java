package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.result.PlacementResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.vo.GradingJobStatus;
import com.ieltspath.assessment.domain.vo.Skill;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The per-section detail of a graded placement: for Listening and Reading every question with the learner's answer,
 * the expected one and whether it was right; for Writing every essay with its band and the LLM's comments. Speaking has
 * no recordings to report on yet, so its section carries neither.
 */
@Component
class PlacementReportAssembler {
    private final AttemptSectionRepository sections;
    private final AttemptItemRepository attemptItems;
    private final AttemptResponseRepository responses;
    private final ItemResultRepository itemResults;
    private final GateEssayJobStore jobs;
    private final PlacementGradingService grading;
    private final ObjectMapper json;

    PlacementReportAssembler(AttemptSectionRepository sections, AttemptItemRepository attemptItems,
                             AttemptResponseRepository responses, ItemResultRepository itemResults,
                             GateEssayJobStore jobs, PlacementGradingService grading, ObjectMapper json) {
        this.sections = sections;
        this.attemptItems = attemptItems;
        this.responses = responses;
        this.itemResults = itemResults;
        this.jobs = jobs;
        this.grading = grading;
        this.json = json;
    }

    List<PlacementResult.Section> sections(UUID attemptId, UUID resultId) {
        List<AttemptItem> items = attemptItems.findByAttemptId(attemptId);
        Map<UUID, List<AttemptItem>> itemsBySection = items.stream()
                .collect(Collectors.groupingBy(AttemptItem::attemptSectionId));
        Map<UUID, AttemptResponse> responseByItem = responses.findByAttemptItemIds(
                        items.stream().map(AttemptItem::id).toList()).stream()
                .collect(Collectors.toMap(AttemptResponse::attemptItemId, Function.identity(), (first, second) -> second));
        Map<UUID, ItemResult> resultByItem = itemResults.findByResultId(resultId).stream()
                .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
        Map<UUID, GateEssayJobStore.JobState> jobByItem = jobs.aiJobs(attemptId).stream()
                .collect(Collectors.toMap(GateEssayJobStore.JobState::attemptItemId, Function.identity(),
                        (first, second) -> second));

        List<PlacementResult.Section> report = new ArrayList<>();
        for (AttemptSection section : sections.findByAttemptId(attemptId).stream()
                .sorted(Comparator.comparingInt(AttemptSection::sortOrder)).toList()) {
            JsonNode snapshot = read(section.snapshot());
            Skill skill = skillOf(snapshot);
            if (skill == null) continue;
            List<AttemptItem> sectionItems = itemsBySection.getOrDefault(section.id(), List.of()).stream()
                    .sorted(Comparator.comparingInt(AttemptItem::sortOrder)).toList();
            List<PlacementResult.Question> questions = new ArrayList<>();
            List<PlacementResult.Essay> essays = new ArrayList<>();
            if (skill == Skill.LISTENING || skill == Skill.READING) {
                sectionItems.forEach(item -> questions.add(question(item, responseByItem.get(item.id()),
                        resultByItem.get(item.id()))));
            } else if (skill == Skill.WRITING) {
                sectionItems.forEach(item -> essays.add(essay(item, jobByItem.get(item.id()))));
            }
            report.add(new PlacementResult.Section(skill, snapshot.path("title").asText(null), questions, essays));
        }
        return report;
    }

    private PlacementResult.Question question(AttemptItem item, AttemptResponse response, ItemResult result) {
        JsonNode question = read(item.questionSnapshot());
        AnswerSnapshot answer = AnswerSnapshot.parse(json, item.answerSnapshot()).orElse(null);
        Map<String, Object> spec = answer == null ? null : answer.answerSpec();
        String expected = null;
        if (spec != null && spec.get("correct") instanceof String correct) {
            expected = optionText(question, correct);
        } else if (spec != null && spec.get("accepted") instanceof List<?> accepted && !accepted.isEmpty()) {
            expected = accepted.stream().map(String::valueOf).collect(Collectors.joining(" / "));
        }
        String given = response == null ? null : read(response.payload()).path("answer").asText(null);
        if (given != null && given.isBlank()) given = null;
        if (given != null && question.path("options").isArray()) given = optionText(question, given);
        String prompt = question.path("stem").asText(question.path("prompt").asText(""));
        return new PlacementResult.Question(item.sortOrder(), prompt, given, expected,
                result != null && Boolean.TRUE.equals(result.correct()), answer == null ? null : answer.explanation());
    }

    /** "B. Six" for lettered options; the option text alone when the key is a word such as NOT_GIVEN. */
    private static String optionText(JsonNode question, String key) {
        for (JsonNode option : question.path("options")) {
            String optionKey = option.path("optionKey").asText(option.path("value").asText(null));
            if (!key.equals(optionKey)) continue;
            String content = option.path("content").asText(option.path("label").asText(key));
            return key.length() == 1 ? key + ". " + content : content;
        }
        return key;
    }

    private PlacementResult.Essay essay(AttemptItem item, GateEssayJobStore.JobState job) {
        String task = AnswerSnapshot.parse(json, item.answerSnapshot())
                .map(AnswerSnapshot::answerSpec).map(spec -> spec.get("task"))
                .map(String::valueOf).orElse(null);
        if (job == null) return new PlacementResult.Essay(task, 0.0, false, null);
        if (!GradingJobStatus.COMPLETED.name().equals(job.status())) {
            return new PlacementResult.Essay(task, null, true, null);
        }
        double band = (job.band() != null ? job.band() : grading.defaultWritingBand()).doubleValue();
        return new PlacementResult.Essay(task, band, true, feedback(job.feedback()));
    }

    private PlacementResult.Feedback feedback(String raw) {
        if (raw == null) return null;
        JsonNode node = read(raw);
        if (!node.isObject()) return null;
        List<PlacementResult.Criterion> criteria = new ArrayList<>();
        node.path("criteria").forEach(criterion -> criteria.add(new PlacementResult.Criterion(
                criterion.path("code").asText(null),
                criterion.path("band").isNumber() ? criterion.path("band").doubleValue() : null,
                criterion.path("comment").asText(null))));
        List<String> focus = new ArrayList<>();
        node.path("focus").forEach(item -> { if (item.isTextual()) focus.add(item.textValue()); });
        return new PlacementResult.Feedback(node.path("summary").asText(null),
                criteria.stream().filter(criterion -> Objects.nonNull(criterion.code())).toList(), focus);
    }

    private static Skill skillOf(JsonNode snapshot) {
        try {
            return Skill.valueOf(snapshot.path("skill").asText());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private JsonNode read(String raw) {
        try {
            return raw == null ? json.missingNode() : json.readTree(raw);
        } catch (Exception exception) {
            return json.missingNode();
        }
    }
}
