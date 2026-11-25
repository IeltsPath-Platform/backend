package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.CourseImportDraftStore;
import com.ieltspath.content.application.port.CourseImportDraftStore.Draft;
import com.ieltspath.content.application.port.CourseWorkbookReader;
import com.ieltspath.content.domain.exception.CourseImportDraftNotFoundException;
import com.ieltspath.content.domain.exception.CourseImportInvalidException;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Row;
import com.ieltspath.content.domain.vo.CourseImportTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a checked workbook as a draft the author can open again, see what is wrong in, correct and import. A draft holds
 * the rows as written; each time it is opened or saved it is checked again against the catalogue as it is then, so the
 * problems shown are never stale. Importing goes through the same checks and the same transaction as a file.
 */
@Service
public class CourseImportDraftUseCases {

    public static final int MAX_DRAFTS_PER_AUTHOR = 20;
    private static final int MAX_CELL_CHARACTERS = 20_000;
    private static final int MAX_ROWS = 2000;

    private final CourseImportUseCases imports;
    private final CourseImportDraftStore store;
    private final Clock clock;

    public CourseImportDraftUseCases(CourseImportUseCases imports, CourseImportDraftStore store, Clock clock) {
        this.imports = imports;
        this.store = store;
        this.clock = clock;
    }

    /** A draft and what the checks say about it now. */
    public record Checked(Draft draft, CourseImportUseCases.Report report) {
    }

    /** Reads a workbook and keeps it. A file that cannot be read as a whole is refused with its problems, kept nowhere. */
    @Transactional
    public Checked create(UUID author, String filename, byte[] content) {
        CourseWorkbookReader.ReadResult read = imports.read(filename, content);
        if (read.model() == null || !read.issues().isEmpty()) {
            throw new CourseImportInvalidException(read.issues(), List.of());
        }
        if (store.countDrafts(author) >= MAX_DRAFTS_PER_AUTHOR) {
            throw new IllegalArgumentException("Bạn đã có " + MAX_DRAFTS_PER_AUTHOR
                    + " bản nháp. Hãy nhập hoặc xóa bớt bản nháp trước khi tạo thêm.");
        }
        Instant now = clock.instant();
        Draft draft = new Draft(UUID.randomUUID(), author, clip(filename), CourseImportDraftStore.DRAFT, null,
                read.model(), now, now);
        store.insert(draft);
        return new Checked(draft, imports.validateModel(draft.model()));
    }

    @Transactional(readOnly = true)
    public List<CourseImportDraftStore.Summary> list(UUID author) {
        return store.list(author);
    }

    @Transactional(readOnly = true)
    public Checked get(UUID author, UUID id) {
        Draft draft = require(author, id);
        return new Checked(draft, imports.validateModel(draft.model()));
    }

    /** Replaces the rows of a draft that is not imported yet with the edited ones. */
    @Transactional
    public Checked update(UUID author, UUID id, Map<String, List<Row>> sheets) {
        Draft draft = requireOpen(author, id);
        CourseImportModel model = new CourseImportModel(draft.model().templateVersion(), sanitize(sheets));
        Instant now = clock.instant();
        store.updateContent(author, id, model, now);
        Draft saved = new Draft(draft.id(), draft.authorId(), draft.filename(), draft.status(), null, model,
                draft.createdAt(), now);
        return new Checked(saved, imports.validateModel(model));
    }

    /** Creates the course from the draft, or answers with its problems and creates nothing. */
    @Transactional
    public CourseImportUseCases.Imported importDraft(UUID author, UUID id) {
        Draft draft = requireOpen(author, id);
        CourseImportUseCases.Imported imported = imports.executeModel(author, draft.model());
        store.markImported(author, id, imported.courseId(), clock.instant());
        return imported;
    }

    @Transactional
    public void delete(UUID author, UUID id) {
        if (!store.delete(author, id)) throw new CourseImportDraftNotFoundException();
    }

    private Draft require(UUID author, UUID id) {
        return store.find(author, id).orElseThrow(CourseImportDraftNotFoundException::new);
    }

    private Draft requireOpen(UUID author, UUID id) {
        Draft draft = require(author, id);
        if (CourseImportDraftStore.IMPORTED.equals(draft.status())) {
            throw new IllegalStateException("Bản nháp này đã được nhập thành khóa học nên không sửa được nữa.");
        }
        return draft;
    }

    /** Only the sheets and columns of the template survive, so a draft can hold nothing the file could not. */
    private static Map<String, List<Row>> sanitize(Map<String, List<Row>> sheets) {
        Map<String, List<Row>> clean = new LinkedHashMap<>();
        int total = 0;
        for (String sheet : CourseImportTemplate.SHEETS.keySet()) {
            Set<String> columns = Set.copyOf(CourseImportTemplate.columnNames(sheet));
            List<Row> rows = new ArrayList<>();
            for (Row row : sheets.getOrDefault(sheet, List.of())) {
                Map<String, String> cells = new HashMap<>();
                for (Map.Entry<String, String> cell : row.cells().entrySet()) {
                    if (!columns.contains(cell.getKey())) continue;
                    String value = cell.getValue() == null ? "" : cell.getValue().strip();
                    if (value.length() > MAX_CELL_CHARACTERS) {
                        throw new IllegalArgumentException("Một ô dài quá " + MAX_CELL_CHARACTERS + " ký tự.");
                    }
                    cells.put(cell.getKey(), value);
                }
                columns.forEach(column -> cells.putIfAbsent(column, ""));
                rows.add(new Row(row.number(), cells));
                total++;
            }
            clean.put(sheet, rows);
        }
        if (total > MAX_ROWS) throw new IllegalArgumentException("Bản nháp vượt " + MAX_ROWS + " dòng dữ liệu.");
        return clean;
    }

    private static String clip(String filename) {
        String name = filename == null || filename.isBlank() ? "course.xlsx" : filename.strip();
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
