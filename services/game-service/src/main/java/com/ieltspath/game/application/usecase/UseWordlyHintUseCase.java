package com.ieltspath.game.application.usecase;
import com.ieltspath.game.application.port.WordlyHintRepository;
import com.ieltspath.game.application.result.WordlyHintResult;
import com.ieltspath.game.domain.vo.WordlyHintSelector;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
@Service
public class UseWordlyHintUseCase {
 private final WordlyHintRepository hints;
 public UseWordlyHintUseCase(WordlyHintRepository hints){this.hints=hints;}
 @Transactional
 public WordlyHintResult execute(UUID matchId,UUID wordId,UUID userId,UUID requestId) {
  java.util.Objects.requireNonNull(requestId,"Request ID is required");
  var context=hints.lock(matchId,wordId,userId).orElseThrow(()->new GameMatchNotFoundException(matchId));
  var history=hints.guesses(wordId);
  if(context.requestId()!=null) {
   if(!context.requestId().equals(requestId))throw new IllegalStateException("Hint already used for this word");
   var source=history.stream().filter(g->g.id().equals(context.sourceGuessId())).findFirst().orElseThrow();
   return result(wordId,source,context.column(),context.letter());
  }
  Instant now=Instant.now();
  if(!"IN_PROGRESS".equals(context.matchStatus()) || !"ACTIVE".equals(context.wordStatus()) || !"ACTIVE".equals(context.playerStatus()))
   throw new IllegalStateException("Hint requires an active word and player");
  if(context.deadline()!=null && !now.isBefore(context.deadline()))throw new IllegalStateException("Word deadline has passed");
  if(history.isEmpty())throw new IllegalArgumentException("A valid guess is required before using a hint");
  int column=WordlyHintSelector.firstUnknown(context.answer(),history.stream().map(WordlyHintRepository.Guess::letters).toList());
  String letter=context.answer().substring(column,column+1);
  var source=history.get(history.size()-1);
  hints.save(wordId,requestId,source.id(),column,letter,now);
  return result(wordId,source,column,letter);
 }
 private WordlyHintResult result(UUID id,WordlyHintRepository.Guess source,int column,String letter) {
  return new WordlyHintResult(id,source.guess(),source.letters(),new WordlyHintResult.RevealedCell(column,letter,"CORRECT"),true);
 }
}
