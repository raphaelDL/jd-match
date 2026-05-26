package com.rafa.jdmatch.common;

/**
 * Lightweight liveness payload returned by {@link HealthController}.
 */
public record HealthStatus(String status, String service) {

    public static HealthStatus up(String service) {
        return new HealthStatus("UP", service);
    }
}
