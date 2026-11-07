package com.ieltspath.library.domain.study;

import java.nio.charset.StandardCharsets;
import java.util.*;
import static com.ieltspath.library.domain.study.StudyValues.*;

/** The attempt seed determines every step, option id and shuffle; answer keys never enter learner payloads. */
public final class ExerciseGenerator {
    public List<StudyStepSnapshot> generate(List<StudyWordSnapshot> words,List<StudyWordSnapshot> pool,UUID attemptId) {
        var random=new SplittableRandom(attemptId.getMostSignificantBits()^attemptId.getLeastSignificantBits());
        var steps=new ArrayList<StudyStepSnapshot>();
        for(var w:words) add(steps,attemptId,"WORD",map("word",word(w)),map(),w.imageUrl(),w.audioUsUrl()!=null?w.audioUsUrl():w.audioUkUrl());
        // Authoring bounds a unit to 60 words and its topic pool to 200, keeping distractor selection bounded.
        for(var w:shuffle(words,random)) {
            var meanings=new LinkedHashMap<String,String>();
            meanings.put(StudyText.normalize(w.vietnameseMeaning()),w.vietnameseMeaning());
            for(var other:shuffle(words,random)) addMeaning(meanings,other.vietnameseMeaning());
            for(var other:shuffle(pool,random)) addMeaning(meanings,other.vietnameseMeaning());
            if(meanings.size()<2)continue;
            var options=new ArrayList<Map<String,Object>>();UUID correct=null;
            for(String meaning:meanings.values()) {
                UUID id=id(random);if(correct==null)correct=id;
                options.add(map("optionId",id,"text",meaning,"imageUrl",null));
            }
            add(steps,attemptId,"CHOICE",map("prompt",w.lemma(),"options",shuffle(options,random)),
                map("correctOptionId",correct,"explanationVi","'"+w.lemma()+"' nghĩa là '"+w.vietnameseMeaning()+"'."),null,null);
        }
        var matching=shuffle(words,random);
        for(int from=0;from<matching.size();) {
            int remaining=matching.size()-from;if(remaining<2)break;
            int size=remaining==6?4:Math.min(5,remaining);
            var left=new ArrayList<Map<String,Object>>();var right=new ArrayList<Map<String,Object>>();var pairs=new ArrayList<StudyAnswer.Pair>();
            var explanations=new ArrayList<String>();
            for(var w:matching.subList(from,from+size)) {
                UUID l=id(random),r=id(random);left.add(map("optionId",l,"text",w.lemma()));right.add(map("optionId",r,"text",w.vietnameseMeaning()));
                pairs.add(new StudyAnswer.Pair(l,r));explanations.add(w.lemma()+": "+w.vietnameseMeaning());
            }
            add(steps,attemptId,"MATCH",map("left",left,"right",shuffle(right,random)),map("pairs",pairs,"explanationVi",String.join("; ",explanations)),null,null);from+=size;
        }
        for(var w:shuffle(words,random)) {
            if(w.exampleSentence()==null)continue;
            var match=StudyText.lemmaPattern(w.lemma()).matcher(w.exampleSentence());if(!match.find())continue;
            var accepted=new LinkedHashSet<String>();accepted.add(StudyText.normalize(match.group()));accepted.add(StudyText.normalize(w.lemma()));
            add(steps,attemptId,"FILL",map("prompt",w.vietnameseMeaning(),"sentenceBefore",w.exampleSentence().substring(0,match.start()),
                "sentenceAfter",w.exampleSentence().substring(match.end()),"inputMode","TEXT","options",null),
                map("accepted",List.copyOf(accepted),"explanationVi",w.exampleSentence()+(w.exampleTranslationVi()==null?"":" — "+w.exampleTranslationVi())),null,null);
        }
        return List.copyOf(steps);
    }
    private void addMeaning(Map<String,String> meanings,String value) {
        if(meanings.size()<4 && !StudyText.normalize(value).isEmpty())meanings.putIfAbsent(StudyText.normalize(value),value);
    }
    private void add(List<StudyStepSnapshot> steps,UUID attempt,String type,Map<String,Object> fields,Map<String,Object> key,String image,String audio) {
        int order=steps.size()+1;UUID id=UUID.nameUUIDFromBytes((attempt+":"+order).getBytes(StandardCharsets.UTF_8));
        String instruction=switch(type){case "WORD"->"Học từ mới";case "CHOICE"->"Chọn nghĩa đúng của từ";case "MATCH"->"Ghép từ với nghĩa";default->"Điền từ vào chỗ trống";};
        var payload=map("stepId",id,"order",order,"type",type,"instructionVi",instruction,"imageUrl",image,"audioUrl",audio);payload.putAll(fields);
        steps.add(new StudyStepSnapshot(id,order,type,!type.equals("WORD"),payload,key));
    }
    private UUID id(SplittableRandom random){return new UUID(random.nextLong(),random.nextLong());}
    private <T> List<T> shuffle(List<T> source,SplittableRandom random) {
        var values=new ArrayList<>(source);for(int i=values.size()-1;i>0;i--){int j=random.nextInt(i+1);Collections.swap(values,i,j);}return values;
    }
}
