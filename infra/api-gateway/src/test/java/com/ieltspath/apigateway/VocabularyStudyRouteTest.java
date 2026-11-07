package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VocabularyStudyRouteTest {
    @Test void libraryAccessUrlIsDeclaredAndWiredWithoutStartupDependency() throws Exception {
        assertEquals("${ACCESS_SERVICE_URL:http://localhost:8084}",ServiceUrlWiringConfigTest.configRepoYaml("library-service").getProperty("library.access.base-url"));
        String compose=ServiceUrlWiringConfigTest.composeBlock("library-service");
        assertTrue(compose.contains("ACCESS_SERVICE_URL: http://access-service:8084"));
        assertFalse(compose.contains("depends_on: access-service"));
    }
    @Test void authenticatedStudyRoutesGoToLibraryAndUseDefaultRateTier() throws Exception {
        Map<String,Object> config;
        try(var input=Files.newInputStream(Path.of("../config-server/config-repo/api-gateway.yaml"))){config=new Yaml().load(input);}
        var gateway=nested(config,"spring","cloud","gateway","server","webflux");
        var routes=(List<Map<String,Object>>)gateway.get("routes");
        var matching=routes.stream().filter(this::matchesStudy).toList();
        assertEquals(1,matching.size());assertEquals("library-personal-service",matching.getFirst().get("id"));
        assertEquals("lb://LIBRARY-SERVICE",matching.getFirst().get("uri"));
        var defaults=(List<Map<String,Object>>)gateway.get("default-filters");
        assertTrue(defaults.stream().anyMatch(f->"ClientRateLimit".equals(f.get("name")) && "default".equals(((Map<?,?>)f.get("args")).get("tier"))));
    }
    private boolean matchesStudy(Map<String,Object> route) {
        var matcher=new AntPathMatcher();
        return ((List<?>)route.get("predicates")).stream().map(Object::toString).filter(p->p.startsWith("Path="))
            .flatMap(p->Arrays.stream(p.substring(5).split(",")))
            .anyMatch(p->matcher.match(p,"/api/learning-support/vocabulary-study/me/summary"));
    }
    private Map<String,Object> nested(Map<String,Object> root,String... keys) {
        Map<String,Object> current=root;for(String key:keys)current=(Map<String,Object>)current.get(key);return current;
    }
}
