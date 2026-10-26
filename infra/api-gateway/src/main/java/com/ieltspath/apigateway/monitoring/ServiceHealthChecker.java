package com.ieltspath.apigateway.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class ServiceHealthChecker {
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_CONCURRENT_DISCOVERY_LOOKUPS = 16;
    private static final int MAX_CONCURRENT_HEALTH_CHECKS = 32;

    private final ReactiveDiscoveryClient discoveryClient;
    private final WebClient webClient;
    private final MonitoringProperties properties;
    private final String configServerUri;
    private final String eurekaUri;
    private final int gatewayPort;
    private final Semaphore healthCheckPermits = new Semaphore(MAX_CONCURRENT_HEALTH_CHECKS);

    public ServiceHealthChecker(
            ReactiveDiscoveryClient discoveryClient,
            WebClient.Builder webClientBuilder,
            MonitoringProperties properties,
            @Value("${SPRING_CLOUD_CONFIG_URI:http://localhost:8888}") String configServerUri,
            @Value("${EUREKA_URL:http://localhost:8761/eureka/}") String eurekaUri,
            @Value("${server.port:8080}") int gatewayPort
    ) {
        this.discoveryClient = discoveryClient;
        this.webClient = webClientBuilder.build();
        this.properties = properties;
        this.configServerUri = configServerUri;
        this.eurekaUri = eurekaUri;
        this.gatewayPort = gatewayPort;
    }

    public Mono<SystemServicesResponse> checkServices() {
        return discoveryClient.getServices()
                .collectList()
                .flatMap(discovered -> {
                    Map<String, String> serviceIds = new LinkedHashMap<>();
                    discovered.stream()
                            .filter(serviceId -> !"API-GATEWAY".equals(normalize(serviceId)))
                            .forEach(serviceId -> serviceIds.put(normalize(serviceId), serviceId));
                    properties.getExpectedServices().forEach(serviceId ->
                            serviceIds.putIfAbsent(normalize(serviceId), serviceId));
                    serviceIds.remove("API-GATEWAY");

                    Mono<List<ServiceHealth>> discoveredHealth = checkDiscoveredServices(serviceIds);
                    Mono<List<ServiceHealth>> platformHealth = checkPlatformServices().collectList();
                    return Mono.zip(discoveredHealth, platformHealth)
                            .map(health -> {
                                List<ServiceHealth> services = new ArrayList<>(health.getT1());
                                services.addAll(health.getT2());
                                services.sort(Comparator.comparing(ServiceHealth::serviceId));
                                return new SystemServicesResponse(Instant.now(), services);
                            });
                });
    }

    private Mono<List<ServiceHealth>> checkDiscoveredServices(Map<String, String> serviceIds) {
        return Flux.fromIterable(serviceIds.entrySet())
                .flatMap(entry -> discoveryClient.getInstances(entry.getValue())
                        .collectList()
                        .map(instances -> new DiscoveredService(entry.getKey(), instances)),
                        MAX_CONCURRENT_DISCOVERY_LOOKUPS)
                .collectList()
                .flatMap(discovered -> {
                    List<InstanceWork> work = discovered.stream()
                            .flatMap(service -> service.instances().stream()
                                    .map(instance -> new InstanceWork(service.serviceId(), instance)))
                            .toList();
                    return Flux.fromIterable(work)
                            .flatMap(task -> checkInstance(task.instance())
                                    .map(instance -> new CheckedInstance(task.serviceId(), instance)),
                                    MAX_CONCURRENT_HEALTH_CHECKS)
                            .collectList()
                            .map(checked -> discovered.stream()
                                    .map(group -> service(group.serviceId(), checked.stream()
                                            .filter(result -> result.serviceId().equals(group.serviceId()))
                                            .map(CheckedInstance::instance)
                                            .toList()))
                                    .toList());
                });
    }

    private Flux<ServiceHealth> checkPlatformServices() {
        ServiceHealth gateway = new ServiceHealth("API-GATEWAY", HealthStatus.UP, List.of(
                new ServiceHealthInstance("api-gateway", "http://localhost:" + gatewayPort, HealthStatus.UP, 0)
        ));
        Mono<ServiceHealth> configServer = checkUri("CONFIG-SERVER", "config-server", configServerUri,
                healthUri(configServerUri, false));
        Mono<ServiceHealth> eurekaServer = checkUri("EUREKA-SERVER", "eureka-server", eurekaUri,
                healthUri(eurekaUri, true));
        return Flux.merge(Mono.just(gateway), configServer, eurekaServer);
    }

    private Mono<ServiceHealth> checkUri(String serviceId, String instanceId, String displayUri, URI healthUri) {
        return checkHealth(instanceId, displayUri, healthUri)
                .map(instance -> service(serviceId, List.of(instance)));
    }

    private Mono<ServiceHealthInstance> checkInstance(ServiceInstance discovered) {
        URI baseUri = discovered.getUri();
        URI healthUri = baseUri.resolve("/actuator/health");
        String instanceId = discovered.getInstanceId() == null || discovered.getInstanceId().isBlank()
                ? baseUri.getAuthority()
                : discovered.getInstanceId();
        return checkHealth(instanceId, baseUri.toString(), healthUri);
    }

    private Mono<ServiceHealthInstance> checkHealth(String instanceId, String displayUri, URI healthUri) {
        long startedAt = System.nanoTime();
        return Mono.defer(() -> {
            if (!healthCheckPermits.tryAcquire()) {
                return Mono.just(new ServiceHealthInstance(instanceId, displayUri, HealthStatus.UNKNOWN, 0));
            }
            return webClient.get()
                    .uri(healthUri)
                    .exchangeToMono(response -> {
                        if (!response.statusCode().is2xxSuccessful() && response.statusCode().value() != 503) {
                            return Mono.just(HealthStatus.DOWN);
                        }
                        return response.bodyToMono(JsonNode.class)
                                .map(body -> parseStatus(body.path("status").asText(null)))
                                .defaultIfEmpty(response.statusCode().is2xxSuccessful() ? HealthStatus.UNKNOWN : HealthStatus.DOWN);
                    })
                    .timeout(HEALTH_TIMEOUT)
                    .map(status -> instance(instanceId, displayUri, status, startedAt))
                    .onErrorResume(exception -> Mono.just(instance(instanceId, displayUri, HealthStatus.DOWN, startedAt)))
                    .doFinally(signal -> healthCheckPermits.release());
        });
    }

    private ServiceHealthInstance instance(String instanceId, String displayUri, HealthStatus status, long startedAt) {
        long responseMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        return new ServiceHealthInstance(instanceId, displayUri, status, responseMs);
    }

    private ServiceHealth service(String serviceId, List<ServiceHealthInstance> instances) {
        HealthStatus status = instances.stream().anyMatch(instance -> instance.status() == HealthStatus.UP)
                ? HealthStatus.UP
                : HealthStatus.DOWN;
        return new ServiceHealth(serviceId, status, instances);
    }

    private HealthStatus parseStatus(String value) {
        if (value == null) {
            return HealthStatus.UNKNOWN;
        }
        try {
            return HealthStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return HealthStatus.UNKNOWN;
        }
    }

    private URI healthUri(String base, boolean eurekaBase) {
        URI uri = URI.create(base);
        return uri.resolve(eurekaBase ? "../actuator/health" : "/actuator/health");
    }

    private String normalize(String serviceId) {
        return serviceId.toUpperCase(Locale.ROOT);
    }

    private record DiscoveredService(String serviceId, List<ServiceInstance> instances) {
    }

    private record InstanceWork(String serviceId, ServiceInstance instance) {
    }

    private record CheckedInstance(String serviceId, ServiceHealthInstance instance) {
    }
}
