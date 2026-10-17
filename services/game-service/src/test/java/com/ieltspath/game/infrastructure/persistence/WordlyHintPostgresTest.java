package com.ieltspath.game.infrastructure.persistence;
import com.ieltspath.game.infrastructure.persistence.adapter.*;
import com.ieltspath.game.application.usecase.UseWordlyHintUseCase;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.flywaydb.core.Flyway;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@Testcontainers(disabledWithoutDocker=true)
class WordlyHintPostgresTest {
 @Container static PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:15-alpine");
 @Test void usesHistoryOnceAndPreservesPlayerCountersAndOriginalGuesses() {
  var ds=new DriverManagerDataSource(db.getJdbcUrl(),db.getUsername(),db.getPassword());
  Flyway.configure().dataSource(ds).load().migrate();var jdbc=new JdbcTemplate(ds);
  var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
  UUID match=UUID.randomUUID(),player=UUID.randomUUID(),user=UUID.randomUUID(),word=UUID.randomUUID(),pw=UUID.randomUUID();
  jdbc.update("INSERT INTO game_matches(id,game_type,learning_domain,config_snapshot,content_snapshot,status,created_at) VALUES (?,'WORDLY','VOCABULARY','{}','{}','IN_PROGRESS',now())",match);
  jdbc.update("INSERT INTO game_match_players(id,match_id,user_id,status,joined_at,score,current_streak,best_streak) VALUES (?,?,?,'ACTIVE',now(),42,2,2)",player,match,user);
  jdbc.update("INSERT INTO game_match_words(id,match_id,sequence_number,source_word_id,answer,meaning) VALUES (?,?,1,gen_random_uuid(),'ACTOR','Actor meaning')",word,match);
  jdbc.update("INSERT INTO game_player_words(id,match_id,match_player_id,match_word_id,started_at,deadline_at) VALUES (?,?,?,?,now(),now()+interval '180 seconds')",pw,match,player,word);
  var use=new UseWordlyHintUseCase(new WordlyHintRepositoryAdapter(jdbc), java.time.Clock.systemUTC());var request=UUID.randomUUID();
  jdbc.update("UPDATE game_player_words SET started_at=now()-interval '181 seconds',deadline_at=now()-interval '1 second' WHERE id=?",pw);
  assertThrows(IllegalStateException.class,()->tx.execute(s->use.execute(match,pw,user,request)));
  jdbc.update("UPDATE game_player_words SET started_at=now(),deadline_at=now()+interval '180 seconds' WHERE id=?",pw);
  jdbc.update("UPDATE game_match_players SET status='FORFEITED' WHERE id=?",player);
  assertThrows(IllegalStateException.class,()->tx.execute(s->use.execute(match,pw,user,request)));
  jdbc.update("UPDATE game_match_players SET status='ACTIVE' WHERE id=?",player);
  assertThrows(IllegalArgumentException.class,()->tx.execute(s->use.execute(match,pw,user,request)));
  assertThrows(GameMatchNotFoundException.class,()->tx.execute(s->use.execute(match,pw,UUID.randomUUID(),request)));
  jdbc.update("INSERT INTO game_guesses(id,player_word_id,request_id,attempt_number,guess,letter_results,is_correct,received_at) VALUES (gen_random_uuid(),?,gen_random_uuid(),1,'ALLOW','[\"CORRECT\",\"ABSENT\",\"ABSENT\",\"CORRECT\",\"ABSENT\"]',false,now())",pw);
  jdbc.update("INSERT INTO game_guesses(id,player_word_id,request_id,attempt_number,guess,letter_results,is_correct,received_at) VALUES (gen_random_uuid(),?,gen_random_uuid(),2,'OTHER','[\"PRESENT\",\"PRESENT\",\"ABSENT\",\"ABSENT\",\"CORRECT\"]',false,now())",pw);
  jdbc.update("UPDATE game_player_words SET attempts_used=2 WHERE id=?",pw);
  var hint=tx.execute(s->use.execute(match,pw,user,request));
  assertEquals(1,hint.revealedCell().column());assertEquals("C",hint.revealedCell().letter());assertEquals("OTHER",hint.sourceGuess());
  assertEquals(hint,tx.execute(s->use.execute(match,pw,user,request)));
  assertThrows(IllegalStateException.class,()->tx.execute(s->use.execute(match,pw,user,UUID.randomUUID())));
  assertEquals(42,jdbc.queryForObject("SELECT score FROM game_match_players WHERE id=?",Integer.class,player));
  assertEquals(2,jdbc.queryForObject("SELECT current_streak FROM game_match_players WHERE id=?",Integer.class,player));
  assertEquals(4,jdbc.queryForObject("SELECT lives_remaining FROM game_match_players WHERE id=?",Integer.class,player));
  assertEquals(2,jdbc.queryForObject("SELECT attempts_used FROM game_player_words WHERE id=?",Integer.class,pw));
  assertEquals("OTHER",jdbc.queryForObject("SELECT guess FROM game_guesses WHERE player_word_id=? AND attempt_number=2",String.class,pw));
  var current=new WordlyMatchStoreAdapter(jdbc).current(match,user).orElseThrow();assertEquals(hint,current.hint());
 }
}
