package com.rafa.jdmatch.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafa.jdmatch.claude.ClaudeClient;
import com.rafa.jdmatch.claude.ClaudeProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private final ClaudeClient claude = mock(ClaudeClient.class);
    private final AnalysisRepository repository = mock(AnalysisRepository.class);
    private final AnalysisService service = new AnalysisService(
            claude, repository,
            new ClaudeProperties("test-key", "claude-sonnet-4-6", 2048),
            new ObjectMapper());

    private static GapAnalysis sampleAnalysis() {
        return new GapAnalysis(
                82, "Strong fit",
                List.of(new RequirementAssessment("Java", true, "8 years")),
                List.of("Kubernetes"),
                List.of("Distributed systems"));
    }

    @Test
    void cacheMissCallsClaudeAndPersists() {
        when(repository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(claude.extract(eq(AnalysisService.PROMPT_NAME), any(), eq(GapAnalysis.class)))
                .thenReturn(sampleAnalysis());
        when(repository.save(any(JpaAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

        AnalysisResult result = service.analyze("a job description", "a resume");

        assertThat(result.analysis()).isEqualTo(sampleAnalysis());
        assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
        assertThat(result.id()).isNotNull();
        verify(claude).extract(eq(AnalysisService.PROMPT_NAME), any(), eq(GapAnalysis.class));
        verify(repository).save(any(JpaAnalysis.class));
    }

    @Test
    void cacheHitReturnsStoredWithoutCallingClaude() throws Exception {
        GapAnalysis analysis = sampleAnalysis();
        UUID id = UUID.randomUUID();
        JpaAnalysis stored = new JpaAnalysis(
                id, "the-key", "jd", "resume",
                new ObjectMapper().writeValueAsString(analysis),
                "claude-sonnet-4-6", "v1", Instant.now());
        when(repository.findByIdempotencyKey(any())).thenReturn(Optional.of(stored));

        AnalysisResult result = service.analyze("jd", "resume");

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.analysis()).isEqualTo(analysis);
        verifyNoInteractions(claude);
        verify(repository, never()).save(any());
    }

    @Test
    void idempotencyKeyIsWhitespaceInsensitive() {
        when(repository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(claude.extract(any(), any(), eq(GapAnalysis.class))).thenReturn(sampleAnalysis());
        when(repository.save(any(JpaAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

        service.analyze("Senior   Java\n\nEngineer", "  My   resume  ");
        service.analyze("Senior Java Engineer", "My resume");

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(2)).findByIdempotencyKey(keys.capture());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
    }

    @Test
    void getReturnsStored() throws Exception {
        GapAnalysis analysis = sampleAnalysis();
        UUID id = UUID.randomUUID();
        JpaAnalysis stored = new JpaAnalysis(
                id, "k", "jd", "resume",
                new ObjectMapper().writeValueAsString(analysis),
                "claude-sonnet-4-6", "v1", Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(stored));

        assertThat(service.get(id).analysis()).isEqualTo(analysis);
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(AnalysisNotFoundException.class);
    }
}
