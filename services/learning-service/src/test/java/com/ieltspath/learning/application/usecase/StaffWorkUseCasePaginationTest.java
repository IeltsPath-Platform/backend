package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.service.WorkItemSubjectResolver;
import com.ieltspath.learning.application.service.WorkItemSync;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffWorkUseCasePaginationTest {
    private final StaffInsightRepository repository = mock(StaffInsightRepository.class);
    private final WorkItemSync sync = mock(WorkItemSync.class);
    private final WorkItemSubjectResolver subjects = mock(WorkItemSubjectResolver.class);
    private final StaffWorkUseCase useCase = new StaffWorkUseCase(repository, sync, subjects);

    @Test
    void appliesActiveFilterAndReturnsPageTotals() {
        when(repository.countWorkItems(EnumSet.of(Queue.CONTENT_AUTHOR), null, WorkKind.CONTENT_GAP, true))
                .thenReturn(51L);
        when(repository.workItems(EnumSet.of(Queue.CONTENT_AUTHOR), null, WorkKind.CONTENT_GAP, true, 25, 25))
                .thenReturn(List.of());
        when(subjects.resolve(List.of())).thenReturn(Map.of());

        var result = useCase.listPageWithSubjects(EnumSet.of(Queue.CONTENT_AUTHOR), null, null,
                WorkKind.CONTENT_GAP, true, 1, 25);

        assertEquals(1, result.page());
        assertEquals(25, result.size());
        assertEquals(51, result.totalElements());
        assertEquals(3, result.totalPages());
        verify(sync).sync();
    }

    @Test
    void rejectsOutOfRangePageParametersWithVietnameseMessage() {
        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> useCase.listPageWithSubjects(EnumSet.of(Queue.CONTENT_AUTHOR), null, null, null,
                        false, -1, 25));
        assertEquals(400, error.getStatus());
        assertEquals("Thông số phân trang không hợp lệ.", error.getMessage());
    }
}
