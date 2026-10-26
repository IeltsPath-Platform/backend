package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.ContentOverviewResult;

/** Reads the counts shown on the admin overview, each with one grouped query. */
public interface ContentOverviewReader {
    ContentOverviewResult read();
}
