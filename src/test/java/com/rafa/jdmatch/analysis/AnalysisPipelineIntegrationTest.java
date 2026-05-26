package com.rafa.jdmatch.analysis;

import com.rafa.jdmatch.TestImages;
import com.rafa.jdmatch.claude.ClaudeClient;
import com.rafa.jdmatch.jd.JdRequirements;
import com.rafa.jdmatch.jd.Requirement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end persistence check against a real Postgres container: Flyway runs the
 * migrations, Hibernate validates the schema on startup, and an analysis is written
 * and read back through the jsonb columns. The Claude gateway is mocked, so the test
 * needs no API key or network — it exercises the database layer, not the model call.
 */
@SpringBootTest(properties = "anthropic.api-key=test-key")
@Testcontainers
class AnalysisPipelineIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestImages.PGVECTOR);

    @MockitoBean
    private ClaudeClient claudeClient;

    @Autowired
    private AnalysisService analysisService;

    @Test
    void persistsAndRoundTripsAnalysisThroughPostgres() {
        JdRequirements requirements = new JdRequirements(
                "Backend Engineer", "Senior",
                List.of(new Requirement("5+ years Java", "experience", true)));
        GapAnalysis analysis = new GapAnalysis(
                88, "Strong fit",
                List.of(new RequirementAssessment("5+ years Java", true, "8 years at Acme")),
                List.of(), List.of("Java", "Distributed systems"));
        when(claudeClient.extract(any(), any(), eq(JdRequirements.class))).thenReturn(requirements);
        when(claudeClient.extract(any(), any(), eq(GapAnalysis.class))).thenReturn(analysis);

        AnalysisOutcome first = analysisService.analyze(
                "Senior backend role, 5+ years Java", "8 years of Java at Acme");

        assertThat(first.created()).isTrue();

        // read it back in a separate transaction — proves the jsonb round-trip, not a cache
        AnalysisResult fetched = analysisService.get(first.result().id());
        assertThat(fetched.analysis()).isEqualTo(analysis);
        assertThat(fetched.model()).isEqualTo("claude-sonnet-4-6");

        // identical inputs are served from the idempotency cache: same id, Claude not called again
        AnalysisOutcome second = analysisService.analyze(
                "Senior backend role, 5+ years Java", "8 years of Java at Acme");

        assertThat(second.created()).isFalse();
        assertThat(second.result().id()).isEqualTo(first.result().id());
        verify(claudeClient, times(1)).extract(any(), any(), eq(GapAnalysis.class));
    }
}
