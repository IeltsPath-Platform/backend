package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.port.ContentPackageProvider;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.group01.assessment.domain.entity.AttemptSection;
import com.group01.assessment.domain.exception.PackageNotAttemptableException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import com.group01.assessment.domain.repository.AttemptItemKnowledgePointRepository;
import com.group01.assessment.domain.repository.AttemptItemRepository;
import com.group01.assessment.domain.repository.AttemptSectionRepository;
import com.group01.assessment.domain.vo.AttemptType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes an attempt and freezes what its results are interpreted against: section text, the question without its
 * answer, the answer (kept server-side) and Content's knowledge-point mapping. None of it is re-read from Content.
 */
@Service
public class AttemptCreator {
    private final AssessmentAttemptRepository attempts;
    private final AttemptSectionRepository sections;
    private final AttemptItemRepository items;
    private final AttemptItemKnowledgePointRepository knowledgeSnapshot;
    private final ObjectMapper json;

    public AttemptCreator(AssessmentAttemptRepository attempts, AttemptSectionRepository sections,
                          AttemptItemRepository items, AttemptItemKnowledgePointRepository knowledgeSnapshot,
                          ObjectMapper json) {
        this.attempts = attempts;
        this.sections = sections;
        this.items = items;
        this.knowledgeSnapshot = knowledgeSnapshot;
        this.json = json;
    }

    /** MVP packages define no time limit, so {@code expiresAt} is always null; the client never sets it. */
    @Transactional
    public AssessmentAttemptResult create(StartAssessmentAttemptCommand command, AttemptType type,
                                          ContentPackageProvider.PackageVersion packageVersion) {
        if (packageVersion.sections().isEmpty()
                || packageVersion.sections().stream().anyMatch(section -> section.items().isEmpty())) {
            throw new PackageNotAttemptableException("Package version has a section without questions");
        }
        var attempt = AssessmentAttempt.start(command.userId(), command.packageVersionId(), type, command.mode(),
                command.channel(), null);
        attempts.save(attempt);

        var sectionEntities = new ArrayList<AttemptSection>();
        var itemEntities = new ArrayList<AttemptItem>();
        var knowledgeEntities = new ArrayList<AttemptItemKnowledgePoint>();
        for (var section : packageVersion.sections()) {
            UUID sectionId = UUID.randomUUID();
            sectionEntities.add(new AttemptSection(sectionId, attempt.getId(), section.sectionId(),
                    section.sortOrder(), sectionSnapshot(section)));
            for (var item : section.items()) {
                UUID itemId = UUID.randomUUID();
                itemEntities.add(new AttemptItem(itemId, sectionId, item.questionVersionId(), item.sortOrder(),
                        questionSnapshot(item), answerSnapshot(item), knowledgeSnapshot(item)));
                for (var mapping : item.knowledgePoints()) {
                    knowledgeEntities.add(new AttemptItemKnowledgePoint(itemId, mapping.knowledgePointId(),
                            mapping.weight()));
                }
            }
        }
        sections.saveAll(sectionEntities);
        items.saveAll(itemEntities);
        if (!knowledgeEntities.isEmpty()) {
            knowledgeSnapshot.saveAll(knowledgeEntities);
        }
        return AssessmentAttemptResult.from(attempt);
    }

    /** {@code passage} is omitted when the section has none. */
    private String sectionSnapshot(ContentPackageProvider.Section section) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", section.title());
        snapshot.put("skill", section.skill());
        snapshot.put("instructions", section.instructions());
        if (section.passage() != null) {
            snapshot.put("passage", section.passage());
        }
        return write(snapshot);
    }

    /** What the learner sees: stem and options (null for a fill question), never the answer. */
    private String questionSnapshot(ContentPackageProvider.Item item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("stem", item.stem());
        snapshot.put("options", item.options());
        return write(snapshot);
    }

    /** Server-side only; read by the auto-grader and by learner solutions once the result allows them. */
    private String answerSnapshot(ContentPackageProvider.Item item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("answerSpec", item.answerSpec());
        snapshot.put("explanation", item.explanation());
        snapshot.put("maxScore", item.maxScore());
        return write(snapshot);
    }

    private String knowledgeSnapshot(ContentPackageProvider.Item item) {
        List<Map<String, Object>> snapshot = item.knowledgePoints().stream()
                .map(mapping -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("knowledgePointId", mapping.knowledgePointId());
                    value.put("weight", mapping.weight());
                    return value;
                })
                .toList();
        return write(snapshot);
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Attempt snapshot could not be serialized", exception);
        }
    }
}
