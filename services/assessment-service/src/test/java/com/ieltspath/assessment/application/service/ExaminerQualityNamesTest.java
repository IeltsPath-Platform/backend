package com.ieltspath.assessment.application.service;

import com.ieltspath.assessment.application.port.ExaminerQaRepository;
import com.ieltspath.assessment.application.port.UserDirectory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExaminerQualityNamesTest {
    private final ExaminerQaRepository repository = mock(ExaminerQaRepository.class);
    private final UserDirectory users = mock(UserDirectory.class);
    private final ExaminerQaService service = new ExaminerQaService(repository, users, 0.1);

    private final UUID known = UUID.randomUUID();
    private final UUID unknown = UUID.randomUUID();

    private void table() {
        when(repository.quality()).thenReturn(List.of(
                new ExaminerQaRepository.ExaminerQuality(known, 4, new BigDecimal("0.50"), new BigDecimal("0.2500")),
                new ExaminerQaRepository.ExaminerQuality(unknown, 1, new BigDecimal("1.00"), new BigDecimal("0.0000"))));
    }

    @Test
    void everyRowKeepsItsFiguresAndGainsItsName() {
        table();
        when(users.displayNames(any())).thenReturn(Map.of(known, "Linh Tran"));

        List<ExaminerQaService.NamedExaminerQuality> rows = service.qualityWithNames();

        assertEquals(2, rows.size());
        assertEquals(known, rows.get(0).examinerId());
        assertEquals("Linh Tran", rows.get(0).examinerName());
        assertEquals(4, rows.get(0).reviewedEssays());
        assertEquals(new BigDecimal("0.50"), rows.get(0).averageAbsoluteBandDifference());
        assertEquals(new BigDecimal("0.2500"), rows.get(0).overOneBandRate());
        assertNull(rows.get(1).examinerName());
    }

    @Test
    void theNamesAreLookedUpOnceForTheWholeTable() {
        table();
        when(users.displayNames(any())).thenReturn(Map.of());

        service.qualityWithNames();

        verify(users, times(1)).displayNames(List.of(known, unknown));
    }

    @Test
    void whenNoNamesComeBackTheTableIsStillReturned() {
        table();
        when(users.displayNames(any())).thenReturn(Map.of());

        List<ExaminerQaService.NamedExaminerQuality> rows = service.qualityWithNames();

        assertEquals(2, rows.size());
        assertNull(rows.get(0).examinerName());
    }
}
