package com.ieltspath.user.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.user.application.command.UpdateExaminerProfileCommand;
import com.ieltspath.user.application.port.ExaminerDirectoryReader;
import com.ieltspath.user.application.result.ExaminerResult;
import com.ieltspath.user.domain.exception.InvalidDataException;
import com.ieltspath.user.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExaminerProfilesUseCaseTest {
    private final ExaminerDirectoryReader directory = mock(ExaminerDirectoryReader.class);
    private final ExaminerProfilesUseCase useCase = new ExaminerProfilesUseCase(directory, new ObjectMapper());
    private final UUID id = UUID.randomUUID();

    private ExaminerDirectoryReader.Examiner stored(String avatar, boolean accepting) {
        return new ExaminerDirectoryReader.Examiner(id, "Ms Lan", avatar, "Bio", new BigDecimal("8.0"),
                "[{\"name\":\"CELTA\",\"issuer\":\"Cambridge\",\"year\":2018,\"score\":\"A\"}]", accepting, 10);
    }

    @Test
    void anExaminerIsShownWithTheirCertificatesAndOnlyASafePhotoAddress() {
        when(directory.findActive(id)).thenReturn(Optional.of(stored("https://images.test/lan.png", true)));
        var shown = useCase.get(id);
        assertEquals("https://images.test/lan.png", shown.avatarUrl());
        assertEquals(new ExaminerResult.Certificate("CELTA", "Cambridge", 2018, "A"), shown.certificates().getFirst());
        assertEquals(10, shown.dailyLimit());

        for (String unsafe : List.of("javascript:alert(1)", "https://user:secret@images.test/a.png", "not a url", "  ")) {
            when(directory.findActive(id)).thenReturn(Optional.of(stored(unsafe, true)));
            assertNull(useCase.get(id).avatarUrl(), unsafe);
        }
    }

    @Test
    void severalExaminersAreReadInOneCallAndAnIdThatIsNotAnActiveExaminerIsLeftOut() {
        UUID other = UUID.randomUUID();
        when(directory.findActive(List.of(id, other))).thenReturn(List.of(stored(null, true)));

        var shown = useCase.getAll(List.of(id, other));

        assertEquals(1, shown.size());
        assertEquals(id, shown.getFirst().id());
        assertEquals(10, shown.getFirst().dailyLimit());
        verify(directory).findActive(List.of(id, other));
        verify(directory, never()).findActive(any(UUID.class));
    }

    @Test
    void anUnknownOrInactiveExaminerIsNotFound() {
        when(directory.findActive(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.get(id));
        assertThrows(ResourceNotFoundException.class, () -> useCase.setCapacity(id, 5));
        assertThrows(ResourceNotFoundException.class, () -> useCase.updateOwn(id, command(null, List.of())));
        verify(directory, never()).saveProfile(any(), any(), anyString(), anyBoolean());
    }

    @Test
    void anExaminerKeepsTheirBandCertificatesAndAvailability() {
        when(directory.findActive(id)).thenReturn(Optional.of(stored(null, true)));
        var certificate = new ExaminerResult.Certificate("IELTS Examiner", "British Council", 2020, null);

        useCase.updateOwn(id, command(new BigDecimal("8.5"), List.of(certificate)));

        verify(directory).saveProfile(eq(id), eq(new BigDecimal("8.5")),
                eq("[{\"name\":\"IELTS Examiner\",\"issuer\":\"British Council\",\"year\":2020,\"score\":null}]"), eq(true));
    }

    @Test
    void aBandThatIsNotAHalfBandOrACertificateThatIsMalformedIsRefused() {
        when(directory.findActive(id)).thenReturn(Optional.of(stored(null, true)));
        for (BigDecimal band : List.of(new BigDecimal("7.3"), new BigDecimal("9.5"), new BigDecimal("-0.5"))) {
            assertThrows(InvalidDataException.class, () -> useCase.updateOwn(id, command(band, List.of())));
        }
        var tooMany = IntStream.range(0, 11).mapToObj(i -> new ExaminerResult.Certificate("C" + i, null, null, null)).toList();
        assertThrows(InvalidDataException.class, () -> useCase.updateOwn(id, command(null, tooMany)));
        for (var bad : List.of(new ExaminerResult.Certificate(" ", null, null, null),
                new ExaminerResult.Certificate("x".repeat(151), null, null, null),
                new ExaminerResult.Certificate("A", null, 1900, null),
                new ExaminerResult.Certificate("A", null, 2101, null),
                new ExaminerResult.Certificate("A", null, null, "x".repeat(21)))) {
            assertThrows(InvalidDataException.class, () -> useCase.updateOwn(id, command(null, List.of(bad))));
        }
        assertThrows(InvalidDataException.class,
                () -> useCase.updateOwn(id, command(null, Collections.singletonList(null))));
        verify(directory, never()).saveProfile(any(), any(), anyString(), anyBoolean());
    }

    @Test
    void theCapacityMustBeBetweenOneAndFifty() {
        when(directory.saveCapacity(id, 5)).thenReturn(true);
        when(directory.findActive(id)).thenReturn(Optional.of(stored(null, true)));
        useCase.setCapacity(id, 5);
        verify(directory).saveCapacity(id, 5);
        assertThrows(InvalidDataException.class, () -> useCase.setCapacity(id, 0));
        assertThrows(InvalidDataException.class, () -> useCase.setCapacity(id, 51));
    }

    private static UpdateExaminerProfileCommand command(BigDecimal band, List<ExaminerResult.Certificate> certificates) {
        return new UpdateExaminerProfileCommand(band, certificates, true);
    }
}
