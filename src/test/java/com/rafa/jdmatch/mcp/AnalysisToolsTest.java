package com.rafa.jdmatch.mcp;

import com.rafa.jdmatch.analysis.AnalysisOutcome;
import com.rafa.jdmatch.analysis.AnalysisResult;
import com.rafa.jdmatch.analysis.AnalysisService;
import com.rafa.jdmatch.analysis.GapAnalysis;
import com.rafa.jdmatch.analysis.RequirementAssessment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for the MCP tool. The service is mocked, so this verifies the tool's own
 * behaviour — delegation and the non-blank guard — without the MCP transport.
 */
class AnalysisToolsTest {

    private final AnalysisService analysisService = mock(AnalysisService.class);
    private final AnalysisTools tools = new AnalysisTools(analysisService);

    @Test
    void analyzeFitDelegatesToServiceAndReturnsTheResult() {
        AnalysisResult result = new AnalysisResult(
                UUID.randomUUID(),
                new GapAnalysis(82, "Strong fit",
                        List.of(new RequirementAssessment("Java", true, "8 years")),
                        List.of("Kubernetes"), List.of("Distributed systems")),
                "claude-sonnet-4-6", Instant.now());
        when(analysisService.analyze("a job description", "a resume"))
                .thenReturn(new AnalysisOutcome(result, true));

        AnalysisResult returned = tools.analyzeFit("a job description", "a resume");

        assertThat(returned).isEqualTo(result);
        verify(analysisService).analyze("a job description", "a resume");
    }

    @Test
    void analyzeFitRejectsBlankInputWithoutCallingTheService() {
        assertThatThrownBy(() -> tools.analyzeFit("   ", "a resume"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tools.analyzeFit("a job description", ""))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(analysisService);
    }
}
