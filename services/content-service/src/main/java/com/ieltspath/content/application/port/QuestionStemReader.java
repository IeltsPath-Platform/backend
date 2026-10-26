package com.ieltspath.content.application.port;

import java.util.List;
import java.util.UUID;

/** Reads the stems of the questions a knowledge point already has, so a draft does not repeat them. */
public interface QuestionStemReader {

    /** The stems of every version of every question tied to the knowledge point, as written. */
    List<String> stemsOf(UUID knowledgePointId);
}
