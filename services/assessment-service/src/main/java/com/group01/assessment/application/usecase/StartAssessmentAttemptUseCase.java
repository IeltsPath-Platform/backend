package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.AttemptSection;
import com.group01.assessment.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class StartAssessmentAttemptUseCase {
    private final AssessmentAttemptRepository attempts; private final AttemptSectionRepository sections; private final AttemptItemRepository items;
    public StartAssessmentAttemptUseCase(AssessmentAttemptRepository attempts, AttemptSectionRepository sections, AttemptItemRepository items){this.attempts=attempts;this.sections=sections;this.items=items;}
    @Transactional
    public AssessmentAttemptResult execute(StartAssessmentAttemptCommand c){
        var attempt=AssessmentAttempt.start(c.userId(),c.packageVersionId(),c.attemptType(),c.mode(),c.channel(),c.expiresAt());
        attempts.save(attempt);
        var sectionEntities=new ArrayList<AttemptSection>(); var itemEntities=new ArrayList<AttemptItem>();
        var sectionOrders=new HashSet<Integer>();
        for(var s:c.sections()==null?List.<StartAssessmentAttemptCommand.SectionInput>of():c.sections()){
            if(!sectionOrders.add(s.sortOrder())) throw new com.group01.assessment.domain.exception.InvalidAssessmentStateException("Duplicate section sort order");
            var sectionId=UUID.randomUUID(); sectionEntities.add(new AttemptSection(sectionId,attempt.getId(),s.contentSectionId(),s.sortOrder(),s.snapshot()));
            var itemOrders=new HashSet<Integer>();
            for(var i:s.items()==null?List.<StartAssessmentAttemptCommand.ItemInput>of():s.items()) { if(!itemOrders.add(i.sortOrder())) throw new com.group01.assessment.domain.exception.InvalidAssessmentStateException("Duplicate item sort order"); itemEntities.add(new AttemptItem(UUID.randomUUID(),sectionId,i.questionVersionId(),i.sortOrder(),i.questionSnapshot(),i.answerSnapshot(),i.knowledgeSnapshot())); }
        }
        sections.saveAll(sectionEntities); items.saveAll(itemEntities);
        return result(attempt);
    }
    private AssessmentAttemptResult result(AssessmentAttempt a){return new AssessmentAttemptResult(a.getId(),a.getUserId(),a.getPackageVersionId(),a.getAttemptType(),a.getMode(),a.getChannel(),a.getStatus(),a.getStartedAt(),a.getSubmittedAt(),a.getExpiresAt(),a.getRowVersion(),a.getCreatedAt(),a.getUpdatedAt());}
}
