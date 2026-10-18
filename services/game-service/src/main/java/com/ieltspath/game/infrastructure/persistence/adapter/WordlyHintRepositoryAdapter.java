package com.ieltspath.game.infrastructure.persistence.adapter;
import com.ieltspath.game.application.port.WordlyHintRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.time.Instant;
@Repository
public class WordlyHintRepositoryAdapter implements WordlyHintRepository {
 private final JdbcTemplate jdbc;
 public WordlyHintRepositoryAdapter(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Optional<Context> lock(UUID matchId,UUID wordId,UUID userId) {
  return jdbc.query("""
   SELECT pw.id,w.answer,pw.status AS word_status,p.status AS player_status,m.status AS match_status,
    pw.deadline_at,pw.hint_request_id,pw.hint_source_guess_id,pw.hint_column,pw.hint_letter
   FROM game_player_words pw JOIN game_match_players p ON p.id=pw.match_player_id
    JOIN game_matches m ON m.id=pw.match_id JOIN game_match_words w ON w.id=pw.match_word_id
   WHERE pw.id=? AND m.id=? AND p.user_id=? AND m.game_type='WORDLY'
    AND NOT pw.is_delete AND NOT p.is_delete AND NOT m.is_delete AND NOT w.is_delete
   FOR UPDATE OF m,p,pw
   """,(r,n)->new Context(r.getObject("id",UUID.class),r.getString("answer"),r.getString("word_status"),r.getString("player_status"),r.getString("match_status"),
     r.getTimestamp("deadline_at")==null?null:r.getTimestamp("deadline_at").toInstant(),r.getObject("hint_request_id",UUID.class),
     r.getObject("hint_source_guess_id",UUID.class),(Integer)r.getObject("hint_column"),r.getString("hint_letter")),wordId,matchId,userId).stream().findFirst();
 }
 public List<Guess> guesses(UUID wordId) {
  return jdbc.query("SELECT id,guess,letter_results::text FROM game_guesses WHERE player_word_id=? AND NOT is_delete ORDER BY attempt_number",(r,n)->{
   try{return new Guess(r.getObject(1,UUID.class),r.getString(2),List.of(new com.fasterxml.jackson.databind.ObjectMapper().readValue(r.getString(3),String[].class)));}
   catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Invalid saved letter results",e);}
  },wordId);
 }
 public void save(UUID wordId,UUID requestId,UUID sourceId,int column,String letter,Instant now) {
  jdbc.update("UPDATE game_player_words SET hint_request_id=?,hint_source_guess_id=?,hint_column=?,hint_letter=?,hint_used_at=? WHERE id=?",requestId,sourceId,column,letter,java.sql.Timestamp.from(now),wordId);
 }
}
