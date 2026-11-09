package com.ieltspath.game.domain.aggregate;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.ieltspath.game.domain.vo.WordlyRoundRules;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class WordlyRoundRulesTest {
 @Test void correctLettersConsumeMultiplicityBeforePresentLetters(){
  assertEquals(List.of("PRESENT","ABSENT","CORRECT","ABSENT","ABSENT"),WordlyRoundRules.evaluate("APPLE","PUPPY").stream().map(s->s).toList());
 }
 @Test void handlesRepeatedLetterWithExactPositionFirst(){
  assertEquals(List.of("CORRECT","PRESENT","ABSENT","PRESENT","ABSENT"),WordlyRoundRules.evaluate("APPLE","ALLEY"));
 }
 @Test void untimedAndThirdStreakGiveExpectedPoints(){
  var now=Instant.now();var r=WordlyRoundRules.reward(true,2,now.minusSeconds(20),null,now);
  assertEquals(100,r.base());assertEquals(50,r.bonus());assertEquals(3,r.streak());assertEquals(20000,r.solveMs());
  assertEquals(160,WordlyRoundRules.reward(true,0,now.minusSeconds(20),now.plusSeconds(160),now).base());
  assertEquals(0,WordlyRoundRules.reward(false,5,now,null,now).streak());
 }

    @Test void aGuessEqualToTheAnswerIsAllCorrect(){
  assertEquals(List.of("CORRECT","CORRECT","CORRECT","CORRECT","CORRECT"),WordlyRoundRules.evaluate("APPLE","APPLE"));
 }
 @Test void aGuessOfAnotherLengthIsRejected(){
  var error=assertThrows(IllegalArgumentException.class,()->WordlyRoundRules.evaluate("APPLE","APPLES"));
  assertEquals("Invalid guess length",error.getMessage());
 }
 @Test void lowerCaseOrMissingWordsAreRejected(){
  for(String[] pair:new String[][]{{"APPLE","apple"},{null,"APPLE"},{"APPLE",null}}){
   var error=assertThrows(IllegalArgumentException.class,()->WordlyRoundRules.evaluate(pair[0],pair[1]));
   assertEquals("Words must contain uppercase English letters",error.getMessage());
  }
 }
 @Test void secondsLeftAreCappedAtOneHundredEighty(){
  var now=Instant.now();
  assertEquals(180,WordlyRoundRules.reward(true,0,now.minusSeconds(20),now.plusSeconds(500),now).base());
 }
 @Test void anAnswerAfterTheDeadlineEarnsNoBasePoints(){
  var now=Instant.now();
  assertEquals(0,WordlyRoundRules.reward(true,0,now.minusSeconds(20),now.minusSeconds(5),now).base());
 }
 @Test void theStreakBonusStartsWithTheThirdWordInARow(){
  var now=Instant.now();
  var second=WordlyRoundRules.reward(true,1,now.minusSeconds(5),null,now);
  assertEquals(2,second.streak());
  assertEquals(0,second.bonus());
  assertEquals(50,WordlyRoundRules.reward(true,2,now.minusSeconds(5),null,now).bonus());
 }
 @Test void thePoolPositionWrapsAround(){
  assertEquals(1,WordlyRoundRules.poolPosition(1,5));
  assertEquals(5,WordlyRoundRules.poolPosition(5,5));
  assertEquals(1,WordlyRoundRules.poolPosition(6,5));
 }
 @Test void aPoolPositionNeedsPositiveNumbers(){
  var zeroSequence=assertThrows(IllegalArgumentException.class,()->WordlyRoundRules.poolPosition(0,5));
  var zeroSize=assertThrows(IllegalArgumentException.class,()->WordlyRoundRules.poolPosition(1,0));
  assertEquals("Sequence and pool size must be positive",zeroSequence.getMessage());
  assertEquals("Sequence and pool size must be positive",zeroSize.getMessage());
 }
}
