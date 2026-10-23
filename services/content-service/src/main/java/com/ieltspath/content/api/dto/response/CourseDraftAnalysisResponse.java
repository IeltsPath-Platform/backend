package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.domain.vo.CourseDraftAnalysis;

import java.util.List;
import java.util.UUID;

public record CourseDraftAnalysisResponse(boolean valid, List<IssueResponse> errors, List<IssueResponse> warnings,
                                          List<GapResponse> gaps, CoverageResponse coverage) {

    public static CourseDraftAnalysisResponse from(CourseDraftAnalysis analysis) {
        return new CourseDraftAnalysisResponse(analysis.valid(),
                analysis.errors().stream().map(IssueResponse::from).toList(),
                analysis.warnings().stream().map(IssueResponse::from).toList(),
                analysis.gaps().stream().map(gap -> new GapResponse(gap.knowledgePointId(), gap.reason())).toList(),
                new CoverageResponse(analysis.coverage().expected(), analysis.coverage().covered()));
    }

    public record IssueResponse(String code, String subject, String related) {
        public static IssueResponse from(CourseDraftAnalysis.Issue issue) {
            return new IssueResponse(issue.code(), issue.subject(), issue.related());
        }
    }

    public record GapResponse(UUID knowledgePointId, String reason) {
    }

    public record CoverageResponse(int expected, int covered) {
    }
}
