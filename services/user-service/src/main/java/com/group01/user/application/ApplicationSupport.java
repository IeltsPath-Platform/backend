package com.group01.user.application;

import com.group01.user.application.query.PageQuery;
import com.group01.user.application.result.PageResult;
import com.group01.user.domain.exception.ResourceNotFoundException;
import com.group01.user.domain.vo.OwnedPage;

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
