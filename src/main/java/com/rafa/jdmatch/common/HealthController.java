package com.rafa.jdmatch.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal liveness endpoint. Stands in for production health checks
 * (e.g. Spring Boot Actuator) until that's wired up.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public HealthStatus health() {
        return HealthStatus.up("jd-match");
    }
}
