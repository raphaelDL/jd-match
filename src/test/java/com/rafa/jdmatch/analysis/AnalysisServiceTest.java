package com.rafa.jdmatch.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafa.jdmatch.claude.ClaudeClient;
import com.rafa.jdmatch.claude.ClaudeProperties;
import com.rafa.jdmatch.jd.ExtractedJd;
import com.rafa.jdmatch.jd.JdRequirements;
import com.rafa.jdmatch.jd.JdService;
import com.rafa.jdmatch.jd.Requirement;
import com.rafa.jdmatch.resume.ResumeService;
import com.rafa.jdmatch.resume.StoredResume;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private final JdService jdService = mock(JdService.class);
    private final ResumeService resumeService = mock(ResumeService.class);
    private final ClaudeClient claude = mock(ClaudeClient.class);
    private final AnalysisRepository repository = mock(AnalysisRepository.class);
    private final AnalysisService service = new AnalysisService(
            jdService, resumeService, claude, repository,
            new ClaudeProperties("test-key", "claude-sonnet-4-6", 2048),
            new ObjectMapper());

    private final UUID jdId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();

    private static GapAnalysis sampleAnalysis() {
        return new GapAnalysis(
                82, "Strong fit",
                List.of(new RequirementAssessment("Java", true, "8 years")),
                List.of("Kubernetes"),
                List.of("Distributed systems"));
    }

    private ExtractedJd extractedJd() {
        return new ExtractedJd(jdId, new JdRequirements(
                "Backend Engineer", "Senior",
                List.of(new Requirement("5+ years Java", "experience", true))));
    }

    private void stubStages() {
        when(jdService.getOrExtract(any())).thenReturn(extractedJd());
        when(resumeService.getOrStore(any())).thenReturn(new StoredResume(resumeId));
    }

    @Test
    void cacheMissRunsBothStagesComparesAndPersists() {
        stubStages();
        when(repository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(claude.extract(eq(AnalysisService.PROMPT_NAME), any(), eq(GapAnalysis.class)))
                .thenReturn(sampleAnalysis());
        when(repository.save(any(JpaAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

        AnalysisOutcome outcome = service.analyze("a job description", "a resume");

        assertThat(outcome.created()).isTrue();
        assertThat(outcome.result().analysis()).isEqualTo(sampleAnalysis());
        verify(jdService).getOrExtract("a job description");
        verify(resumeService).getOrStore("a resume");

        // the compare step receives the structured requirements + the resume text
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(claude).extract(eq(AnalysisService.PROMPT_NAME), content.capture(), eq(GapAnalysis.class));
        assertThat(content.getValue()).contains("5+ years Java").contains("a resume");
        verify(repository).save(any(JpaAnalysis.class));
    }

    @Test
    void cacheHitSkipsCompare() throws Exception {
        stubStages();
        GapAnalysis analysis = sampleAnalysis();
        UUID id = UUID.randomUUID();
        JpaAnalysis stored = new JpaAnalysis(
                id, "the-key", jdId, resumeId,
                new ObjectMapper().writeValueAsString(analysis),
                "claude-sonnet-4-6", "v1", Instant.now());
        when(repository.findByIdempotencyKey(any())).thenReturn(Optional.of(stored));

        AnalysisOutcome outcome = service.analyze("jd", "resume");

        assertThat(outcome.created()).isFalse();
        assertThat(outcome.result().id()).isEqualTo(id);
        assertThat(outcome.result().analysis()).isEqualTo(analysis);
        verify(claude, never()).extract(any(), any(), eq(GapAnalysis.class));
        verify(repository, never()).save(any());
    }

    @Test
    void getReturnsStored() throws Exception {
        GapAnalysis analysis = sampleAnalysis();
        UUID id = UUID.randomUUID();
        JpaAnalysis stored = new JpaAnalysis(
                id, "k", jdId, resumeId,
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

    @Test
    void listMapsStoredAnalysesToSummaries() throws Exception {
        UUID id = UUID.randomUUID();
        JpaAnalysis stored = new JpaAnalysis(
                id, "k", jdId, resumeId,
                new ObjectMapper().writeValueAsString(sampleAnalysis()),
                "claude-sonnet-4-6", "v1", Instant.now());
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stored), PageRequest.of(0, 20), 1));

        var page = service.list(PageRequest.of(0, 20));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).id()).isEqualTo(id);
        assertThat(page.content().get(0).overallFitScore()).isEqualTo(82);
    }
}
