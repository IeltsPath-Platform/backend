package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.application.result.ContentOverviewResult.QuestionLabel;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Reads the counts shown on the admin overview, each with one grouped query. */
public interface ContentOverviewReader {
    ContentOverviewResult read();

    /** The labels of the given question versions; an id that does not exist is left out. */
    List<QuestionLabel> questionLabels(Collection<UUID> questionVersionIds);
}