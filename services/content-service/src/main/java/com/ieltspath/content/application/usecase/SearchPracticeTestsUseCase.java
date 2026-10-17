package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.SearchPracticeTestsCommand;
import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.application.result.PracticeTestCardResult;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.TestSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchPracticeTestsUseCase {
    private final PracticeTestCatalogReader reader;
    private final MediaReferencePolicy mediaPolicy;

    public SearchPracticeTestsUseCase(PracticeTestCatalogReader reader, MediaReferencePolicy mediaPolicy) {
        this.reader = reader;
        this.mediaPolicy = mediaPolicy;
    }

    @Transactional(readOnly = true)
    public PracticeTestCatalogReader.Page execute(SearchPracticeTestsCommand command) {
        if (!"PART".equals(command.scope()) && !"FULL".equals(command.scope()))
            throw new IllegalArgumentException("scope must be FULL or PART");
        if ("FULL".equals(command.scope()) && command.partNo() != null)
            throw new IllegalArgumentException("partNo is only valid for PART scope");
        if (command.only() != null && !command.only().isEmpty()
                && command.exclude() != null && !command.exclude().isEmpty())
            throw new IllegalArgumentException("only and exclude cannot be used together");
        if (command.page() < 0 || command.size() < 1 || command.size() > 50)
            throw new IllegalArgumentException("Invalid page or size");
        if (command.query() != null && command.query().length() > 100)
            throw new IllegalArgumentException("query must be at most 100 characters");
        List<String> sources = command.sources() == null ? List.of() : command.sources();
        for (String source : sources) TestSource.valueOf(source);
        if (command.skill() == Skill.ALL) return new PracticeTestCatalogReader.Page(0, List.of());
        List<String> formats = command.formats() == null ? List.of() : command.formats();
        if (formats.size() > 10) throw new IllegalArgumentException("At most 10 formats");
        return reader.search(new PracticeTestCatalogReader.Search(command.skill(), command.scope(), command.partNo(),
                sources, command.query() == null ? null : command.query().strip(), command.only(), command.exclude(),
                command.page(), command.size(), formats));
    }

    public String coverUrl(PracticeTestCardResult card) {
        if (card.coverReference() == null) return null;
        try {
            return mediaPolicy.resolve(AssetType.IMAGE, card.coverReference());
        } catch (RuntimeException invalidReference) {
            return null;
        }
    }
}
