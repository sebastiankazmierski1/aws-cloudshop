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
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final ApplicationAvailability availability;

    @GetMapping
    @Operation(summary = "Basic health check")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "product-service"
        ));
    }

    @GetMapping("/liveness")
    @Operation(summary = "Liveness probe for Kubernetes")
    public ResponseEntity<Map<String, Object>> liveness() {
        LivenessState livenessState = availability.getLivenessState();
        
        if (livenessState == LivenessState.CORRECT) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "livenessState", livenessState.name()
            ));
        }
        
        return ResponseEntity.internalServerError().body(Map.of(
                "status", "DOWN",
                "livenessState", livenessState.name()
        ));
    }

    @GetMapping("/readiness")
    @Operation(summary = "Readiness probe for Kubernetes")
    public ResponseEntity<Map<String, Object>> readiness() {
        ReadinessState readinessState = availability.getReadinessState();
        
        if (readinessState == ReadinessState.ACCEPTING_TRAFFIC) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "readinessState", readinessState.name()
            ));
        }
        
        return ResponseEntity.internalServerError().body(Map.of(
                "status", "DOWN",
                "readinessState", readinessState.name()
        ));
    }
}
