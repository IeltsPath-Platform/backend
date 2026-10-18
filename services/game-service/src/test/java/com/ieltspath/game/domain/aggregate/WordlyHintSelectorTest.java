package com.ieltspath.game.domain.aggregate;
import com.ieltspath.game.domain.vo.WordlyHintSelector;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class WordlyHintSelectorTest {
 @Test void skipsCorrectPositionsFromAllRowsLeftToRight() {
  assertEquals(1,WordlyHintSelector.firstUnknown("ACTOR",List.of(
    List.of("CORRECT","ABSENT","ABSENT","CORRECT","ABSENT"),
    List.of("ABSENT","PRESENT","ABSENT","ABSENT","CORRECT"))));
 }
 @Test void noUnknownPositionDoesNotRevealAnotherLetter() {
  assertThrows(IllegalArgumentException.class,()->WordlyHintSelector.firstUnknown("EAR",List.of(List.of("CORRECT","CORRECT","CORRECT"))));
 }
}
