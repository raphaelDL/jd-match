package com.rafa.jdmatch.jd;

import com.rafa.jdmatch.TestImages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end RAG check: boots the full context with the real local embedding model
 * (all-MiniLM-L6-v2) and a pgvector container, indexes two clearly different JDs, and
 * confirms a backend-flavoured query ranks the backend JD first. The first run downloads
 * the ONNX model; afterwards it is served from the local cache.
 */
@SpringBootTest(properties = "anthropic.api-key=test-key")
@Testcontainers
class JdSimilaritySearchIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestImages.PGVECTOR);

    @Autowired
    private JdSimilarityService similarityService;

    @Test
    void ranksTheMostSimilarIndexedJdFirst() {
        UUID backendId = UUID.randomUUID();
        UUID designId = UUID.randomUUID();
        similarityService.index(backendId,
                "Senior Backend Engineer. Java, Spring Boot, PostgreSQL, REST APIs, distributed systems.",
                new JdRequirements("Senior Backend Engineer", "Senior", List.of()));
        similarityService.index(designId,
                "Lead Product Designer. Figma, user research, design systems, visual prototyping.",
                new JdRequirements("Lead Product Designer", "Lead", List.of()));

        List<SimilarJd> results = similarityService.findSimilar(
                "Backend engineer building REST services in Java and Spring Boot on Postgres.", 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).jdId()).isEqualTo(backendId);
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
    }
}
