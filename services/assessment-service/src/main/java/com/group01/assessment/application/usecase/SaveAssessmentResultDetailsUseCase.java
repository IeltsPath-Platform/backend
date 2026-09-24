package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.ErrorAnalysisInput;
import com.group01.assessment.application.command.KnowledgeJudgmentInput;
import com.group01.assessment.application.command.SaveAssessmentResultDetailsCommand;
import com.group01.assessment.domain.entity.ErrorAnalysisItem;
import com.group01.assessment.domain.entity.ItemResult;
import com.group01.assessment.domain.entity.ItemResultKnowledgeJudgment;
import com.group01.assessment.domain.entity.SkillScore;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SaveAssessmentResultDetailsUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AssessmentResultRepository results;
    private final AttemptItemRepository attemptItems;
    private final SkillScoreRepository skills;
    private final ItemResultRepository items;
    private final ErrorAnalysisItemRepository errors;
    private final AttemptItemKnowledgePointRepository knowledgeSnapshot;
    private final ItemResultKnowledgeJudgmentRepository judgments;

    public SaveAssessmentResultDetailsUseCase(AssessmentAttemptRepository attempts,
                                              AssessmentResultRepository results,
                                              AttemptItemRepository attemptItems,
                                              SkillScoreRepository skills,
                                              ItemResultRepository items,
                                              ErrorAnalysisItemRepository errors,
                                              AttemptItemKnowledgePointRepository knowledgeSnapshot,
                                              ItemResultKnowledgeJudgmentRepository judgments) {
        this.attempts = attempts;
        this.results = results;
        this.attemptItems = attemptItems;
        this.skills = skills;
        this.items = items;
        this.errors = errors;
        this.knowledgeSnapshot = knowledgeSnapshot;
        this.judgments = judgments;
    }

    @Transactional
    public void execute(SaveAssessmentResultDetailsCommand command) {
        var attempt = attempts.findByIdAndUserId(command.attemptId(), command.userId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        var result = results.findLatestForUpdateByAttemptId(attempt.getId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));
        if (!result.isGradable()) {
            throw new InvalidAssessmentStateException(
                    "A finalized result cannot be changed; create a new result version to regrade");
        }

        if (command.skillScores() != null && !command.skillScores().isEmpty()) {
            Set<Skill> submittedSkills = new HashSet<>();
            for (var score : command.skillScores()) {
                if (!submittedSkills.add(score.skill())) {
                    throw new InvalidAssessmentStateException("Only one score per skill is allowed");
                }
            }

            Map<Skill, SkillScore> existingBySkill = skills.findByResultId(result.id()).stream()
                    .collect(Collectors.toMap(SkillScore::skill, Function.identity()));
            skills.saveAll(command.skillScores().stream()
                    .map(score -> {
                        SkillScore existing = existingBySkill.get(score.skill());
                        return new SkillScore(existing == null ? UUID.randomUUID() : existing.id(), result.id(),
                                score.skill(), score.rawScore(), score.band(), score.gradingSource(),
                                score.feedbackRevisionId());
                    })
                    .toList());
        }

        if (command.itemResults() != null && !command.itemResults().isEmpty()) {
            List<UUID> submittedAttemptItemIds = command.itemResults().stream()
                    .map(item -> item.attemptItemId())
                    .toList();
            Set<UUID> uniqueAttemptItemIds = new HashSet<>(submittedAttemptItemIds);
            if (submittedAttemptItemIds.contains(null) || uniqueAttemptItemIds.size() != submittedAttemptItemIds.size()) {
                throw new InvalidAssessmentStateException(
                        "Each item result must reference a distinct assessment item");
            }

            Set<UUID> existingAttemptItemIds = attemptItems.findExistingIdsByAttemptId(
                    uniqueAttemptItemIds, attempt.getId());
            if (!existingAttemptItemIds.containsAll(uniqueAttemptItemIds)) {
                throw new InvalidAssessmentStateException(
                        "Item results must reference items from this assessment attempt");
            }

            for (var item : command.itemResults()) {
                if (item.maxScore() != null && (item.maxScore() <= 0
                        || (item.score() != null && item.score() > item.maxScore()))) {
                    throw new InvalidAssessmentStateException(
                            "Item maximum score must be positive and not below the awarded score");
                }
            }

            Map<UUID, ItemResult> existingByAttemptItem = items.findByResultId(result.id()).stream()
                    .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
            List<ItemResult> itemResults = command.itemResults().stream()
                    .map(item -> {
                        ItemResult existing = existingByAttemptItem.get(item.attemptItemId());
                        return new ItemResult(existing == null ? UUID.randomUUID() : existing.id(), result.id(),
                                item.attemptItemId(), item.score(), item.maxScore(), item.correct(),
                                item.durationMilliseconds(), item.feedbackSnapshot());
                    })
                    .toList();
            items.saveAll(itemResults);
        }

        if (command.errors() != null && !command.errors().isEmpty()) {
            Set<UUID> submittedErrorIds = new HashSet<>();
            for (ErrorAnalysisInput error : command.errors()) {
                if (error.id() == null || !submittedErrorIds.add(error.id())) {
                    throw new InvalidAssessmentStateException("Error analysis IDs must be present and unique");
                }
            }

            Map<UUID, ItemResult> itemResultByAttemptItemId = items.findByResultId(result.id()).stream()
                    .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
            Set<UUID> validResultItemIds = itemResultByAttemptItemId.values().stream()
                    .map(ItemResult::id)
                    .collect(Collectors.toSet());
            boolean referencesMissingItemResult = command.errors().stream()
                    .anyMatch(error -> !itemResultByAttemptItemId.containsKey(error.attemptItemId()));
            if (referencesMissingItemResult) {
                throw new InvalidAssessmentStateException(
                        "Error analysis must reference an item result from this assessment");
            }

            List<ErrorAnalysisItem> existingErrors = errors.findByIds(submittedErrorIds);
            boolean reusesIdFromAnotherResult = existingErrors.stream()
                    .anyMatch(error -> !validResultItemIds.contains(error.itemResultId()));
            if (reusesIdFromAnotherResult) {
                throw new InvalidAssessmentStateException(
                        "Error analysis ID already belongs to another assessment result");
            }

            errors.saveAll(command.errors().stream()
                    .map(error -> new ErrorAnalysisItem(error.id(),
                            itemResultByAttemptItemId.get(error.attemptItemId()).id(),
                            error.knowledgePointId(), error.errorType(), error.explanation()))
                    .toList());
        }

        if (command.knowledgeJudgments() != null && !command.knowledgeJudgments().isEmpty()) {
            saveKnowledgeJudgments(result.id(), command.knowledgeJudgments());
        }
    }

    private void saveKnowledgeJudgments(UUID resultId, List<KnowledgeJudgmentInput> inputs) {
        Set<List<UUID>> submitted = new HashSet<>();
        for (KnowledgeJudgmentInput input : inputs) {
            if (input.attemptItemId() == null || input.knowledgePointId() == null || input.judgment() == null) {
                throw new InvalidAssessmentStateException("Knowledge judgments require item, knowledge point and judgment");
            }
            if (!submitted.add(List.of(input.attemptItemId(), input.knowledgePointId()))) {
                throw new InvalidAssessmentStateException("Only one judgment per item and knowledge point is allowed");
            }
        }

        Map<UUID, ItemResult> itemResultByAttemptItemId = items.findByResultId(resultId).stream()
                .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
        Set<UUID> judgedItems = inputs.stream().map(KnowledgeJudgmentInput::attemptItemId).collect(Collectors.toSet());
        if (!itemResultByAttemptItemId.keySet().containsAll(judgedItems)) {
            throw new InvalidAssessmentStateException("Knowledge judgments must reference a graded item of this result");
        }
        // A judgment is only meaningful for a knowledge point the item was attributed to when the attempt started.
        Set<List<UUID>> snapshotted = knowledgeSnapshot.findByAttemptItemIds(judgedItems).stream()
                .map(mapping -> List.of(mapping.attemptItemId(), mapping.knowledgePointId()))
                .collect(Collectors.toSet());
        if (!snapshotted.containsAll(submitted)) {
            throw new InvalidAssessmentStateException(
                    "Knowledge judgments must reference a knowledge point mapped to the item");
        }

        judgments.saveAll(inputs.stream()
                .map(input -> new ItemResultKnowledgeJudgment(
                        itemResultByAttemptItemId.get(input.attemptItemId()).id(),
                        input.knowledgePointId(), input.judgment()))
                .toList());
    }
}
