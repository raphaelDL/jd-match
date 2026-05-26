package com.rafa.jdmatch;

import org.testcontainers.utility.DockerImageName;

/**
 * Shared container images for tests. Since Phase 3, the schema requires the pgvector
 * extension, so integration tests run against {@code pgvector/pgvector:pg16} (Postgres 16
 * plus pgvector) rather than the stock {@code postgres:16} image.
 */
public final class TestImages {

    public static final DockerImageName PGVECTOR =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    private TestImages() {
    }
}
