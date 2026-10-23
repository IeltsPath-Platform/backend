package com.ieltspath.content.application.port;

import com.ieltspath.content.domain.vo.CourseCatalogFacts;

/** Reads, in a fixed number of queries, what the catalogue says about the things a course draft mentions. */
public interface CourseCatalogReader {

    CourseCatalogFacts read(CourseCatalogFacts.Query query);
}
