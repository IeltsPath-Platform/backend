package com.ieltspath.game.application.port;
import com.ieltspath.game.application.result.WordlyHintResult;
import java.util.*;
import java.time.Instant;
public interface WordlyHintRepository {
 Optional<Context> lock(UUID matchId,UUID playerWordId,UUID userId);
 List<Guess> guesses(UUID playerWordId);
 void save(UUID wordId,UUID requestId,UUID sourceGuessId,int column,String letter,Instant now);
 record Guess(UUID id,String guess,List<String> letters){}
 record Context(UUID id,String answer,String wordStatus,String playerStatus,String matchStatus,Instant deadline,
   UUID requestId,UUID sourceGuessId,Integer column,String letter){}
}
