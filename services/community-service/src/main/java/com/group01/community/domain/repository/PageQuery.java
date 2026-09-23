package com.group01.community.domain.repository;

public record PageQuery(int page, int size, String sortProperty, boolean descending) {
}
