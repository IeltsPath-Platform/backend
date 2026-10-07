package com.ieltspath.library.application;

import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.vo.OwnedPage;

public final class ApplicationSupport {
    private ApplicationSupport() {
    }

    public static <T> PageResult<T> page(OwnedPage<T> owned, PageQuery query) {
        return new PageResult<>(owned.items(), query.page(), query.size(), owned.total());
    }

    public static <T> T required(java.util.Optional<T> value) {
        return value.orElseThrow(ResourceNotFoundException::new);
    }
}
