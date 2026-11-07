package com.ieltspath.library.application.service;

import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.port.*;
import com.ieltspath.library.application.port.StudyAttemptRepository.*;
import com.ieltspath.library.application.result.StudyAttemptResult.*;
import com.ieltspath.library.domain.study.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

@Service
public class StudyAttemptService {
    private final StudyUnitRepository units;
    private final StudyAttemptRepository repository;
    private final StudyProgressService progress;
    private final StudyLock lock;
    private final StudyRequestHash hashes;
    private final Clock clock;
    private final TransactionTemplate transactions;
    public StudyAttemptService(StudyUnitRepository units,StudyAttemptRepository repository,StudyProgressService progress,
            StudyLock lock,StudyRequestHash hashes,Clock clock,PlatformTransactionManager manager) {
        this.units=units;this.repository=repository;this.progress=progress;this.lock=lock;this.hashes=hashes;this.clock=clock;this.transactions=new TransactionTemplate(manager);
    }
    public Start start(UUID user,UUID unitId,UUID request,String mode) {
        if(mode==null || !List.of("RESUME_OR_START","REPLAY").contains(mode))throw new StudyRequestException(HttpStatus.BAD_REQUEST,"VOCAB_UNIT_INVALID","Chế độ học không hợp lệ.");
        String hash=hashes.start(unitId,mode);
        return write(user,()->{
            var previous=repository.findStart(user,request);
            if(previous.isPresent()) {
                if(!previous.get().startHash().equals(hash))throw conflict();
                return new Start(view(previous.get()),false);
            }
            var unit=units.find(unitId,false,false).filter(u->u.archivedAt()==null && units.activeTopic(u.topicId())).orElseThrow(()->StudyRequestException.missing("VOCAB_UNIT_NOT_FOUND"));
            var active=repository.active(user,unitId).orElse(null);
            if(active!=null && active.expiresAt().isBefore(clock.instant())){repository.expire(active.id());active=null;}
            var topic=progress.execute(user,List.of(unit.topicId())).getFirst();
            var state=topic.units().stream().filter(u->u.unitId().equals(unitId)).findFirst().orElseThrow(()->StudyRequestException.missing("VOCAB_UNIT_NOT_FOUND"));
            boolean completed=state.status().equals("COMPLETED");
            if(mode.equals("REPLAY") && !completed)throw StudyRequestException.conflict("VOCAB_UNIT_NOT_COMPLETED","Hãy hoàn thành bài trước khi ôn lại.");
            if(active!=null){repository.rememberStart(user,request,active.id(),hash);return new Start(view(active),false);}
            if(state.status().equals("LOCKED"))throw new StudyRequestException(HttpStatus.CONFLICT,"VOCAB_UNIT_LOCKED","Hoàn thành bài trước để tiếp tục.",Map.of("requiredUnitId",state.prerequisiteUnitId().toString()));
            if(!mode.equals("REPLAY") && completed)throw StudyRequestException.conflict("VOCAB_UNIT_COMPLETED","Bài đã hoàn thành. Hãy chọn ôn lại.");
            var words=units.words(unitId,unit.revision(),true);
            if(words.size()<4)throw StudyRequestException.conflict("VOCAB_UNIT_UNAVAILABLE","Bài hiện chưa đủ từ để học.");
            UUID id=UUID.randomUUID();var steps=new ExerciseGenerator().generate(words,units.distractors(unit.topicId()),id);
            int graded=(int)steps.stream().filter(StudyStepSnapshot::graded).count();
            if(graded==0)throw StudyRequestException.conflict("VOCAB_UNIT_UNAVAILABLE","Bài hiện chưa có bài tập phù hợp.");
            Instant now=clock.instant();
            var attempt=new AttemptRow(id,user,unit.topicId(),unitId,unit.revision(),mode.equals("REPLAY")?"REPLAY":"FIRST_COMPLETION","IN_PROGRESS",now,now.plus(Duration.ofDays(14)),request,hash,graded,0,null);
            repository.create(attempt,steps,now);return new Start(view(attempt),true);
        });
    }
    @org.springframework.transaction.annotation.Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Attempt get(UUID user,UUID id){return view(require(user,id));}
    public StepResult answer(UUID user,UUID id,UUID stepId,UUID request,StudyAnswer answer) {
        String hash=hashes.answer(id,stepId,answer);
        return write(user,()->{
            var prior=repository.submission(user,request);
            if(prior.isPresent()){if(!prior.get().hash().equals(hash))throw conflict();return prior.get().result();}
            var attempt=require(user,id);var steps=repository.steps(id);
            var step=steps.stream().filter(s->s.snapshot().stepId().equals(stepId)).findFirst().orElseThrow(()->StudyRequestException.missing("STEP_NOT_FOUND"));
            requireInProgress(attempt);
            var current=steps.stream().filter(s->s.completedAt()==null).findFirst().orElse(null);
            if(current==null || !current.snapshot().stepId().equals(stepId))throw StudyRequestException.conflict("STEP_OUT_OF_ORDER","Hãy tiếp tục đúng bước hiện tại.");
            AnswerGrader.Grade grade;
            try{grade=new AnswerGrader().grade(step.snapshot(),answer);}
            catch(InvalidStudyAnswer e){throw new StudyRequestException(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_ANSWER",e.getMessage(),Map.of("field",e.field()));}
            boolean correct=!grade.outcome().equals("INCORRECT");boolean first=step.firstTryCorrect()==null;
            Instant now=clock.instant();repository.applyAnswer(id,stepId,correct,first,step.snapshot().graded(),now);
            int done=(int)steps.stream().filter(s->s.completedAt()!=null).count()+(correct?1:0);
            UUID next=correct?steps.stream().filter(s->s.completedAt()==null && !s.snapshot().stepId().equals(stepId)).map(s->s.snapshot().stepId()).findFirst().orElse(null):stepId;
            var result=new StepResult(id,stepId,UUID.randomUUID(),grade.outcome(),grade.correctAnswer(),grade.explanationVi(),correct,done,steps.size(),next,done==steps.size(),false,null);
            repository.appendSubmission(user,request,hash,answer,first,result,now);return result;
        });
    }
    public Completion complete(UUID user,UUID id,UUID request) {
        return write(user,()->{
            var attempt=require(user,id);if(attempt.status().equals("COMPLETED"))return attempt.completion();
            requireInProgress(attempt);var steps=repository.steps(id);
            if(steps.stream().anyMatch(s->s.completedAt()==null))throw StudyRequestException.conflict("ATTEMPT_NOT_COMPLETABLE","Hãy hoàn thành tất cả các bước.");
            int accuracy=attempt.gradedCount()==0?0:(int)Math.round(100.0*attempt.firstTryCount()/attempt.gradedCount());Instant now=clock.instant();
            var before=progress.execute(user,List.of(attempt.topicId())).getFirst();
            boolean first=repository.recordProgress(attempt,accuracy,now);
            var after=progress.afterCompletion(user,List.of(attempt.topicId()),id).getFirst();
            int index=-1;for(int i=0;i<after.units().size();i++)if(after.units().get(i).unitId().equals(attempt.unitId()))index=i;
            UUID next=index>=0 && index+1<after.units().size()?after.units().get(index+1).unitId():null;
            boolean unlocked=first && next!=null && before.units().stream().anyMatch(u->u.unitId().equals(next)&&u.status().equals("LOCKED"));
            var result=new Completion(id,attempt.unitId(),attempt.topicId(),now,(int)steps.stream().filter(s->s.snapshot().type().equals("WORD")).count(),attempt.gradedCount(),attempt.firstTryCount(),accuracy,Math.max(0,Duration.between(attempt.startedAt(),now).toSeconds()),first,after,unlocked?List.of(next):List.of(),next,null);
            repository.complete(id,result,now);return result;
        });
    }
    private AttemptRow require(UUID user,UUID id){return repository.find(user,id).orElseThrow(()->StudyRequestException.missing("ATTEMPT_NOT_FOUND"));}
    private void requireInProgress(AttemptRow a) {
        if(a.status().equals("IN_PROGRESS") && a.expiresAt().isBefore(clock.instant())){repository.expire(a.id());throw StudyRequestException.conflict("ATTEMPT_EXPIRED","Phiên học đã hết hạn. Hãy bắt đầu lại.");}
        if(a.status().equals("EXPIRED"))throw StudyRequestException.conflict("ATTEMPT_EXPIRED","Phiên học đã hết hạn. Hãy bắt đầu lại.");
        if(a.status().equals("COMPLETED"))throw StudyRequestException.conflict("ATTEMPT_ALREADY_COMPLETED","Phiên học đã hoàn thành.");
    }
    private Attempt view(AttemptRow a) {
        var steps=repository.steps(a.id());var complete=steps.stream().filter(s->s.completedAt()!=null).map(s->s.snapshot().stepId()).toList();
        UUID current=a.status().equals("COMPLETED")?null:steps.stream().filter(s->s.completedAt()==null).map(s->s.snapshot().stepId()).findFirst().orElse(null);
        return new Attempt(a.id(),a.topicId(),a.unitId(),a.revision(),a.status(),a.mode(),a.startedAt(),a.expiresAt(),current,complete,steps.stream().map(s->s.snapshot().payload()).toList(),a.completion());
    }
    private StudyRequestException conflict(){return StudyRequestException.conflict("IDEMPOTENCY_CONFLICT","requestId đã được dùng với dữ liệu khác.");}
    private record Outcome<T>(T value,StudyRequestException error) {}
    private <T>T write(UUID user,Supplier<T> action) {
        // Business errors are returned after commit so an expiry transition is not rolled back by its 409 response.
        Outcome<T> outcome=transactions.execute(status->{lock.lock(user);try{return new Outcome<>(action.get(),null);}catch(StudyRequestException e){return new Outcome<>(null,e);}});
        if(outcome.error()!=null)throw outcome.error();return outcome.value();
    }
}
