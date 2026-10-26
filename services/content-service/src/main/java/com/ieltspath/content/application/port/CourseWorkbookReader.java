package com.ieltspath.content.application.port;

import com.ieltspath.content.domain.vo.CourseImportModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Reads a course import workbook into plain data. The file is untrusted: the reader refuses anything but a plain
 * {@code .xlsx}, formulas, macros, oversize cells and too many rows, and never logs a cell.
 */
public interface CourseWorkbookReader {

    /**
     * @return the model and the problems with the file itself (shape, version, limits). The model is null when the file
     *         could not be read at all. Problems with the values in the rows are for the validator.
     */
    ReadResult read(InputStream content) throws IOException;

    record ReadResult(CourseImportModel model, List<CourseImportModel.Issue> issues) {
        public ReadResult {
            issues = List.copyOf(issues);
        }
    }
}
