package com.ieltspath.user.infrastructure.client;

import com.ieltspath.user.application.port.StudySourceVerifier;
import com.ieltspath.user.domain.exception.StudyTrackingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Component
public class HttpStudySourceVerifier implements StudySourceVerifier {
    private final RestClient client;
    private final String learningUrl, assessmentUrl, libraryUrl;
    public HttpStudySourceVerifier(@Value("${app.study-time.learning-url:http://localhost:8086}") String learningUrl,
            @Value("${app.study-time.assessment-url:http://localhost:8083}") String assessmentUrl,
            @Value("${app.study-time.library-url:http://localhost:8081}") String libraryUrl) {
        this.learningUrl=learningUrl; this.assessmentUrl=assessmentUrl; this.libraryUrl=libraryUrl;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        client=RestClient.builder().requestFactory(factory).build();
    }
    public Context verify(String type, UUID sourceId) {
        String url = switch(type) {
            case "LESSON", "PRACTICE", "REVIEW" -> learningUrl+"/api/learning/statistics/study-context";
            case "ASSESSMENT" -> assessmentUrl+"/api/assessments/me/statistics/study-context";
            case "VOCABULARY_REVIEW" -> libraryUrl+"/api/learning-support/flashcards/statistics/study-context";
            default -> throw new IllegalArgumentException("Unsupported study source");
        };
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) throw new StudyTrackingException("SOURCE_UNAVAILABLE");
        HttpServletRequest request = servlet.getRequest();
        try {
            Context context=client.get().uri(url+"?sourceType={type}&sourceId={id}",type,sourceId)
                    .headers(headers -> {
                        String bearer=request.getHeader("Authorization");
                        if (bearer!=null) headers.set("Authorization",bearer);
                        String correlation=request.getHeader("X-Correlation-Id");
                        if(correlation!=null) headers.set("X-Correlation-Id",correlation);
                    }).retrieve().body(Context.class);
            if(context==null || !sourceId.equals(context.sourceId())) throw new StudyTrackingException("SOURCE_UNAVAILABLE");
            return context;
        } catch(HttpClientErrorException e) {
            if(e.getStatusCode().value()==404 || e.getStatusCode().value()==403 || e.getStatusCode().value()==409)
                throw new StudyTrackingException("SOURCE_NOT_AVAILABLE");
            throw new StudyTrackingException("SOURCE_UNAVAILABLE");
        } catch(RestClientException e) { throw new StudyTrackingException("SOURCE_UNAVAILABLE"); }
    }
}
