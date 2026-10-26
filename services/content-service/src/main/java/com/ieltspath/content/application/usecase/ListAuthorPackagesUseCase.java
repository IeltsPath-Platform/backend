package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AuthorPackageReader;
import com.ieltspath.content.application.result.AuthorPackageSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListAuthorPackagesUseCase {

    private final AuthorPackageReader reader;

    public ListAuthorPackagesUseCase(AuthorPackageReader reader) {
        this.reader = reader;
    }

    public List<AuthorPackageSummary> execute(AuthorPackageReader.Filter filter) {
        return reader.list(filter);
    }
}
