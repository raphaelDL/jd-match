package com.rafa.jdmatch.jd;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JdController.class)
class JdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JdSimilarityService similarityService;

    @MockitoBean
    private JdService jdService;

    @Test
    void postReindexReturnsCount() throws Exception {
        when(jdService.reindexAll()).thenReturn(3);

        mockMvc.perform(post("/jds/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(3));
    }

    @Test
    void postSimilarReturnsMatches() throws Exception {
        UUID jdId = UUID.randomUUID();
        when(similarityService.findSimilar(eq("a job description"), eq(2)))
                .thenReturn(List.of(new SimilarJd(jdId, "Backend Engineer", "Senior", 0.91)));

        String body = objectMapper.writeValueAsString(new SimilarJdRequest("a job description", 2));

        mockMvc.perform(post("/jds/similar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jdId").value(jdId.toString()))
                .andExpect(jsonPath("$[0].roleTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$[0].score").value(0.91));
    }

    @Test
    void postSimilarDefaultsTopKWhenAbsent() throws Exception {
        when(similarityService.findSimilar(eq("a job description"), eq(5))).thenReturn(List.of());

        String body = objectMapper.writeValueAsString(new SimilarJdRequest("a job description", null));

        mockMvc.perform(post("/jds/similar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void postSimilarRejectsBlankJdText() throws Exception {
        String body = objectMapper.writeValueAsString(new SimilarJdRequest("  ", null));

        mockMvc.perform(post("/jds/similar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
