package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lists the question formats a learner can filter practice tests of one skill by. */
@Service
public class ListPracticeTestFormatsUseCase {
    private final PracticeTestCatalogReader reader;

    public ListPracticeTestFormatsUseCase(PracticeTestCatalogReader reader) { this.reader = reader; }

    @Transactional(readOnly = true)
    public List<PracticeTestCatalogReader.FormatCount> execute(Skill skill) {
        if (skill == Skill.ALL) return List.of();
        return reader.listFormats(skill);
    }
}
