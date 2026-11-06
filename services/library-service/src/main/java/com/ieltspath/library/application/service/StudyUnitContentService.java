package com.ieltspath.library.application.service;

import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.port.StudyUnitRepository;
import com.ieltspath.library.application.result.StudyContentResult.*;
import com.ieltspath.library.domain.study.StudyWordSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.*;

@Service @RequiredArgsConstructor
public class StudyUnitContentService {
    private final StudyUnitRepository repository;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public TopicUnits list(UUID topic) {
        if(!repository.activeTopic(topic)) throw StudyRequestException.missing("VOCAB_TOPIC_NOT_FOUND");
        Set<UUID> unique=new HashSet<>();var summaries=new ArrayList<UnitSummary>();
        var units=repository.list(topic,false);var wordsByUnit=repository.words(units,true);
        for(var unit:units) {
            var words=wordsByUnit.getOrDefault(unit.id(),List.of());words.forEach(w->unique.add(w.vocabularyItemId()));
            summaries.add(summary(unit,words.size()));
        }
        return new TopicUnits(topic,repository.version(topic),summaries.size(),unique.size(),summaries);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public UnitContent get(UUID id) {
        var unit=repository.find(id,false,false).filter(u->u.archivedAt()==null && repository.activeTopic(u.topicId())).orElseThrow(()->StudyRequestException.missing("VOCAB_UNIT_NOT_FOUND"));
        var words=repository.words(id,unit.revision(),true);
        return new UnitContent(summary(unit,words.size()),unit.introductionVi(),words.stream().map(StudyUnitContentService::word).toList());
    }
    public static Word word(StudyWordSnapshot w) {
        return new Word(w.vocabularyItemId(),w.vocabularySenseId(),w.sortOrder(),w.lemma(),w.partOfSpeech(),w.vietnameseMeaning(),w.englishDefinition(),w.ipaUk(),w.ipaUs(),w.audioUkUrl(),w.audioUsUrl(),w.imageUrl(),w.exampleSentence(),w.exampleTranslationVi());
    }
    private UnitSummary summary(StudyUnitRepository.Unit u,int count) { return new UnitSummary(u.id(),u.topicId(),u.revision(),u.title(),u.description(),u.sortOrder(),u.kind(),count,Math.max(1,(int)Math.ceil(count*.75)),u.exerciseTypes()); }
}
