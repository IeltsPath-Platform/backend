package com.ieltspath.library.application.service;

import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.port.*;
import com.ieltspath.library.application.port.StudyUnitRepository.*;
import com.ieltspath.library.application.result.StudyContentResult.*;
import com.ieltspath.library.domain.study.StudyUnitRules;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.ieltspath.library.domain.study.StudyWordSnapshot;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudyUnitAuthoringService {
    private final StudyUnitRepository repository;
    private final ExercisePreview exercises;
    private final Clock clock;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<AuthorUnit> list(UUID topic) {
        requireTopic(topic);var units=repository.list(topic,true);var words=repository.words(units,false);
        return units.stream().map(u->view(u,words.getOrDefault(u.id(),List.of()))).toList();
    }
    @Transactional public AuthorUnit create(UUID topic,UUID author,Draft draft) {
        requireTopic(topic);validateDraft(draft);repository.lockTopic(topic);
        UUID id=repository.create(topic,author,draft,clock.instant());return view(require(id,false));
    }
    @Transactional public AuthorUnit update(UUID id,UUID author,Draft draft) {
        validateDraft(draft);var unit=require(id,true);requireLive(unit);
        repository.update(unit,author,draft,clock.instant());return view(require(id,false));
    }
    public Validation validate(UUID id) { return validation(require(id,false)); }
    @Transactional public AuthorUnit publish(UUID id) {
        var unit=require(id,true);requireLive(unit);
        if(unit.draftRevision()==null) throw StudyRequestException.conflict("VOCAB_UNIT_NO_DRAFT","Bài chưa có bản nháp để phát hành.");
        var report=validation(unit);
        if(!report.canPublish()) {
            var details=new LinkedHashMap<String,String>(); for(int i=0;i<report.errors().size();i++) details.put("errors["+i+"]",report.errors().get(i).code());
            throw new StudyRequestException(HttpStatus.UNPROCESSABLE_ENTITY,"VOCAB_UNIT_INVALID","Bài học chưa đủ điều kiện phát hành.",details);
        }
        repository.publish(unit,report.preview()==null?null:report.preview().exerciseTypes(),report.preview()==null?null:report.preview().gradedStepCount(),clock.instant());
        repository.bumpVersion(unit.topicId(),clock.instant());return view(require(id,false));
    }
    @Transactional public AuthorUnit archive(UUID id) {
        var unit=require(id,true);requireLive(unit);repository.archive(id,clock.instant());repository.bumpVersion(unit.topicId(),clock.instant());return view(require(id,false));
    }
    @Transactional public List<AuthorUnit> reorder(UUID topic,List<UUID> ids) {
        requireTopic(topic);repository.lockTopic(topic);
        var current=repository.list(topic,true).stream().filter(u->u.archivedAt()==null).map(Unit::id).toList();
        if(ids.size()!=new HashSet<>(ids).size() || !new HashSet<>(ids).equals(new HashSet<>(current))) throw new StudyRequestException(HttpStatus.BAD_REQUEST,"VOCAB_UNIT_INVALID","unitIds phải chứa đúng các bài chưa lưu trữ.",Map.of("field","unitIds"));
        repository.reorder(ids,clock.instant());repository.bumpVersion(topic,clock.instant());return list(topic);
    }
    private Validation validation(Unit unit) {
        var words=repository.words(unit.id(),unit.revision(),false);var pool=repository.distractors(unit.topicId());
        var rules=StudyUnitRules.validate(words,pool);var preview=exercises.preview(words.stream().filter(w->w.active()).toList(),pool);
        var errors=new ArrayList<>(rules.errors());
        if(preview!=null && preview.gradedStepCount()==0) errors.add(new StudyUnitRules.Issue("NO_GRADED_STEP","Không tạo được bài tập chấm điểm.",null));
        return new Validation(unit.id(),unit.revision(),errors.isEmpty(),errors,rules.warnings(),preview);
    }
    private void validateDraft(Draft d) {
        if(d.title()==null || d.title().isBlank() || d.title().length()>120 || d.description()!=null && d.description().length()>500 || d.introductionVi()==null || d.introductionVi().isBlank() || d.kind()==null || !List.of("LEARN","REVIEW").contains(d.kind()) || d.senseIds()==null || d.senseIds().size()<4 || d.senseIds().size()>60 || new HashSet<>(d.senseIds()).size()!=d.senseIds().size() || d.senseIds().stream().anyMatch(Objects::isNull))
            throw new StudyRequestException(HttpStatus.BAD_REQUEST,"VOCAB_UNIT_INVALID","Nội dung bài học không hợp lệ; cần 4–60 nghĩa khác nhau.",Map.of("field","senseIds"));
        if(!repository.sensesExist(d.senseIds())) throw new StudyRequestException(HttpStatus.BAD_REQUEST,"VOCAB_UNIT_INVALID","Một số nghĩa từ vựng không tồn tại.",Map.of("field","senseIds"));
    }
    private Unit require(UUID id,boolean lock) {
        var unit=repository.find(id,true,false).orElseThrow(()->StudyRequestException.missing("VOCAB_UNIT_NOT_FOUND"));
        // All author writes lock topic before unit, including reorder, to avoid opposite row-lock ordering.
        if(lock) { repository.lockTopic(unit.topicId());return repository.find(id,true,true).orElseThrow(()->StudyRequestException.missing("VOCAB_UNIT_NOT_FOUND")); }
        return unit;
    }
    private void requireTopic(UUID topic) { if(!repository.activeTopic(topic)) throw StudyRequestException.missing("VOCAB_TOPIC_NOT_FOUND"); }
    private void requireLive(Unit u) { if(u.archivedAt()!=null) throw StudyRequestException.conflict("VOCAB_UNIT_ARCHIVED","Bài đã được lưu trữ."); }
    private AuthorUnit view(Unit u) {
        return view(u,repository.words(u.id(),u.revision(),false));
    }
    private AuthorUnit view(Unit u,List<StudyWordSnapshot> snapshots) {
        String status=u.archivedAt()!=null?"ARCHIVED":u.publishedRevision()==null?"DRAFT":u.draftRevision()==null?"PUBLISHED":"PUBLISHED_WITH_DRAFT";
        var words=snapshots.stream().map(w->new AuthorWord(w.vocabularySenseId(),w.lemma(),w.vietnameseMeaning(),w.sortOrder())).toList();
        return new AuthorUnit(u.id(),u.topicId(),u.sortOrder(),u.kind(),status,u.publishedRevision(),u.draftRevision(),new Revision(u.revision(),u.title(),u.description(),u.introductionVi(),words),u.updatedAt());
    }
}
