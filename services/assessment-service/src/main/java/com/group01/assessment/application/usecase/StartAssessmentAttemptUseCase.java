package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.port.KnowledgeMappingProvider;
import com.group01.assessment.application.port.LearningGoalProvider;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.group01.assessment.domain.entity.AttemptSection;
import com.group01.assessment.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * Starts an attempt and freezes what later results are interpreted against: the learner's active goal and
 * Content's question to knowledge-point mapping for every item. Neither is re-resolved after this point.
 */
@Service
public class StartAssessmentAttemptUseCase {
    private final AssessmentAttemptRepository attempts; private final AttemptSectionRepository sections; private final AttemptItemRepository items;
    private final AttemptItemKnowledgePointRepository knowledgeSnapshot;
    private final LearningGoalProvider learningGoals;
    private final KnowledgeMappingProvider knowledgeMappings;
    public StartAssessmentAttemptUseCase(AssessmentAttemptRepository attempts, AttemptSectionRepository sections, AttemptItemRepository items,
                                         AttemptItemKnowledgePointRepository knowledgeSnapshot, LearningGoalProvider learningGoals,
                                         KnowledgeMappingProvider knowledgeMappings){
        this.attempts=attempts;this.sections=sections;this.items=items;
        this.knowledgeSnapshot=knowledgeSnapshot;this.learningGoals=learningGoals;this.knowledgeMappings=knowledgeMappings;
    }
    @Transactional
    public AssessmentAttemptResult execute(StartAssessmentAttemptCommand c){
        var sectionInputs=c.sections()==null?List.<StartAssessmentAttemptCommand.SectionInput>of():c.sections();
        var questionVersionIds=new LinkedHashSet<UUID>();
        for(var s:sectionInputs) for(var i:s.items()==null?List.<StartAssessmentAttemptCommand.ItemInput>of():s.items()) questionVersionIds.add(i.questionVersionId());
        UUID learningGoalId=learningGoals.findActiveGoalId(c.userId()).orElse(null);
        Map<UUID,List<KnowledgeMappingProvider.KnowledgePointWeight>> mappings=questionVersionIds.isEmpty()?Map.of():knowledgeMappings.findByQuestionVersionIds(questionVersionIds);

        var attempt=AssessmentAttempt.start(c.userId(),c.packageVersionId(),c.attemptType(),c.mode(),c.channel(),c.expiresAt(),learningGoalId);
        attempts.save(attempt);
        var sectionEntities=new ArrayList<AttemptSection>(); var itemEntities=new ArrayList<AttemptItem>();
        var snapshotEntities=new ArrayList<AttemptItemKnowledgePoint>();
        var sectionOrders=new HashSet<Integer>();
        for(var s:sectionInputs){
            if(!sectionOrders.add(s.sortOrder())) throw new com.group01.assessment.domain.exception.InvalidAssessmentStateException("Duplicate section sort order");
            var sectionId=UUID.randomUUID(); sectionEntities.add(new AttemptSection(sectionId,attempt.getId(),s.contentSectionId(),s.sortOrder(),s.snapshot()));
            var itemOrders=new HashSet<Integer>();
            for(var i:s.items()==null?List.<StartAssessmentAttemptCommand.ItemInput>of():s.items()) {
                if(!itemOrders.add(i.sortOrder())) throw new com.group01.assessment.domain.exception.InvalidAssessmentStateException("Duplicate item sort order");
                var itemId=UUID.randomUUID();
                itemEntities.add(new AttemptItem(itemId,sectionId,i.questionVersionId(),i.sortOrder(),i.questionSnapshot(),i.answerSnapshot(),i.knowledgeSnapshot()));
                for(var mapping:mappings.getOrDefault(i.questionVersionId(),List.of())) snapshotEntities.add(new AttemptItemKnowledgePoint(itemId,mapping.knowledgePointId(),mapping.weight()));
            }
        }
        sections.saveAll(sectionEntities); items.saveAll(itemEntities);
        if(!snapshotEntities.isEmpty()) knowledgeSnapshot.saveAll(snapshotEntities);
        return result(attempt);
    }
    private AssessmentAttemptResult result(AssessmentAttempt a){return new AssessmentAttemptResult(a.getId(),a.getUserId(),a.getPackageVersionId(),a.getAttemptType(),a.getMode(),a.getChannel(),a.getStatus(),a.getStartedAt(),a.getSubmittedAt(),a.getExpiresAt(),a.getRowVersion(),a.getCreatedAt(),a.getUpdatedAt());}
}
