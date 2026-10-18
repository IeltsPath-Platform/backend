package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.ReviewReevaluation;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false", "management.health.rabbit.enabled=false",
        "learning.messaging.consumer-enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Testcontainers
class PracticeTestAssignmentIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    static final byte[] KEY=new byte[32];
    static { new SecureRandom().nextBytes(KEY); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl);
        r.add("spring.datasource.username",postgres::getUsername);
        r.add("spring.datasource.password",postgres::getPassword);
        r.add("app.auth.internal-jwt-secret",()->Base64.getEncoder().encodeToString(KEY));
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ListPracticeTestsUseCase list;
    @Autowired AssignPracticeTestUseCase assign;
    @Autowired ApplyAssessmentResultUseCase apply;
    @MockitoBean LearningContentClient content;
    @MockitoSpyBean ReviewReevaluation reviews;
    final UUID user=UUID.randomUUID(),lower=UUID.randomUUID(),course=UUID.randomUUID(),first=UUID.randomUUID(),
            second=UUID.randomUUID(),otherTopic=UUID.randomUUID(),pkg=UUID.randomUUID(),version=UUID.randomUUID(),kp=UUID.randomUUID();

    @BeforeEach void setup() {
        jdbc.execute("TRUNCATE practice_test_assignments, topic_progress, knowledge_point_catalog, lesson_progress, "
                + "assessment_result_versions, kp_evidence, review_items CASCADE");
        var c1=new LearningContentClient.Course(lower,"LOWER","Lower",new BigDecimal("5.5"),true);
        var c2=new LearningContentClient.Course(course,"UPPER","Upper",new BigDecimal("6.5"),true);
        when(content.getTopicSequence()).thenReturn(List.of(topic(otherTopic,1,c1),topic(first,2,c2),topic(second,3,c2)));
        when(content.getPracticeTestMilestones(course)).thenReturn(List.of(
                new LearningContentClient.PracticeTestMilestone(pkg,version,"PT","Practice test",1)));
    }

    @Test void gateUsesPositionInsideCourseAndAssignmentIsIdempotent() {
        assertThat(list.execute(user,course).getFirst().status()).isEqualTo("LOCKED");
        pass(otherTopic);
        assertThat(list.execute(user,course).getFirst().status()).isEqualTo("LOCKED");
        assertThatThrownBy(()->assign.execute(user,course,pkg)).isInstanceOfSatisfying(LearningRequestException.class,
                e->assertThat(e.getCode()).isEqualTo("PRACTICE_TEST_LOCKED"));
        pass(first);
        assertThat(list.execute(user,course).getFirst().status()).isEqualTo("AVAILABLE");
        var a=assign.execute(user,course,pkg);
        assertThat(assign.execute(user,course,pkg)).isEqualTo(a);
        assertThat(a.packageVersionId()).isEqualTo(version);
        assertThat(countAssignments()).isEqualTo(1);
    }

    @Test void firstScoreSurvivesRetakeRegradeAndReplay() {
        list.execute(user,course); pass(first); assign.execute(user,course,pkg);
        Instant completed=Instant.now().plusSeconds(10);
        var initial=result(user,version,UUID.randomUUID(),1,completed,3);
        apply.execute(initial); apply.execute(initial);
        assertThat(list.execute(user,course).getFirst().status()).isEqualTo("DONE");
        assertThat(list.execute(user,course).getFirst().percent()).isEqualByComparingTo("60");
        assign.execute(user,course,pkg);
        apply.execute(result(user,version,UUID.randomUUID(),1,completed.plusSeconds(5),5));
        apply.execute(result(user,version,initial.attemptId(),2,completed.plusSeconds(10),5));
        assertThat(list.execute(user,course).getFirst().percent()).isEqualByComparingTo("60");
        assertThat(countAssignments()).isEqualTo(1);
    }

    @Test void freePracticeWritesEvidenceWithoutScoringOrBlockingTopics() {
        list.execute(user,course);
        apply.execute(result(user,version,UUID.randomUUID(),1,Instant.now(),1));
        assertThat(countAssignments()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id=?",Integer.class,user)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM topic_progress WHERE user_id=? AND passed_at IS NOT NULL",Integer.class,user)).isZero();
        verify(reviews,never()).execute(any(),any(),any());
    }

    @Test void regradingEarlierFreeAttemptCannotConsumeLaterAssignment() {
        list.execute(user,course); pass(first);
        var free=result(user,version,UUID.randomUUID(),1,Instant.now().minusSeconds(30),1);
        apply.execute(free); assign.execute(user,course,pkg);
        apply.execute(result(user,version,free.attemptId(),2,Instant.now().plusSeconds(30),5));
        assertThat(list.execute(user,course).getFirst().percent()).isNull();
    }

    @Test void resultBeforeAssignmentWrongVersionAndOtherUserCannotConsumeAssignment() {
        list.execute(user,course); pass(first); assign.execute(user,course,pkg);
        apply.execute(result(user,version,UUID.randomUUID(),1,Instant.now().minusSeconds(60),5));
        apply.execute(result(user,UUID.randomUUID(),UUID.randomUUID(),1,Instant.now().plusSeconds(30),5));
        apply.execute(result(UUID.randomUUID(),version,UUID.randomUUID(),1,Instant.now().plusSeconds(30),5));
        assertThat(list.execute(user,course).getFirst().percent()).isNull();
    }

    @Test void unknownMilestoneOrCourseDoesNotCreateAssignment() {
        list.execute(user,course); pass(first);
        assertThatThrownBy(()->assign.execute(user,course,UUID.randomUUID())).isInstanceOfSatisfying(LearningRequestException.class,
                e->assertThat(e.getCode()).isEqualTo("PRACTICE_TEST_NOT_FOUND"));
        assertThatThrownBy(()->list.execute(user,UUID.randomUUID())).isInstanceOfSatisfying(LearningRequestException.class,
                e->assertThat(e.getCode()).isEqualTo("COURSE_NOT_FOUND"));
        assertThat(countAssignments()).isZero();
    }

    @Test void concurrentAssignmentRequestsShareOneStoredAssignment() throws Exception {
        list.execute(user,course); pass(first);
        try (var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->assign.execute(user,course,pkg));
            var b=pool.submit(()->assign.execute(user,course,pkg));
            assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
        }
        assertThat(countAssignments()).isEqualTo(1);
    }

    private void pass(UUID topic) {
        jdbc.update("UPDATE topic_progress SET passed_at=? WHERE user_id=? AND topic_id=?",Timestamp.from(Instant.now()),user,topic);
    }
    private int countAssignments() { return jdbc.queryForObject("SELECT count(*) FROM practice_test_assignments",Integer.class); }
    private LearningContentClient.Topic topic(UUID id,int order,LearningContentClient.Course c) {
        return new LearningContentClient.Topic(id,"T"+order,"Topic",order,null,List.of(),LearningSkill.READING,true,c);
    }
    private AssessmentResult result(UUID learner,UUID v,UUID attempt,int revision,Instant at,int correct) {
        var items=java.util.stream.IntStream.range(0,5).mapToObj(i->new AssessmentResult.ItemResult(
                UUID.randomUUID(),UUID.randomUUID(),i<correct,i<correct?BigDecimal.ONE:BigDecimal.ZERO,BigDecimal.ONE,
                List.of(new AssessmentResult.KnowledgePointJudgment(kp,null)))).toList();
        return new AssessmentResult(UUID.randomUUID(),learner,v,attempt,UUID.randomUUID(),revision,"OFFICIAL_PRACTICE",at,items);
    }
}
