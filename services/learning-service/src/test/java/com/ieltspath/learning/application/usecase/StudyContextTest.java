package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.*;
import com.ieltspath.learning.application.service.*;
import com.ieltspath.learning.domain.aggregate.*;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.*;
import com.ieltspath.learning.domain.vo.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudyContextTest {
    final UUID user=UUID.randomUUID(), id=UUID.randomUUID(), lessonId=UUID.randomUUID(), topicId=UUID.randomUUID(), courseId=UUID.randomUUID();
    final LearnerLock lock=mock(LearnerLock.class);
    final LessonAccess lessons=mock(LessonAccess.class);
    final PracticeAccess practice=mock(PracticeAccess.class);
    final PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
    final ReviewItemRepository reviews=mock(ReviewItemRepository.class);
    final LearningContentClient content=mock(LearningContentClient.class);
    final GetStudyContextUseCase useCase=new GetStudyContextUseCase(lock,lessons,practice,attempts,reviews,content);
    final LearningContentClient.Lesson lesson=new LearningContentClient.Lesson(lessonId,topicId,"L","Lesson",null,1,List.of(),List.of(),LearningSkill.READING);

    @Test void lessonUsesExistingGateAndTrustedCourse() {
        when(lessons.authorizeToRead(user,lessonId)).thenReturn(new LessonAccess.Context(lesson,null,true,java.util.Set.of(),List.of()));
        course();
        var result=useCase.execute(user,"LESSON",lessonId);
        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.sourceId()).isEqualTo(lessonId);
        verify(lock).lock(user);
        verify(lessons).authorizeToRead(user,lessonId);
    }

    @Test void practiceAndReviewRejectSourcesOwnedByAnotherUserBeforeContentRead() {
        assertThatThrownBy(() -> useCase.execute(user,"PRACTICE",id)).isInstanceOfSatisfying(LearningRequestException.class,e -> assertThat(e.getStatus()).isEqualTo(404));
        assertThatThrownBy(() -> useCase.execute(user,"REVIEW",id)).isInstanceOfSatisfying(LearningRequestException.class,e -> assertThat(e.getStatus()).isEqualTo(404));
        verifyNoInteractions(content,lessons,practice);
    }

    @Test void pendingReviewUsesOwnerGateWithoutRequiringTheLessonItBlocks() {
        when(reviews.findOwned(user,id)).thenReturn(Optional.of(ReviewItem.restore(id,user,UUID.randomUUID(),lessonId,ReviewStatus.PENDING,null,0)));
        when(content.getLesson(lessonId)).thenReturn(lesson);
        course();
        assertThat(useCase.execute(user,"REVIEW",id).courseId()).isEqualTo(courseId);
        verify(lessons,never()).authorize(any(UUID.class),any(UUID.class));
    }

    @Test void submittedPracticeAndFinishedReviewAreInactive() {
        when(attempts.findOwned(user,id)).thenReturn(Optional.of(PracticeAttempt.restore(id,user,lessonId,LearningSkill.READING,UUID.randomUUID(),UUID.randomUUID(),Instant.now(),Instant.now(),UUID.randomUUID(),null)));
        when(reviews.findOwned(user,id)).thenReturn(Optional.of(ReviewItem.restore(id,user,UUID.randomUUID(),lessonId,ReviewStatus.DONE,null,0)));
        for(String type:List.of("PRACTICE","REVIEW")) assertThatThrownBy(() -> useCase.execute(user,type,id))
                .isInstanceOfSatisfying(LearningRequestException.class,e -> assertThat(e.getStatus()).isEqualTo(409));
        verifyNoInteractions(content,practice);
    }

    @Test void activePracticeChecksPracticeAccess() {
        when(attempts.findOwned(user,id)).thenReturn(Optional.of(PracticeAttempt.start(id,user,lessonId,LearningSkill.READING,UUID.randomUUID(),UUID.randomUUID(),Instant.now())));
        when(practice.require(user,lessonId)).thenReturn(lesson);
        course();
        assertThat(useCase.execute(user,"PRACTICE",id).courseId()).isEqualTo(courseId);
        verify(practice).require(user,lessonId);
    }
    void course(){when(lessons.topic(user,topicId)).thenReturn(new TopicProgress(topicId,courseId,1,null,LearningSkill.READING,true));}
}
