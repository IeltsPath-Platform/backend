package com.group01.assessment.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group01.assessment.application.exception.ContentUnavailableException;
import com.group01.assessment.application.port.ContentPackageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Reads {@code GET /internal/learning-content/package-versions/{id}} with the caller's bearer and correlation id. */
@Component
public class ContentPackageClient implements ContentPackageProvider {
    private final RestClient restClient;

    public ContentPackageClient(RestClient.Builder builder,
                                @Value("${assessment.content.base-url:http://localhost:8082}") String contentBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
    }

    @Override
    public Optional<PackageVersion> findPackageVersion(UUID packageVersionId) {
        VersionResponse response;
        try {
            response = restClient.get()
                    .uri("/internal/learning-content/package-versions/{id}", packageVersionId)
                    .header(HttpHeaders.AUTHORIZATION, GatewayRequestContext.bearerToken())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                    .retrieve()
                    .body(VersionResponse.class);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new ContentUnavailableException("Content Service could not provide the package version", exception);
        }
        return Optional.of(toPackageVersion(packageVersionId, response));
    }

    private static PackageVersion toPackageVersion(UUID requested, VersionResponse response) {
        if (response == null || !requested.equals(response.packageVersionId()) || response.packageType() == null
                || response.sections() == null) {
            throw invalid();
        }
        return new PackageVersion(response.packageVersionId(), response.packageType(),
                response.sections().stream().map(ContentPackageClient::toSection).toList());
    }

    private static Section toSection(SectionResponse section) {
        if (section == null || section.sectionId() == null || section.items() == null) {
            throw invalid();
        }
        return new Section(section.sectionId(), section.title(), section.skill(), section.instructions(),
                section.sortOrder(), section.passage(), toAudio(section.audio()),
                section.items().stream().map(ContentPackageClient::toItem).toList());
    }

    private static Audio toAudio(AudioResponse audio) {
        if (audio == null) return null;
        if (audio.assetId() == null || audio.mediaUrl() == null || audio.mediaUrl().isBlank()
                || (audio.durationSeconds() != null && audio.durationSeconds() < 0)) {
            throw invalid();
        }
        return new Audio(audio.assetId(), audio.mediaUrl(), audio.durationSeconds(), audio.transcript());
    }

    private static Item toItem(ItemResponse item) {
        if (item == null || item.questionVersionId() == null || item.maxScore() == null
                || item.maxScore().signum() <= 0) {
            throw invalid();
        }
        List<Option> options = item.options() == null ? null : item.options().stream()
                .map(option -> new Option(option.optionKey(), option.content(), option.sortOrder()))
                .toList();
        List<KnowledgePointWeight> knowledgePoints = item.knowledgePointMappings() == null ? List.of()
                : item.knowledgePointMappings().stream().map(mapping -> {
                    if (mapping == null || mapping.knowledgePointId() == null || mapping.weight() == null
                            || mapping.weight().signum() < 0) {
                        throw invalid();
                    }
                    return new KnowledgePointWeight(mapping.knowledgePointId(), mapping.weight());
                }).toList();
        return new Item(item.questionVersionId(), item.sortOrder(), item.stem(), options, item.answerSpec(),
                item.explanation(), item.maxScore(), knowledgePoints);
    }

    private static ContentUnavailableException invalid() {
        return new ContentUnavailableException("Content Service returned an invalid package version");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VersionResponse(UUID packageVersionId, String packageType, List<SectionResponse> sections) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SectionResponse(UUID sectionId, String title, String skill, String instructions, int sortOrder,
                                   String passage, AudioResponse audio, List<ItemResponse> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AudioResponse(UUID assetId, String mediaUrl, Integer durationSeconds, String transcript) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ItemResponse(UUID questionVersionId, int sortOrder, String stem, List<OptionResponse> options,
                                Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                                List<MappingResponse> knowledgePointMappings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OptionResponse(String optionKey, String content, int sortOrder) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MappingResponse(UUID knowledgePointId, BigDecimal weight) {}
}
