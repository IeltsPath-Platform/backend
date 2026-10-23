package com.ieltspath.content.application.service;

import com.ieltspath.content.application.port.CourseCatalogReader;
import com.ieltspath.content.domain.vo.CourseCatalogFacts;

import java.util.Map;
import java.util.Set;

/** A catalogue that knows nothing: every point, package and question a draft names is unknown, no code is taken. */
public class CourseCatalogReaderStub implements CourseCatalogReader {

    @Override
    public CourseCatalogFacts read(CourseCatalogFacts.Query query) {
        return new CourseCatalogFacts(Map.of(), Set.of(), Map.of(), Map.of(), Map.of(), Set.of(), Map.of(), Set.of(),
                Map.of(), Set.of(), Set.of(), Set.of(), Set.of(), false, Set.of());
    }
}
