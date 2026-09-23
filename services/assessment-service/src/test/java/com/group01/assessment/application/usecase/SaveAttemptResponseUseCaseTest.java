package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.SaveAttemptResponseCommand;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.AttemptResponse;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.*;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant; import java.util.Optional; import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveAttemptResponseUseCaseTest {
 @Mock AssessmentAttemptRepository attempts; @Mock AttemptItemRepository items; @Mock AttemptResponseRepository responses;
 @Test void savesNextRevision(){UUID user=UUID.randomUUID(),attemptId=UUID.randomUUID(),itemId=UUID.randomUUID(); var attempt=AssessmentAttempt.start(user,UUID.randomUUID(),AttemptType.QUIZ,AttemptMode.STANDARD,AttemptChannel.WEB,null); when(attempts.findByIdAndUserId(attemptId,user)).thenReturn(Optional.of(new AssessmentAttempt(attemptId,user,attempt.getPackageVersionId(),attempt.getAttemptType(),attempt.getMode(),attempt.getChannel(),AttemptStatus.IN_PROGRESS,attempt.getStartedAt(),null,null,0,attempt.getCreatedAt(),attempt.getUpdatedAt()))); when(items.findByIdAndAttemptId(itemId,attemptId)).thenReturn(Optional.of(new AttemptItem(itemId,UUID.randomUUID(),UUID.randomUUID(),1,"{}",null,null))); when(responses.findByAttemptItemId(itemId)).thenReturn(Optional.of(new AttemptResponse(UUID.randomUUID(),itemId,"{}",1,2,Instant.now(),null))); when(responses.save(any())).thenAnswer(i->i.getArgument(0)); var result=new SaveAttemptResponseUseCase(attempts,items,responses).execute(new SaveAttemptResponseCommand(user,attemptId,itemId,"{\"answer\":\"A\"}",1,2)); assertEquals(3,result.revision()); verify(responses).save(any()); }
}
