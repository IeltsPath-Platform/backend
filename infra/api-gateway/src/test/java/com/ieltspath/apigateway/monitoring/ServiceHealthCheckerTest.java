package com.ieltspath.apigateway.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceHealthCheckerTest {
    @Test
    void reportsExpectedServiceWithoutInstancesAsDown() {
        ReactiveDiscoveryClient discovery = mock(ReactiveDiscoveryClient.class);
        when(discovery.getServices()).thenReturn(Flux.empty());
        when(discovery.getInstances("access-service")).thenReturn(Flux.empty());
        ServiceHealthChecker checker = checker(discovery, List.of("access-service"), request ->
                Mono.just(health(HttpStatus.OK, "UP")));

        SystemServicesResponse response = checker.checkServices().block(Duration.ofSeconds(5));
        ServiceHealth service = service(response, "ACCESS-SERVICE");
        assertThat(service.status()).isEqualTo(HealthStatus.DOWN);
        assertThat(service.instances()).isEmpty();
    }

    @Test
    void reportsGatewayOnlyOnceWhenItIsRegisteredInEureka() {
        ReactiveDiscoveryClient discovery = mock(ReactiveDiscoveryClient.class);
        when(discovery.getServices()).thenReturn(Flux.just("api-gateway"));
        ServiceHealthChecker checker = checker(discovery, List.of("api-gateway"), request ->
                Mono.just(health(HttpStatus.OK, "UP")));

        SystemServicesResponse response = checker.checkServices().block(Duration.ofSeconds(5));

        assertThat(response.services().stream().filter(service -> service.serviceId().equals("API-GATEWAY"))).hasSize(1);
        assertThat(service(response, "API-GATEWAY").status()).isEqualTo(HealthStatus.UP);
        verify(discovery, never()).getInstances("api-gateway");
    }

    @Test
    void boundsConcurrentHealthRequests() throws Exception {
        ReactiveDiscoveryClient discovery = mock(ReactiveDiscoveryClient.class);
        when(discovery.getServices()).thenReturn(Flux.just("content-service"));
        List<DefaultServiceInstance> instances = java.util.stream.IntStream.range(0, 100)
                .mapToObj(index -> new DefaultServiceInstance("instance-" + index, "content-service",
                        "content-" + index, 8082, false))
                .toList();
        when(discovery.getInstances("content-service")).thenReturn(Flux.fromIterable(instances));
        AtomicInteger currentRequests = new AtomicInteger();
        AtomicInteger maximumRequests = new AtomicInteger();
        ServiceHealthChecker checker = checker(discovery, List.of(), request -> {
            int active = currentRequests.incrementAndGet();
            maximumRequests.accumulateAndGet(active, Math::max);
            return Mono.delay(Duration.ofMillis(15))
                    .map(ignored -> health(HttpStatus.OK, "UP"))
                    .doFinally(signal -> currentRequests.decrementAndGet());
        });

        CompletableFuture<SystemServicesResponse> firstSnapshot = CompletableFuture.supplyAsync(
                () -> checker.checkServices().block(Duration.ofSeconds(5)));
        CompletableFuture<SystemServicesResponse> secondSnapshot = CompletableFuture.supplyAsync(
                () -> checker.checkServices().block(Duration.ofSeconds(5)));
        SystemServicesResponse response = firstSnapshot.get(5, TimeUnit.SECONDS);
        SystemServicesResponse secondResponse = secondSnapshot.get(5, TimeUnit.SECONDS);

        assertThat(service(response, "CONTENT-SERVICE").instances()).hasSize(100);
        assertThat(service(secondResponse, "CONTENT-SERVICE").instances()).hasSize(100);
        assertThat(maximumRequests.get()).isLessThanOrEqualTo(32);
        assertThat(maximumRequests.get()).isGreaterThan(1);
    }

    @Test
    void reportsInstancesIndividuallyAndServiceUpWhenAnyInstanceIsUp() {
        ReactiveDiscoveryClient discovery = mock(ReactiveDiscoveryClient.class);
        when(discovery.getServices()).thenReturn(Flux.just("content-service"));
        when(discovery.getInstances("content-service")).thenReturn(Flux.just(
                new DefaultServiceInstance("content-up", "content-service", "content-a", 8082, false),
                new DefaultServiceInstance("content-down", "content-service", "content-b", 8082, false)
        ));
        ServiceHealthChecker checker = checker(discovery, List.of(), request -> {
            String host = request.url().getHost();
            return Mono.just(health(HttpStatus.OK, "content-a".equals(host) ? "UP" : "DOWN"));
        });

        SystemServicesResponse response = checker.checkServices().block(Duration.ofSeconds(5));
        ServiceHealth service = service(response, "CONTENT-SERVICE");
        assertThat(service.status()).isEqualTo(HealthStatus.UP);
        assertThat(service.instances()).extracting(ServiceHealthInstance::status)
                .containsExactlyInAnyOrder(HealthStatus.UP, HealthStatus.DOWN);
        assertThat(service.instances()).allSatisfy(instance -> assertThat(instance.responseMs()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void mapsOutOfServiceAndUnknownStatusesWithoutExposingHealthDetails() {
        ReactiveDiscoveryClient discovery = mock(ReactiveDiscoveryClient.class);
        when(discovery.getServices()).thenReturn(Flux.just("maintenance-service", "unknown-service"));
        when(discovery.getInstances("maintenance-service")).thenReturn(Flux.just(
                new DefaultServiceInstance("maintenance", "maintenance-service", "maintenance", 8080, false)));
        when(discovery.getInstances("unknown-service")).thenReturn(Flux.just(
                new DefaultServiceInstance("unknown", "unknown-service", "unknown", 8080, false)));
        ServiceHealthChecker checker = checker(discovery, List.of(), request -> {
            if (request.url().getHost().equals("maintenance")) {
                return Mono.just(health(HttpStatus.SERVICE_UNAVAILABLE, "OUT_OF_SERVICE"));
            }
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"details\":{\"diskSpace\":\"hidden\"}}")
                    .build());
        });

        SystemServicesResponse response = checker.checkServices().block(Duration.ofSeconds(5));
        assertThat(service(response, "MAINTENANCE-SERVICE").instances().getFirst().status())
                .isEqualTo(HealthStatus.OUT_OF_SERVICE);
        assertThat(service(response, "UNKNOWN-SERVICE").instances().getFirst().status())
                .isEqualTo(HealthStatus.UNKNOWN);
    }

    @Test
    void mapsTimeoutToDown() {
        ReactiveDiscoveryClient discovery = mock(ReactiveDiscoveryClient.class);
        when(discovery.getServices()).thenReturn(Flux.just("slow-service"));
        when(discovery.getInstances("slow-service")).thenReturn(Flux.just(
                new DefaultServiceInstance("slow", "slow-service", "slow", 8080, false)));
        ServiceHealthChecker checker = checker(discovery, List.of(), request -> request.url().getHost().equals("slow")
                ? Mono.never()
                : Mono.just(health(HttpStatus.OK, "UP")));

        SystemServicesResponse response = checker.checkServices().block(Duration.ofSeconds(5));
        ServiceHealth service = service(response, "SLOW-SERVICE");
        assertThat(service.status()).isEqualTo(HealthStatus.DOWN);
        assertThat(service.instances().getFirst().status()).isEqualTo(HealthStatus.DOWN);
        assertThat(service.instances().getFirst().responseMs()).isGreaterThanOrEqualTo(1900);
    }

    private ServiceHealthChecker checker(ReactiveDiscoveryClient discovery, List<String> expected,
                                         ExchangeFunction exchangeFunction) {
        MonitoringProperties properties = new MonitoringProperties();
        properties.setExpectedServices(expected);
        return new ServiceHealthChecker(discovery, WebClient.builder().exchangeFunction(exchangeFunction), properties,
                "http://config-server:8888", "http://eureka-server:8761/eureka/", 8080);
    }

    private ClientResponse health(HttpStatus httpStatus, String status) {
        return ClientResponse.create(httpStatus)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"status\":\"" + status + "\",\"components\":{\"details\":{\"hidden\":true}}}")
                .build();
    }

    private ServiceHealth service(SystemServicesResponse response, String serviceId) {
        return response.services().stream().filter(service -> service.serviceId().equals(serviceId)).findFirst().orElseThrow();
    }
}
