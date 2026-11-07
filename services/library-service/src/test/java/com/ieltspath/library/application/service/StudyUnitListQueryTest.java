package com.ieltspath.library.application.service;

import com.ieltspath.library.application.port.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StudyUnitListQueryTest {
    @Test void publicAndAuthorListsNeverFetchWordsSeparatelyForEachUnit() {
        var repository=mock(StudyUnitRepository.class);UUID topic=UUID.randomUUID();
        var units=new ArrayList<StudyUnitRepository.Unit>();
        for(int i=0;i<20;i++) units.add(new StudyUnitRepository.Unit(UUID.randomUUID(),topic,i+1,"LEARN",1,null,null,Instant.EPOCH,1,"Title",null,"Intro",List.of(),null));
        when(repository.activeTopic(topic)).thenReturn(true);
        when(repository.list(eq(topic),anyBoolean())).thenReturn(units);
        new StudyUnitContentService(repository).list(topic);
        new StudyUnitAuthoringService(repository,mock(ExercisePreview.class),Clock.systemUTC()).list(topic);
        verify(repository,never()).words(any(UUID.class),anyInt(),anyBoolean());
    }
}
