package com.rafa.jdmatch.jd;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdSimilarityServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final JdSimilarityService service = new JdSimilarityService(vectorStore);

    @Test
    void indexAddsDocumentKeyedByJdIdWithRoleMetadata() {
        UUID jdId = UUID.randomUUID();
        JdRequirements requirements = new JdRequirements("Senior Backend Engineer", "Senior", List.of());

        service.index(jdId, "the JD text", requirements);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> docs = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(docs.capture());
        Document doc = docs.getValue().get(0);
        assertThat(doc.getId()).isEqualTo(jdId.toString());
        assertThat(doc.getText()).isEqualTo("the JD text");
        assertThat(doc.getMetadata())
                .containsEntry("jdId", jdId.toString())
                .containsEntry("roleTitle", "Senior Backend Engineer")
                .containsEntry("seniority", "Senior");
    }

    @Test
    void indexToleratesNullRoleFields() {
        UUID jdId = UUID.randomUUID();

        service.index(jdId, "text", new JdRequirements(null, null, List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> docs = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(docs.capture());
        assertThat(docs.getValue().get(0).getMetadata())
                .containsEntry("roleTitle", "")
                .containsEntry("seniority", "");
    }

    @Test
    void findSimilarMapsResultsAndRequestsTopK() {
        UUID jdId = UUID.randomUUID();
        Document match = Document.builder()
                .id(jdId.toString())
                .text("the JD text")
                .metadata(Map.of("jdId", jdId.toString(), "roleTitle", "Backend Engineer", "seniority", "Mid"))
                .score(0.87)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(match));

        List<SimilarJd> results = service.findSimilar("query text", 3);

        assertThat(results).hasSize(1);
        SimilarJd similar = results.get(0);
        assertThat(similar.jdId()).isEqualTo(jdId);
        assertThat(similar.roleTitle()).isEqualTo("Backend Engineer");
        assertThat(similar.seniority()).isEqualTo("Mid");
        assertThat(similar.score()).isEqualTo(0.87);

        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertThat(request.getValue().getTopK()).isEqualTo(3);
        assertThat(request.getValue().getQuery()).isEqualTo("query text");
    }
}
