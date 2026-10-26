package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.ContentOverviewReader;
import com.ieltspath.content.application.result.ContentOverviewResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetContentOverviewUseCase {
    private final ContentOverviewReader reader;

    public GetContentOverviewUseCase(ContentOverviewReader reader) {
        this.reader = reader;
    }

    public ContentOverviewResult execute() {
        return reader.read();
    }
}
