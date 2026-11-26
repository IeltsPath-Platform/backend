package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ieltspath.events.user.UserDeletedV1;
import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.port.AssessmentResultLog;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.CourseOutcomeService;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.application.service.PlacementAssignmentCompleter;
import com.ieltspath.learning.application.service.ReviewReevaluation;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import com.ieltspath.learning.domain.repository.CourseTestAssignmentRepository;
import com.ieltspath.learning.domain.repository.DeletedUserRepository;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.PracticeTestAssignmentRepository;
import com.ieltspath.learning.domain.repository.TopicTestAssignmentRepository;
import com.ieltspath.learning.infrastructure.messaging.UserDeletedListener;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserDeletionLearningTest {

    @Test
    void listenerRecordsDeletionAndAcks() throws Exception {
        RecordUserDeletionUseCase recordDeletion = mock(RecordUserDeletionUseCase.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(
                new SimpleModule().addSerializer(Instant.class, ToStringSerializer.instance))
                .findAndRegisterModules();
        UserDeletedListener listener = new UserDeletedListener(mapper, recordDeletion, 5);

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-11-26T05:00:00Z");
        byte[] body = mapper.writeValueAsBytes(new UserDeletedV1(eventId, UserDeletedV1.EVENT_TYPE, occurredAt, userId));
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        Message message = new Message(body, props);
        Channel channel = mock(Channel.class);

        listener.onMessage(message, channel);

        verify(recordDeletion).execute(eventId, userId, occurredAt);
        verify(channel).basicAck(7L, false);
    }

    @Test
    void applyAssessmentResultSkipsDeletedUserByTombstone() {
        LearnerLock lock = mock(LearnerLock.class);
        DeletedUserRepository deletedUsers = mock(DeletedUserRepository.class);
        UUID userId = UUID.randomUUID();
        when(deletedUsers.isDeleted(userId)).thenReturn(true);

        ApplyAssessmentResultUseCase useCase = new ApplyAssessmentResultUseCase(
                lock,
                mock(KnowledgeEvidenceRepository.class),
                mock(LearnerCurriculumRepository.class),
                mock(AssessmentResultLog.class),
                mock(TopicTestAssignmentRepository.class),
                mock(ReviewReevaluation.class),
                mock(LearnerPlacementRepository.class),
                mock(CourseProgressRepository.class),
                mock(CourseTestAssignmentRepository.class),
                mock(PracticeTestAssignmentRepository.class),
                mock(PlacementAssignmentCompleter.class),
                mock(QuestionEventLog.class),
                mock(MasteryProfileUpdater.class),
                mock(CourseOutcomeService.class));
        useCase.setDeletedUsers(deletedUsers);

        AssessmentResult result = new AssessmentResult(
                UUID.randomUUID(), userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, "TOPIC_GATE", Instant.now(), List.of());

        useCase.execute(result);

        verify(deletedUsers).isDeleted(userId);
        verifyNoInteractions(lock);
    }
}
