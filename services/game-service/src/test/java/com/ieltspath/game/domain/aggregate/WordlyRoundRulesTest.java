package com.ieltspath.game.domain.aggregate;
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
}
