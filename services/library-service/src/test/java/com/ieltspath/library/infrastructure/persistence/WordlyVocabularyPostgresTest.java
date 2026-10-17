package com.ieltspath.library.infrastructure.persistence;
import com.ieltspath.library.infrastructure.persistence.adapter.WordlyVocabularyAdapter;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.*;
@Testcontainers(disabledWithoutDocker=true)
class WordlyVocabularyPostgresTest {
 @Container static PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:15-alpine");
 @Test void filtersInvalidInactiveAndDuplicateWords() {
  var ds=new DriverManagerDataSource(db.getJdbcUrl(),db.getUsername(),db.getPassword());
  Flyway.configure().dataSource(ds).load().migrate();var sql=new JdbcTemplate(ds);
  for(String word:java.util.List.of("air","AIR","book","two words","hello-world")) {
   var id=java.util.UUID.randomUUID();
   sql.update("INSERT INTO vocabulary_items(id,lemma,normalized_lemma) VALUES (?,?,?)",id,word,word);
   sql.update("INSERT INTO vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence) VALUES (gen_random_uuid(),?,'NOUN','Test meaning','Example')",id);
  }
  var words=new WordlyVocabularyAdapter(sql).random(10);
  assertEquals(2,words.size());assertEquals(2,words.stream().map(w->w.answer()).distinct().count());
  assertTrue(words.stream().allMatch(w->w.answer().matches("[A-Z]+")));
  var catalog=new WordlyVocabularyAdapter(sql);
  assertEquals("AIR",catalog.page("",1).get(0).answer());
  assertEquals("BOOK",catalog.page("AIR",1).get(0).answer());
  assertTrue(catalog.page("BOOK",1).isEmpty());
  sql.update("UPDATE vocabulary_items SET status='INACTIVE'");
  assertTrue(catalog.page("",500).isEmpty());
  assertTrue(new WordlyVocabularyAdapter(sql).random(10).isEmpty());
 }
}
