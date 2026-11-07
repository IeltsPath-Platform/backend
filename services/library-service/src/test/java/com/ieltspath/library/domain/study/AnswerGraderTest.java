package com.ieltspath.library.domain.study;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnswerGraderTest {
    @Test void normalizesFillButPreservesPunctuationAndAccents() {
        var s=new StudyStepSnapshot(UUID.randomUUID(),1,"FILL",true,Map.of(),Map.of("accepted",List.of("café au lait"),"explanationVi","Ví dụ"));
        var grader=new AnswerGrader();
        assertEquals("CORRECT",grader.grade(s,new StudyAnswer("FILL_TEXT",null,null,null," CAFE\u0301  AU LAIT ")).outcome());
        assertEquals("INCORRECT",grader.grade(s,new StudyAnswer("FILL_TEXT",null,null,null,"cafe au lait")).outcome());
        assertThrows(InvalidStudyAnswer.class,()->grader.grade(s,new StudyAnswer("FILL_TEXT",null,null,null," ")));
        assertEquals("CORRECT",grader.grade(s,new StudyAnswer("FILL_TEXT",null,null,null,"\u00a0CAFE\u0301\u202fAU LAIT\u00a0")).outcome());
        assertThrows(InvalidStudyAnswer.class,()->grader.grade(s,new StudyAnswer("FILL_TEXT",null,null,null,"\u00a0\u202f")));
    }
    @Test void matchValidatesFullBijectionAndGradesWrongPairs() {
        UUID l1=UUID.randomUUID(),l2=UUID.randomUUID(),r1=UUID.randomUUID(),r2=UUID.randomUUID();
        var pairs=List.of(new StudyAnswer.Pair(l1,r1),new StudyAnswer.Pair(l2,r2));
        var s=new StudyStepSnapshot(UUID.randomUUID(),1,"MATCH",true,Map.of("left",List.of(Map.of("optionId",l1),Map.of("optionId",l2)),"right",List.of(Map.of("optionId",r1),Map.of("optionId",r2))),Map.of("pairs",pairs));
        var grader=new AnswerGrader();
        assertEquals("CORRECT",grader.grade(s,new StudyAnswer("MATCH",null,null,pairs,null)).outcome());
        assertEquals("INCORRECT",grader.grade(s,new StudyAnswer("MATCH",null,null,List.of(new StudyAnswer.Pair(l1,r2),new StudyAnswer.Pair(l2,r1)),null)).outcome());
        assertThrows(InvalidStudyAnswer.class,()->grader.grade(s,new StudyAnswer("MATCH",null,null,pairs.subList(0,1),null)));
        assertThrows(InvalidStudyAnswer.class,()->grader.grade(s,new StudyAnswer("MATCH",null,null,List.of(pairs.getFirst(),pairs.getFirst()),null)));
    }
}
