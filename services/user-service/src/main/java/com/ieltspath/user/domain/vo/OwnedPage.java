package com.ieltspath.user.domain.vo;

import java.util.List;

public record OwnedPage<T>(List<T> items, long total) {
}
