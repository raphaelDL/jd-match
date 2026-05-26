package com.rafa.jdmatch.jd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafa.jdmatch.claude.ClaudeClient;
import com.rafa.jdmatch.claude.ClaudeProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JdServiceTest {

    private final ClaudeClient claude = mock(ClaudeClient.class);
    private final JdRepository repository = mock(JdRepository.class);
    private final JdSimilarityService similarityService = mock(JdSimilarityService.class);
    private final JdService service = new JdService(
            claude, repository,
            new ClaudeProperties("test-key", "claude-sonnet-4-6", 2048),
            new ObjectMapper(), similarityService);

    private static JdRequirements sample() {
        return new JdRequirements("Senior Backend Engineer", "Senior",
                List.of(new Requirement("5+ years Java", "experience", true),
                        new Requirement("Kafka", "skill", false)));
    }

    @Test
    void missExtractsViaClaudeAndPersists() {
        when(repository.findByJdHash(any())).thenReturn(Optional.empty());
        when(claude.extract(eq(JdService.PROMPT_NAME), any(), eq(JdRequirements.class)))
                .thenReturn(sample());
        when(repository.save(any(Jd.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractedJd result = service.getOrExtract("a job description");

        assertThat(result.requirements()).isEqualTo(sample());
        assertThat(result.id()).isNotNull();
        verify(claude).extract(eq(JdService.PROMPT_NAME), any(), eq(JdRequirements.class));
        verify(repository).save(any(Jd.class));
        verify(similarityService).index(eq(result.id()), eq("a job description"), eq(sample()));
    }

    @Test
    void hitReturnsStoredWithoutCallingClaude() throws Exception {
        JdRequirements requirements = sample();
        UUID id = UUID.randomUUID();
        Jd stored = new Jd(id, "hash", "jd text",
                new ObjectMapper().writeValueAsString(requirements),
                "claude-sonnet-4-6", "v1", Instant.now());
        when(repository.findByJdHash(any())).thenReturn(Optional.of(stored));

        ExtractedJd result = service.getOrExtract("a job description");

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.requirements()).isEqualTo(requirements);
        verifyNoInteractions(claude);
        verifyNoInteractions(similarityService);
        verify(repository, never()).save(any());
    }

    @Test
    void sameJdHashesEquallyRegardlessOfWhitespace() {
        when(repository.findByJdHash(any())).thenReturn(Optional.empty());
        when(claude.extract(any(), any(), eq(JdRequirements.class))).thenReturn(sample());
        when(repository.save(any(Jd.class))).thenAnswer(inv -> inv.getArgument(0));

        service.getOrExtract("Senior   Java\n\nEngineer");
        service.getOrExtract("Senior Java Engineer");

        org.mockito.ArgumentCaptor<String> hashes = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(2)).findByJdHash(hashes.capture());
        assertThat(hashes.getAllValues().get(0)).isEqualTo(hashes.getAllValues().get(1));
    }
}
