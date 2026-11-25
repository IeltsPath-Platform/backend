package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.CourseImportDraftStore;
import com.ieltspath.content.application.port.CourseImportDraftStore.Draft;
import com.ieltspath.content.application.port.CourseWorkbookReader.ReadResult;
import com.ieltspath.content.domain.exception.CourseImportDraftNotFoundException;
import com.ieltspath.content.domain.exception.CourseImportInvalidException;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import com.ieltspath.content.domain.vo.CourseImportModel.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseImportDraftUseCasesTest {

    private final CourseImportUseCases imports = mock(CourseImportUseCases.class);
    private final CourseImportDraftStore store = mock(CourseImportDraftStore.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC);
    private final CourseImportDraftUseCases drafts = new CourseImportDraftUseCases(imports, store, clock);
    private final UUID author = UUID.randomUUID();
    private final byte[] file = {'P', 'K', 3, 4};
    private CourseImportModel model;
    private CourseImportUseCases.Report report;

    @BeforeEach
    void setUp() {
        model = new CourseImportModel("1", Map.of("Course", List.of(new Row(3, Map.of("code", "C1", "name", "N", "bandLevel", "4.5")))));
        report = new CourseImportUseCases.Report(List.of(), List.of(), null, null);
        when(imports.read(any(), any())).thenReturn(new ReadResult(model, List.of()));
        when(imports.validateModel(any())).thenReturn(report);
    }

    private Draft draft(String status) {
        return new Draft(UUID.randomUUID(), author, "course.xlsx", status, null, model, clock.instant(), clock.instant());
    }

    @Test
    void aReadableWorkbookIsKeptEvenWhenItsRowsHaveErrors() {
        var withErrors = new CourseImportUseCases.Report(List.of(new Issue("Course", 3, "code", "CODE_CONFLICT", "x")), List.of(), null, null);
        when(imports.validateModel(any())).thenReturn(withErrors);

        var checked = drafts.create(author, "course.xlsx", file);

        verify(store).insert(any(Draft.class));
        assertFalse(checked.report().valid());
        assertEquals("course.xlsx", checked.draft().filename());
    }

    @Test
    void aWorkbookThatCannotBeReadIsRefusedAndKeptNowhere() {
        when(imports.read(any(), any())).thenReturn(new ReadResult(null, List.of(new Issue("", 0, "", "UNREADABLE_FILE", "x"))));

        assertThrows(CourseImportInvalidException.class, () -> drafts.create(author, "course.xlsx", file));
        verify(store, never()).insert(any());
    }

    @Test
    void anAuthorAtTheLimitCannotKeepAnotherDraft() {
        when(store.countDrafts(author)).thenReturn(CourseImportDraftUseCases.MAX_DRAFTS_PER_AUTHOR);

        assertThrows(IllegalArgumentException.class, () -> drafts.create(author, "course.xlsx", file));
        verify(store, never()).insert(any());
    }

    @Test
    void savingKeepsOnlyTheTemplateSheetsAndColumnsAndChecksTheEditedRows() {
        Draft kept = draft("DRAFT");
        when(store.find(author, kept.id())).thenReturn(Optional.of(kept));
        Map<String, String> cells = new HashMap<>(Map.of("code", " C2 ", "name", "N", "bandLevel", "5", "evil", "x"));

        var checked = drafts.update(author, kept.id(), Map.of("Course", List.of(new Row(3, cells)), "Secret", List.of(new Row(3, Map.of("a", "b")))));

        ArgumentCaptor<CourseImportModel> saved = ArgumentCaptor.forClass(CourseImportModel.class);
        verify(store).updateContent(eq(author), eq(kept.id()), saved.capture(), any());
        Row row = saved.getValue().rows("Course").get(0);
        assertEquals("C2", row.get("code"));
        assertFalse(row.cells().containsKey("evil"));
        assertTrue(saved.getValue().rows("Secret").isEmpty());
        assertEquals("", saved.getValue().rows("Topics").isEmpty() ? "" : "has rows");
        assertTrue(checked.report().valid());
    }

    @Test
    void importingMarksTheDraftAsImported() {
        Draft kept = draft("DRAFT");
        UUID course = UUID.randomUUID();
        when(store.find(author, kept.id())).thenReturn(Optional.of(kept));
        when(imports.executeModel(author, model)).thenReturn(new CourseImportUseCases.Imported(course, List.of(), List.of(),
                List.of(), List.of(), Map.of(), List.of()));

        drafts.importDraft(author, kept.id());

        verify(store).markImported(eq(author), eq(kept.id()), eq(course), any());
    }

    @Test
    void anImportedDraftCannotBeChangedOrImportedAgain() {
        Draft done = draft("IMPORTED");
        when(store.find(author, done.id())).thenReturn(Optional.of(done));

        assertThrows(IllegalStateException.class, () -> drafts.update(author, done.id(), Map.of()));
        assertThrows(IllegalStateException.class, () -> drafts.importDraft(author, done.id()));
        verify(imports, never()).executeModel(any(), any());
    }

    @Test
    void anotherAuthorsDraftIsNotFound() {
        UUID id = UUID.randomUUID();
        when(store.find(author, id)).thenReturn(Optional.empty());

        assertThrows(CourseImportDraftNotFoundException.class, () -> drafts.get(author, id));
        assertThrows(CourseImportDraftNotFoundException.class, () -> drafts.delete(author, id));
    }
}
