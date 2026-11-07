package com.ieltspath.library.domain.study;

import java.util.*;
import static com.ieltspath.library.domain.study.StudyValues.map;

public final class AnswerGrader {
    public record Grade(String outcome,Map<String,Object> correctAnswer,String explanationVi) {}
    public Grade grade(StudyStepSnapshot step,StudyAnswer answer) {
        if(answer==null || answer.type()==null)throw new InvalidStudyAnswer("answer.type");
        var key=step.answerKey();Map<String,Object> correct;boolean valid;
        switch(step.type()) {
            case "WORD":
                require(answer.type().equals("WORD") && Boolean.TRUE.equals(answer.acknowledged()),"answer.acknowledged");
                return new Grade("ACKNOWLEDGED",null,null);
            case "CHOICE":
                require(answer.type().equals("CHOICE"),"answer.type");
                require(answer.optionId()!=null && ids(step.payload().get("options")).contains(answer.optionId().toString()),"answer.optionId");
                valid=answer.optionId().toString().equals(key.get("correctOptionId").toString());
                correct=map("type","CHOICE","optionId",key.get("correctOptionId"));break;
            case "MATCH":
                require(answer.type().equals("MATCH"),"answer.type");
                var left=ids(step.payload().get("left"));var right=ids(step.payload().get("right"));
                require(answer.pairs()!=null && answer.pairs().size()==left.size(),"answer.pairs");
                Set<String> seenLeft=new HashSet<>(),seenRight=new HashSet<>();
                for(var p:answer.pairs()) {
                    require(p!=null && p.leftOptionId()!=null && p.rightOptionId()!=null,"answer.pairs");
                    require(left.contains(p.leftOptionId().toString()) && right.contains(p.rightOptionId().toString()) && seenLeft.add(p.leftOptionId().toString()) && seenRight.add(p.rightOptionId().toString()),"answer.pairs");
                }
                valid=pairSet(answer.pairs()).equals(pairSet((List<?>)key.get("pairs")));
                correct=map("type","MATCH","pairs",key.get("pairs"));break;
            case "FILL":
                require(answer.type().equals("FILL_TEXT"),"answer.type");
                String normalized=StudyText.normalize(answer.text());
                require(answer.text()!=null && !normalized.isEmpty() && normalized.length()<=100,"answer.text");
                var accepted=(List<?>)key.get("accepted");valid=accepted.stream().anyMatch(s->StudyText.normalize(s.toString()).equals(StudyText.normalize(answer.text())));
                correct=map("type","FILL_TEXT","text",accepted.getFirst());break;
            default:throw new InvalidStudyAnswer("answer.type");
        }
        return new Grade(valid?"CORRECT":"INCORRECT",correct,(String)key.get("explanationVi"));
    }
    private static void require(boolean value,String field){if(!value)throw new InvalidStudyAnswer(field);}
    private static Set<String> ids(Object options){var result=new HashSet<String>();for(var o:(List<?>)options)result.add(((Map<?,?>)o).get("optionId").toString());return result;}
    private static Set<String> pairSet(List<?> pairs) {
        Set<String> result=new HashSet<>();for(var pair:pairs) {
            if(pair instanceof StudyAnswer.Pair p)result.add(p.leftOptionId()+":"+p.rightOptionId());
            else {var p=(Map<?,?>)pair;result.add(p.get("leftOptionId")+":"+p.get("rightOptionId"));}
        }return result;
    }
}
