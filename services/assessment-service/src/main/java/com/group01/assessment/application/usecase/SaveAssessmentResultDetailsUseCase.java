package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.ErrorAnalysisInput;
import com.group01.assessment.application.command.SaveAssessmentResultDetailsCommand;
import com.group01.assessment.domain.entity.ErrorAnalysisItem;
import com.group01.assessment.domain.entity.ItemResult;
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

    public SaveAssessmentResultDetailsUseCase(AssessmentAttemptRepository attempts,
                                              AssessmentResultRepository results,
                                              AttemptItemRepository attemptItems,
                                              SkillScoreRepository skills,
                                              ItemResultRepository items,
                                              ErrorAnalysisItemRepository errors) {
        this.attempts = attempts;
        this.results = results;
        this.attemptItems = attemptItems;
        this.skills = skills;
        this.items = items;
        this.errors = errors;
    }

    @Transactional
    public void execute(SaveAssessmentResultDetailsCommand command) {
        var attempt = attempts.findByIdAndUserId(command.attemptId(), command.userId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        var result = results.findLatestForUpdateByAttemptId(attempt.getId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));

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

            Map<UUID, ItemResult> existingByAttemptItem = items.findByResultId(result.id()).stream()
                    .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
            List<ItemResult> itemResults = command.itemResults().stream()
                    .map(item -> {
                        ItemResult existing = existingByAttemptItem.get(item.attemptItemId());
                        return new ItemResult(existing == null ? UUID.randomUUID() : existing.id(), result.id(),
                                item.attemptItemId(), item.score(), item.correct(), item.durationMilliseconds(),
                                item.feedbackSnapshot());
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
    }
}
