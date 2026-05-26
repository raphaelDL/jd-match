package com.rafa.jdmatch.analysis;

import java.time.Instant;
import java.util.UUID;

/**
 * The result of an analysis as returned by the service and serialized to clients:
 * the persisted id, the structured analysis, and the model/timestamp that produced it.
 */
public record AnalysisResult(UUID id, GapAnalysis analysis, String model, Instant createdAt) {
}
