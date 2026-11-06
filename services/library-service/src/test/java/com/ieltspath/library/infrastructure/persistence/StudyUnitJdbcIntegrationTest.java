package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.port.StudyUnitRepository.Draft;
import com.ieltspath.library.application.service.StudyUnitAuthoringService;
import com.ieltspath.library.application.service.StudyUnitContentService;
import com.ieltspath.library.application.service.StudyExercisePreview;
import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.infrastructure.adapter.StudyUnitJdbcAdapter;
import com.ieltspath.library.infrastructure.adapter.VocabularyAudioUrlResolver;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({StudyUnitJdbcAdapter.class, VocabularyAudioUrlResolver.class, StudyUnitAuthoringService.class,
        StudyUnitContentService.class, StudyExercisePreview.class, StudyUnitJdbcIntegrationTest.TimeConfig.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties={"spring.flyway.enabled=true","spring.cloud.config.enabled=false","eureka.client.enabled=false"})
class StudyUnitJdbcIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl); r.add("spring.datasource.username",POSTGRES::getUsername); r.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @TestConfiguration static class TimeConfig { @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"),ZoneOffset.UTC); } }
    @Autowired JdbcTemplate jdbc;
    @Autowired StudyUnitAuthoringService author;
    @Autowired StudyUnitContentService content;
    @SpyBean StudyUnitJdbcAdapter repository;
    UUID topic; List<UUID> senses; UUID user=UUID.randomUUID();
    @BeforeEach void fixture() {
        topic=UUID.randomUUID(); senses=new ArrayList<>();
        jdbc.update("insert into vocabulary_topics(id,slug,name,name_vi,status) values(?,?,?,'Học từ','ACTIVE')",topic,"study-"+topic,"Study");
        for(int i=0;i<4;i++) {
            UUID item=UUID.randomUUID(), sense=UUID.randomUUID(); senses.add(sense);
            jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status) values(?,?,?,'ACTIVE')",item,"study"+item,"study"+item);
            jdbc.update("insert into vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence,topic_id,status) values(?,?,'NOUN',?, ?,?,'ACTIVE')",sense,item,"nghĩa "+i,"Example.",topic);
        }
    }
    Draft draft(String title) { return new Draft(title,null,"LEARN","Giới thiệu",senses); }
    @Test void revisionsPublishArchiveOrderAndCounts() {
        var one=author.create(topic,user,draft("First")); var two=author.create(topic,user,draft("Second"));
        assertEquals(1,one.sortOrder()); assertEquals(2,two.sortOrder());
        assertEquals(0,content.list(topic).totalUnits());
        author.publish(one.unitId()); author.publish(two.unitId());
        assertEquals(2,content.list(topic).curriculumVersion()); assertEquals(4,content.list(topic).uniqueWordCount());
        author.update(one.unitId(),user,draft("Changed"));
        assertEquals("First",content.get(one.unitId()).unit().title());
        assertEquals(2,author.list(topic).getFirst().draftRevision());
        author.publish(one.unitId()); assertEquals("Changed",content.get(one.unitId()).unit().title());
        author.reorder(topic,List.of(two.unitId(),one.unitId()));
        assertEquals(two.unitId(),content.list(topic).items().getFirst().unitId());
        assertEquals("VOCAB_UNIT_INVALID",assertThrows(StudyRequestException.class,()->author.reorder(topic,List.of(one.unitId()))).code());
        jdbc.update("update vocabulary_senses set status='INACTIVE' where id=?",senses.getFirst());
        assertEquals(3,content.get(one.unitId()).words().size()); assertEquals(3,content.list(topic).items().getFirst().wordCount());
        author.archive(one.unitId()); assertEquals(1,content.list(topic).totalUnits());
        assertEquals("VOCAB_UNIT_NOT_FOUND",assertThrows(StudyRequestException.class,()->content.get(one.unitId())).code());
    }
    @Test void draftHiddenDuplicateSensesRejectedAndConstraintsPresent() {
        var unit=author.create(topic,user,draft("Draft"));
        assertThrows(StudyRequestException.class,()->content.get(unit.unitId()));
        assertEquals("VOCAB_UNIT_INVALID",assertThrows(StudyRequestException.class,()->author.create(topic,user,new Draft("X",null,"LEARN","Y",List.of(senses.getFirst(),senses.getFirst())))).code());
        assertEquals(4,jdbc.queryForObject("select count(*) from information_schema.tables where table_name in ('vocabulary_study_units','vocabulary_study_unit_revisions','vocabulary_study_unit_words','vocabulary_study_topic_versions')",Integer.class));
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void concurrentPublishOnlyCommitsOnce() throws Exception {
        var unit=author.create(topic,user,draft("Concurrent"));
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            var jobs=List.<Callable<String>>of(()->publish(unit.unitId()),()->publish(unit.unitId()));
            var results=pool.invokeAll(jobs).stream().map(f->{try{return f.get();}catch(Exception e){throw new RuntimeException(e);}}).toList();
            assertEquals(1,results.stream().filter("OK"::equals).count());
            assertEquals(1,results.stream().filter("VOCAB_UNIT_NO_DRAFT"::equals).count());
        } finally { pool.shutdownNow(); }
    }
    String publish(UUID id) { try {author.publish(id);return "OK";}catch(StudyRequestException e){return e.code();} }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void migrationRestrictsReferencedSensesAndCascadesUnitChildren() {
        var unit=author.create(topic,user,draft("Constraints"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("delete from vocabulary_senses where id=?",senses.getFirst()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into vocabulary_study_units(topic_id,sort_order,kind) values(?,1,'LEARN')",topic));
        jdbc.update("delete from vocabulary_study_units where id=?",unit.unitId());
        assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_unit_revisions where unit_id=?",Integer.class,unit.unitId()));
        assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_unit_words where unit_id=?",Integer.class,unit.unitId()));
    }
    @Test void changingKindRemainsDraftUntilPublication() {
        var unit=author.create(topic,user,draft("Learn"));author.publish(unit.unitId());
        var updated=author.update(unit.unitId(),user,new Draft("Review",null,"REVIEW","Giới thiệu",senses));
        assertEquals("REVIEW",updated.kind());
        assertEquals("LEARN",content.get(unit.unitId()).unit().kind());
        author.publish(unit.unitId());assertEquals("REVIEW",content.get(unit.unitId()).unit().kind());
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void listHasCoherentRevisionAndCurriculumDuringPublish() throws Exception {
        var unit=author.create(topic,user,draft("Original"));author.publish(unit.unitId());
        author.update(unit.unitId(),user,draft("New"));
        var readUnits=new CountDownLatch(1);var published=new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(invocation->{
            var result=invocation.callRealMethod();readUnits.countDown();
            if(!published.await(10,TimeUnit.SECONDS))throw new AssertionError("publisher did not complete");
            return result;
        }).when(repository).list(topic,false);
        ExecutorService executor=Executors.newSingleThreadExecutor();
        try {
            var read=executor.submit(()->content.list(topic));assertTrue(readUnits.await(10,TimeUnit.SECONDS));
            author.publish(unit.unitId());published.countDown();
            var result=read.get(10,TimeUnit.SECONDS);
            assertEquals("Original",result.items().getFirst().title());assertEquals(1,result.curriculumVersion());
        } finally {published.countDown();executor.shutdownNow();org.mockito.Mockito.reset(repository);}
    }
}
