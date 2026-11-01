package com.ieltspath.learning.application;

import com.ieltspath.learning.application.query.PageQuery;
import com.ieltspath.learning.application.result.PageResult;
import com.ieltspath.learning.domain.exception.ResourceNotFoundException;
import com.ieltspath.learning.domain.vo.OwnedPage;

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
