package com.ieltspath.game.application.result;
import java.util.UUID;
import java.util.List;
public record WordlyHintResult(UUID playerWordId,String sourceGuess,List<String> sourceLetters,RevealedCell revealedCell,boolean hintUsed) {
 public record RevealedCell(int column,String letter,String status){}
}
