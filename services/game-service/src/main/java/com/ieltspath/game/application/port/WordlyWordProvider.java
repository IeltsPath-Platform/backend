package com.ieltspath.game.application.port;
import java.util.List;
import java.util.UUID;
public interface WordlyWordProvider {
    List<Word> random(int count);
    List<Word> catalog();
    List<Word> catalog(com.ieltspath.game.domain.vo.WordFilter filter);
    Filters filters();
    record Word(UUID sourceWordId,String answer,String meaning){}
    record Topic(UUID id,String slug,String name,String nameVi){}
    /** A null level or topic means the total across all of them. */
    record Availability(String cefrLevel,UUID topicId,int words){}
    record Filters(List<Topic> topics,List<Availability> availability){}
}
