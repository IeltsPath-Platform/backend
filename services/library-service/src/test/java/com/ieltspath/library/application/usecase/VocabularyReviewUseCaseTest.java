package com.ieltspath.library.application.usecase;

import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import com.ieltspath.library.domain.repository.VocabularyReviewRepository;
import com.ieltspath.library.domain.exception.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VocabularyReviewUseCaseTest {
    private final VocabularyReviewRepository repo=mock(VocabularyReviewRepository.class);
    private final VocabularyReviewUseCase useCase=new VocabularyReviewUseCase(repo);
    private final UUID user=UUID.randomUUID(),sense=UUID.randomUUID(),session=UUID.randomUUID(),request=UUID.randomUUID();
    @Test void lostResponseReplayReturnsOriginalSnapshotWithoutMutatingProgress() {
        Instant now=Instant.now();
        when(repo.findReceipt(user,request)).thenReturn(Optional.of(new VocabularyReviewRepository.Receipt(user,request,session,sense,
                0,VocabularyProgress.Outcome.REMEMBERED,VocabularyProgress.State.L1,1,now.plusSeconds(86400),now)));
        var result=useCase.answer(user,session,request,sense,0,VocabularyProgress.Outcome.REMEMBERED);
        assertEquals(1,result.stateVersion());assertEquals(VocabularyProgress.State.L1,result.state());
        verify(repo,never()).save(any());verify(repo,never()).saveReceipt(any());
        assertThrows(ConflictException.class,()->useCase.answer(user,session,request,sense,0,VocabularyProgress.Outcome.FORGOT));
    }
    @Test void sessionOfAnotherUserCannotReadOrWriteProgress() {
        when(repo.findSession(user,session)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->useCase.answer(user,session,request,sense,0,VocabularyProgress.Outcome.REMEMBERED));
        verify(repo,never()).findProgress(any(),any());
    }
    @Test void expiredSessionCannotBeAnswered() {
        Instant old=Instant.now().minusSeconds(3600);
        when(repo.findSession(user,session)).thenReturn(Optional.of(new VocabularyReviewRepository.Session(session,user,request,20,old,old.plusSeconds(1800),
                List.of(new VocabularyReviewRepository.QueueItem(sense,0,"word","meaning","example",false)))));
        assertThrows(ConflictException.class,()->useCase.answer(user,session,UUID.randomUUID(),sense,0,VocabularyProgress.Outcome.REMEMBERED));
        verify(repo,never()).save(any());
    }
    @Test void summaryIncludesAllStatesAndRoundsRetainedPercentage() {
        when(repo.summarize(eq(user),any())).thenReturn(new VocabularyReviewRepository.Summary(List.of(
                new VocabularyReviewRepository.StateCount(VocabularyProgress.State.NEW,91,0,Instant.now()),
                new VocabularyReviewRepository.StateCount(VocabularyProgress.State.RETAINED,110,25,Instant.now()))));
        var summary=useCase.statistics(user);
        assertEquals(201,summary.total());assertEquals(54.7,summary.retainedPercent());assertEquals(25,summary.dueCount());
        assertEquals(5,summary.buckets().size());assertEquals("SELF_RATED",summary.recallSource());
    }
    @Test void emptyVocabularyHasNoRetainedPercentage() {
        when(repo.summarize(eq(user),any())).thenReturn(new VocabularyReviewRepository.Summary(List.of()));
        var summary=useCase.statistics(user);
        assertEquals(0,summary.total());assertNull(summary.retainedPercent());
    }
    @Test void addingSameSenseKeepsItsVersionAndProgress() {
        var existing=VocabularyProgress.start(user,sense,Instant.now());
        when(repo.findProgress(user,sense)).thenReturn(Optional.of(existing));when(repo.isAvailableSense(sense)).thenReturn(true);
        assertEquals(0,useCase.study(user,sense,true).stateVersion());verify(repo).save(existing);
    }
}
