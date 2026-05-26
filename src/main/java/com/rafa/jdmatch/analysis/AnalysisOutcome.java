package com.rafa.jdmatch.analysis;

/**
 * The result of {@link AnalysisService#analyze} plus whether it was produced fresh
 * ({@code created == true}) or served from the idempotency cache. Lets the controller
 * answer 201 vs 200 without leaking a transient flag into the response body.
 */
public record AnalysisOutcome(AnalysisResult result, boolean created) {
}
