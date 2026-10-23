package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.usecase.CourseImportUseCases;
import com.ieltspath.content.domain.service.CourseImportValidator;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;

import java.util.List;
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
                         SummaryResponse summary) {
        public static Report from(CourseImportUseCases.Report report) {
            return new Report(report.valid(), report.errors().stream().map(IssueResponse::from).toList(),
                    report.warnings().stream().map(IssueResponse::from).toList(),
                    SummaryResponse.from(report.summary()));
        }
    }

    public record Imported(UUID courseId, List<UUID> topicIds, List<UUID> lessonIds, List<UUID> questionIds,
                           List<UUID> packageIds, List<IssueResponse> warnings) {
        public static Imported from(CourseImportUseCases.Imported imported) {
            return new Imported(imported.courseId(), imported.topicIds(), imported.lessonIds(),
                    imported.questionIds(), imported.packageIds(),
                    imported.warnings().stream().map(IssueResponse::from).toList());
        }
    }
}
