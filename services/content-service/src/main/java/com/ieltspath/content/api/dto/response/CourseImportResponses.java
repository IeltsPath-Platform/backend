package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.usecase.CourseImportUseCases;
import com.ieltspath.content.domain.service.CourseImportValidator;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import com.ieltspath.content.domain.vo.CourseImportOutline;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CourseImportResponses {

    private CourseImportResponses() {
    }

    public record IssueResponse(String sheet, int row, String column, String code, String message) {
        public static IssueResponse from(Issue issue) {
            return new IssueResponse(issue.sheet(), issue.row(), issue.column(), issue.code(), issue.message());
        }
    }

    public record SummaryResponse(int topics, int lessons, int questions, int packages) {
        static SummaryResponse from(CourseImportValidator.Summary summary) {
            return summary == null ? null : new SummaryResponse(summary.topics(), summary.lessons(),
                    summary.questions(), summary.packages());
        }
    }

    /** The result of a check. {@code summary} is null when the file could not be read. */
    public record Report(boolean valid, List<IssueResponse> errors, List<IssueResponse> warnings,
                         SummaryResponse summary, CourseImportOutline outline) {
        public static Report from(CourseImportUseCases.Report report) {
            return new Report(report.valid(), report.errors().stream().map(IssueResponse::from).toList(),
                    report.warnings().stream().map(IssueResponse::from).toList(),
                    SummaryResponse.from(report.summary()), report.outline());
        }
    }

    public record Imported(UUID courseId, List<UUID> topicIds, List<UUID> lessonIds, List<UUID> questionIds,
                           List<UUID> packageIds, Map<UUID, String> packageTitles, List<IssueResponse> warnings) {
        public static Imported from(CourseImportUseCases.Imported imported) {
            return new Imported(imported.courseId(), imported.topicIds(), imported.lessonIds(),
                    imported.questionIds(), imported.packageIds(), imported.packageTitles(),
                    imported.warnings().stream().map(IssueResponse::from).toList());
        }
    }

    public record DraftSummary(UUID id, String filename, String status, UUID importedCourseId, String courseCode,
                               String courseName, java.time.Instant updatedAt) {
        public static DraftSummary from(com.ieltspath.content.application.port.CourseImportDraftStore.Summary summary) {
            return new DraftSummary(summary.id(), summary.filename(), summary.status(), summary.importedCourseId(),
                    summary.courseCode(), summary.courseName(), summary.updatedAt());
        }
    }

    /** A kept workbook: its rows, as the author can edit them, and what the checks say about them now. */
    public record DraftDetail(UUID id, String filename, String status, UUID importedCourseId, java.time.Instant updatedAt,
                              Map<String, List<CourseImportModel.Row>> sheets, Report report) {
        public static DraftDetail from(com.ieltspath.content.application.usecase.CourseImportDraftUseCases.Checked checked) {
            var draft = checked.draft();
            return new DraftDetail(draft.id(), draft.filename(), draft.status(), draft.importedCourseId(),
                    draft.updatedAt(), draft.model().sheets(), Report.from(checked.report()));
        }
    }
}
