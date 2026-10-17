package com.ieltspath.game.application.port;
import java.util.List;
import java.util.UUID;
public interface WordlyWordProvider {
    List<Word> random(int count);
    List<Word> catalog();
    record Word(UUID sourceWordId,String answer,String meaning){}
}
