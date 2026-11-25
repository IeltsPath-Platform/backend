package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AuthorPackageReader;
import com.ieltspath.content.application.result.AuthorPackageSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListAuthorPackagesUseCase {

    public static final int MAX_PAGE_SIZE = 100;

    private final AuthorPackageReader reader;

    public ListAuthorPackagesUseCase(AuthorPackageReader reader) {
        this.reader = reader;
    }

    /** One page of the catalog, newest first. {@code page} is zero-based. */
    public Page execute(AuthorPackageReader.Filter filter, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Trang phải từ 0 trở lên và kích thước trang từ 1 đến " + MAX_PAGE_SIZE);
        }
        AuthorPackageReader.Page result = reader.list(filter, page, size);
        return new Page(result.totalElements(), page, size, result.items());
    }

    public record Page(long totalElements, int page, int size, List<AuthorPackageSummary> items) {
        public int totalPages() {
            return (int) ((totalElements + size - 1) / size);
        }
    }
}
