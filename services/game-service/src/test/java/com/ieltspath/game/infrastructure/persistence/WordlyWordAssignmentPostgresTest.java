package com.ieltspath.game.infrastructure.persistence;
import com.ieltspath.game.infrastructure.persistence.adapter.WordlyMatchStoreAdapter;
import com.ieltspath.game.application.port.WordlyWordProvider;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import java.util.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
@Testcontainers(disabledWithoutDocker=true)
class WordlyWordAssignmentPostgresTest {
 @Container static PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:15-alpine");
 @Test void assignsSameFirstWordAndKeepsAnswersOutOfCurrentState() throws Exception {
  var ds=new DriverManagerDataSource(db.getJdbcUrl(),db.getUsername(),db.getPassword());
  Flyway.configure().dataSource(ds).load().migrate();var sql=new JdbcTemplate(ds);
  var match=UUID.randomUUID();var first=UUID.randomUUID();var second=UUID.randomUUID();
  var user1=UUID.randomUUID();var user2=UUID.randomUUID();
  sql.update("INSERT INTO game_matches(id,game_type,learning_domain,config_snapshot,content_snapshot,status,created_at) VALUES (?,'WORDLY','VOCABULARY','{}','{}','IN_PROGRESS',now())",match);
  for(var pair:List.of(new UUID[]{first,user1},new UUID[]{second,user2}))
   sql.update("INSERT INTO game_match_players(id,match_id,user_id,status,joined_at) VALUES (?,?,?,'ACTIVE',now())",pair[0],match,pair[1]);
  var store=new WordlyMatchStoreAdapter(sql);var now=Instant.now();
  store.initialize(match,List.of(new WordlyWordProvider.Word(UUID.randomUUID(),"AIR","Test meaning"),new WordlyWordProvider.Word(UUID.randomUUID(),"BOOK","Other meaning")),List.of(first,second),now,true);
  var a=store.current(match,user1).orElseThrow();var b=store.current(match,user2).orElseThrow();
  assertEquals(1,a.wordNumber());assertEquals(a.meaning(),b.meaning());
  assertNotEquals(a.playerWordId(),b.playerWordId());assertEquals(3,a.columns());assertEquals(4,a.maxAttempts());
  assertTrue(store.current(match,UUID.randomUUID()).isEmpty());
  String json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(a);
  assertFalse(json.contains("AIR"));assertFalse(json.contains("BOOK"));assertFalse(json.contains("answer"));
 }
}
