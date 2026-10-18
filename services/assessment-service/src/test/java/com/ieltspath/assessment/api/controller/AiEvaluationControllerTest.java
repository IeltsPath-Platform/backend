package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CreateAiEvaluationRequest;
import com.ieltspath.assessment.api.dto.request.CreateAiGoldEssayRequest;
import com.ieltspath.assessment.application.service.AiEvaluationService;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiEvaluationControllerTest {
    private final AiEvaluationService service = mock(AiEvaluationService.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final AiEvaluationController controller = new AiEvaluationController(service, currentUser);
    private final UUID actor = UUID.randomUUID();

    @Test void createsManualGoldEssayForAdmin() {
        UUID id = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(actor);
        when(service.addManual(eq("TASK_2"), eq("prompt"), isNull(), eq("essay"), eq(new BigDecimal("6.5")),
                any(), eq(List.of("GRAMMAR")), eq(actor))).thenReturn(id);
        var response = controller.addGold(new CreateAiGoldEssayRequest("TASK_2", "prompt", null, "essay",
                new BigDecimal("6.5"), List.of(), List.of("GRAMMAR")));
        assertEquals(id, response.get("id"));
    }

    @Test void importsEligibleExaminerComparisons() {
        when(currentUser.requireUserId()).thenReturn(actor);
        when(service.importComparisons(actor)).thenReturn(3);
        assertEquals(3, controller.importComparisons().get("imported"));
    }

    @Test void startsEvaluationAndReadsItsResult() {
        UUID run = UUID.randomUUID();
        when(service.start("TASK_1")).thenReturn(run);
        when(service.getRun(run)).thenReturn(Map.of("id", run, "status", "DONE", "passed", true));
        assertEquals(run, controller.start(new CreateAiEvaluationRequest("TASK_1")).get("id"));
        assertEquals("DONE", controller.get(run).get("status"));
    }

    @Test void activatesPassedRunForCurrentAdmin() {
        UUID run = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(actor);
        controller.activate(new AiEvaluationController.ActivationRequest(run));
        verify(service).activate(run, actor);
    }
}
