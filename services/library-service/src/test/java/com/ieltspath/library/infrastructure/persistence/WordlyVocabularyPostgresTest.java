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
 @Test void filtersByLevelAndTopicBeforeChoosingAMeaning() {
  var ds=new DriverManagerDataSource(db.getJdbcUrl(),db.getUsername(),db.getPassword());
  Flyway.configure().dataSource(ds).load().migrate();var sql=new JdbcTemplate(ds);
  var work=sql.queryForObject("SELECT id FROM vocabulary_topics WHERE slug='work-business'",java.util.UUID.class);
  var life=sql.queryForObject("SELECT id FROM vocabulary_topics WHERE slug='daily-life'",java.util.UUID.class);
  var run=java.util.UUID.randomUUID();
  sql.update("INSERT INTO vocabulary_items(id,lemma,normalized_lemma) VALUES (?,'zzrun','zzrun')",run);
  String sense="INSERT INTO vocabulary_senses(vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence,sort_order,cefr_level,topic_id) VALUES (?,'VERB',?,'x',?,?,?)";
  sql.update(sense,run,"chạy",0,"A1",life);
  sql.update(sense,run,"điều hành",1,"B2",work);
  var adapter=new WordlyVocabularyAdapter(sql);
  java.util.function.BiFunction<String,java.util.UUID,String> meaning=(level,topic)->adapter.page("ZZRU",5,level,topic)
   .stream().filter(w->w.answer().equals("ZZRUN")).map(w->w.meaning()).findFirst().orElse(null);
  assertEquals("chạy",meaning.apply(null,null),"no filter keeps the first meaning");
  assertEquals("điều hành",meaning.apply("B2",null));
  assertEquals("điều hành",meaning.apply(null,work));
  assertEquals("điều hành",meaning.apply("B2",work));
  assertNull(meaning.apply("B2",life),"one meaning has to match both");
  assertEquals("chạy",meaning.apply("A1",life));
  sql.update("DELETE FROM vocabulary_items WHERE id=?",run);
 }
 @Test void filtersInvalidInactiveAndDuplicateWords() {
  var ds=new DriverManagerDataSource(db.getJdbcUrl(),db.getUsername(),db.getPassword());
  Flyway.configure().dataSource(ds).load().migrate();var sql=new JdbcTemplate(ds);
  // The development vocabulary seeded by migration would otherwise be drawn next to the words under test.
  sql.execute("TRUNCATE vocabulary_items CASCADE");
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
