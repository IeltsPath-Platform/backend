package com.ieltspath.library.domain.study;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ExerciseGeneratorTest {
    List<StudyWordSnapshot> words(int count) {
        var result=new ArrayList<StudyWordSnapshot>();
        for(int i=0;i<count;i++) result.add(StudyUnitRulesTest.word("word"+i,"nghĩa "+i));
        return result;
    }
    @Test void deterministicOrderedExercisesAndNoAnswerLeak() {
        var id=UUID.randomUUID();var words=words(5);var generator=new ExerciseGenerator();
        var steps=generator.generate(words,List.of(),id);
        assertEquals(steps,generator.generate(words,List.of(),id));
        assertEquals(16,steps.size());
        assertEquals(List.of("WORD","WORD","WORD","WORD","WORD"),steps.subList(0,5).stream().map(StudyStepSnapshot::type).toList());
        assertEquals(11,steps.stream().filter(StudyStepSnapshot::graded).count());
        for(int i=0;i<steps.size();i++) {
            var s=steps.get(i);assertEquals(i+1,s.order());
            assertFalse(s.payload().containsKey("answerKey"));assertFalse(s.payload().containsKey("correctOptionId"));
            if(s.type().equals("CHOICE")) {
                var options=(List<Map<String,Object>>)s.payload().get("options");
                assertTrue(options.size()>=2 && options.size()<=4);
                assertEquals(options.size(),options.stream().map(o->StudyText.normalize((String)o.get("text"))).distinct().count());
            }
        }
    }
    @Test void matchGroupsNeverHaveSinglePairAndIdsDoNotRevealPairs() {
        for(int count=2;count<=60;count++) for(var step:new ExerciseGenerator().generate(words(count),List.of(),UUID.randomUUID())) {
            if(!step.type().equals("MATCH"))continue;
            var left=(List<Map<String,Object>>)step.payload().get("left");var right=(List<Map<String,Object>>)step.payload().get("right");
            assertTrue(left.size()>=2 && left.size()<=5);
            assertTrue(Collections.disjoint(left.stream().map(o->o.get("optionId")).toList(),right.stream().map(o->o.get("optionId")).toList()));
        }
    }
    @Test void fillRespectsUnicodeBoundariesMultiwordAndQuotedPunctuation() {
        var w=StudyUnitRulesTest.word("art","nghệ thuật");
        w=new StudyWordSnapshot(w.vocabularyItemId(),w.vocabularySenseId(),1,"art","NOUN",w.vietnameseMeaning(),null,null,null,null,null,null,"A party.",null,true);
        assertTrue(new ExerciseGenerator().generate(List.of(w),List.of(),UUID.randomUUID()).stream().noneMatch(s->s.type().equals("FILL")));
        assertTrue(StudyText.lemmaPattern("ice cream").matcher("ICE   CREAM is cold.").find());
        assertTrue(StudyText.lemmaPattern("C++").matcher("Use C++ today.").find());
    }
    @Test void twoMeaningsProduceTwoOptionsAndInsufficientMeaningsSkipChoice() {
        var steps=new ExerciseGenerator().generate(words(2),List.of(),UUID.randomUUID());
        assertTrue(steps.stream().filter(s->s.type().equals("CHOICE")).allMatch(s->((List<?>)s.payload().get("options")).size()==2));
        assertTrue(new ExerciseGenerator().generate(words(1),List.of(),UUID.randomUUID()).stream().noneMatch(s->s.type().equals("CHOICE")));
    }
}
