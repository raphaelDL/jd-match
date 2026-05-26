package com.rafa.jdmatch.jd;

import java.util.UUID;

/**
 * A past JD that is similar to a query JD, with its embedding-similarity {@code score}
 * (higher is more similar). Returned by {@link JdSimilarityService#findSimilar}.
 */
public record SimilarJd(UUID jdId, String roleTitle, String seniority, double score) {
}
