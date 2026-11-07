package com.ieltspath.library.domain.study;
import java.util.*;

/** Ordered, nullable values for the persisted exercise snapshots. */
public final class StudyValues {
    private StudyValues() {}
    public static Map<String,Object> map(Object... values) {
        var result=new LinkedHashMap<String,Object>();
        for(int i=0;i<values.length;i+=2) result.put((String)values[i],values[i+1]);
        return result;
    }
    public static Map<String,Object> word(StudyWordSnapshot w) {
        return map("vocabularyItemId",w.vocabularyItemId(),"vocabularySenseId",w.vocabularySenseId(),"sortOrder",w.sortOrder(),
            "lemma",w.lemma(),"partOfSpeech",w.partOfSpeech(),"vietnameseMeaning",w.vietnameseMeaning(),"englishDefinition",w.englishDefinition(),
            "ipaUk",w.ipaUk(),"ipaUs",w.ipaUs(),"audioUkUrl",w.audioUkUrl(),"audioUsUrl",w.audioUsUrl(),"imageUrl",w.imageUrl(),
            "exampleSentence",w.exampleSentence(),"exampleTranslationVi",w.exampleTranslationVi());
    }
}
