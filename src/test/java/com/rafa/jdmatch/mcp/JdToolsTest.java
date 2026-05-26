package com.rafa.jdmatch.mcp;

import com.rafa.jdmatch.jd.JdSimilarityService;
import com.rafa.jdmatch.jd.SimilarJd;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JdToolsTest {

    private final JdSimilarityService similarityService = mock(JdSimilarityService.class);
    private final JdTools tools = new JdTools(similarityService);

    @Test
    void findSimilarJdsDelegatesWithGivenTopK() {
        UUID jdId = UUID.randomUUID();
        when(similarityService.findSimilar("query", 3))
                .thenReturn(List.of(new SimilarJd(jdId, "Backend Engineer", "Senior", 0.9)));

        List<SimilarJd> result = tools.findSimilarJds("query", 3);

        assertThat(result).hasSize(1);
        verify(similarityService).findSimilar("query", 3);
    }

    @Test
    void findSimilarJdsDefaultsTopKWhenNull() {
        when(similarityService.findSimilar(eq("query"), eq(5))).thenReturn(List.of());

        tools.findSimilarJds("query", null);

        verify(similarityService).findSimilar("query", 5);
    }

    @Test
    void findSimilarJdsRejectsBlankInput() {
        assertThatThrownBy(() -> tools.findSimilarJds("  ", 5))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(similarityService);
    }
}
