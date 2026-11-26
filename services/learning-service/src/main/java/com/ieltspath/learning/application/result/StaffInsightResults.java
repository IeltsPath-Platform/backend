package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.StaffRecords.EdgeStat;
import com.ieltspath.learning.domain.vo.StaffRecords.KnowledgePointStat;
import com.ieltspath.learning.domain.vo.StaffRecords.QuestionStat;
import java.util.List;

/** Results of staff insight endpoints. Names of knowledge points come from Content; here only ids. */
public final class StaffInsightResults {
    private StaffInsightResults() {}

    /** {@code probes}, {@code passed} and {@code concludedAsRoot} are over concluded diagnoses. */
    public record TreeQuality(List<EdgeStat> edges, List<KnowledgePointStat> knowledgePoints) {}

    public record QuestionStats(List<QuestionStat> questions) {}
}
