package com.ieltspath.library;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class StudyIndependenceTest {
    @Test void libraryStudyNeverImportsWebLearningOrCallsItsHttpApi() throws Exception {
        try(var paths=Files.walk(Path.of("src/main/java/com/ieltspath/library"))) {
            for(Path path:paths.filter(p->p.toString().endsWith(".java")).toList()) {
                String source=Files.readString(path);
                assertFalse(source.matches("(?s).*import\\s+com\\.ieltspath\\.learning\\..*"),path.toString());
                assertFalse(source.matches("(?s).*https?://[^\\s\"]*learning[^\\s\"]*.*"),path.toString());
                assertFalse(source.contains("library.learning.base-url"),path.toString());
            }
        }
    }
}
