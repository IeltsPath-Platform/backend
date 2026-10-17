package com.ieltspath.game.api.dto.response;
import com.ieltspath.game.application.result.WordlyHintResult;
import java.util.UUID;
import java.util.List;
public record WordlyHintResponse(UUID playerWordId,String sourceGuess,List<String> sourceLetters,Cell revealedCell,boolean hintUsed) {
 public record Cell(int column,String letter,String status){}
 public static WordlyHintResponse from(WordlyHintResult result) {
  return new WordlyHintResponse(result.playerWordId(),result.sourceGuess(),result.sourceLetters(),
   new Cell(result.revealedCell().column(),result.revealedCell().letter(),result.revealedCell().status()),result.hintUsed());
 }
}
