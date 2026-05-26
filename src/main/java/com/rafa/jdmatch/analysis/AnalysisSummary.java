package com.rafa.jdmatch.analysis;

import java.time.Instant;
import java.util.UUID;

/**
 * A lightweight view of a stored analysis for list endpoints — id, fit score, and
 * provenance, without the full gap analysis.
 */
public record AnalysisSummary(UUID id, int overallFitScore, String model, Instant createdAt) {
}
