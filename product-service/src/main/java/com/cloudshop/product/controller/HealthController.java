package com.cloudshop.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check endpoints for load balancer and monitoring.
 * 
 * Java 25: Uses records for response types, pattern matching for state checks.
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final ApplicationAvailability availability;

    /**
     * Java 25: Record for health response - immutable and concise.
     */
    public record HealthResponse(
            String status,
            LocalDateTime timestamp,
            String service,
            String javaVersion,
            Runtime.Version runtimeVersion
    ) {
        public static HealthResponse up(String service) {
            return new HealthResponse(
                    "UP",
                    LocalDateTime.now(),
                    service,
                    System.getProperty("java.version"),
                    Runtime.version()
            );
        }
    }

    /**
     * Java 25: Record for probe responses.
     */
    public record ProbeResponse(String status, String state) {
        public static ProbeResponse up(String state) {
            return new ProbeResponse("UP", state);
        }
        
        public static ProbeResponse down(String state) {
            return new ProbeResponse("DOWN", state);
        }
    }

    @GetMapping
    @Operation(summary = "Basic health check")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.up("product-service"));
    }

    /**
     * Java 25: Pattern matching with switch for liveness state handling.
     */
    @GetMapping("/liveness")
    @Operation(summary = "Liveness probe for Kubernetes")
    public ResponseEntity<ProbeResponse> liveness() {
        LivenessState state = availability.getLivenessState();
        
        return switch (state) {
            case CORRECT -> ResponseEntity.ok(ProbeResponse.up(state.name()));
            case BROKEN -> ResponseEntity.internalServerError()
                    .body(ProbeResponse.down(state.name()));
        };
    }

    /**
     * Java 25: Pattern matching with switch for readiness state handling.
     */
    @GetMapping("/readiness")
    @Operation(summary = "Readiness probe for Kubernetes")
    public ResponseEntity<ProbeResponse> readiness() {
        ReadinessState state = availability.getReadinessState();
        
        return switch (state) {
            case ACCEPTING_TRAFFIC -> ResponseEntity.ok(ProbeResponse.up(state.name()));
            case REFUSING_TRAFFIC -> ResponseEntity.internalServerError()
                    .body(ProbeResponse.down(state.name()));
        };
    }

    /**
     * Endpoint showing JVM info using modern APIs.
     */
    @GetMapping("/info")
    @Operation(summary = "System information")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        Runtime.Version version = Runtime.version();
        Runtime runtime = Runtime.getRuntime();
        
        return ResponseEntity.ok(Map.of(
                "java", Map.of(
                        "version", version.toString(),
                        "feature", version.feature(),
                        "interim", version.interim(),
                        "update", version.update(),
                        "vendor", System.getProperty("java.vendor"),
                        "vmName", System.getProperty("java.vm.name")
                ),
                "memory", Map.of(
                        "maxMB", runtime.maxMemory() / (1024 * 1024),
                        "totalMB", runtime.totalMemory() / (1024 * 1024),
                        "freeMB", runtime.freeMemory() / (1024 * 1024),
                        "processors", runtime.availableProcessors()
                ),
                "service", "product-service",
                "timestamp", LocalDateTime.now()
        ));
    }
}
