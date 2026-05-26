package com.rafa.jdmatch.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnalysisService service;

    private static AnalysisResult sampleResult(UUID id) {
        return new AnalysisResult(
                id,
                new GapAnalysis(82, "Strong fit",
                        List.of(new RequirementAssessment("Java", true, "8 years")),
                        List.of("Kubernetes"), List.of("Distributed systems")),
                "claude-sonnet-4-6", Instant.parse("2026-05-25T12:00:00Z"));
    }

    @Test
    void postCreatesAnalysisAndReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.analyze(any(), any())).thenReturn(sampleResult(id));

        String body = objectMapper.writeValueAsString(
                new AnalyzeRequest("a job description", "a resume"));

        mockMvc.perform(post("/analyses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/analyses/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.analysis.overallFitScore").value(82))
                .andExpect(jsonPath("$.model").value("claude-sonnet-4-6"));
    }

    @Test
    void postWithBlankFieldsReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(new AnalyzeRequest("", "  "));

        mockMvc.perform(post("/analyses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsAnalysis() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(sampleResult(id));

        mockMvc.perform(get("/analyses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.analysis.requirements[0].requirement").value("Java"));
    }

    @Test
    void getMissingReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new AnalysisNotFoundException(id));

        mockMvc.perform(get("/analyses/{id}", id))
                .andExpect(status().isNotFound());
    }
}
