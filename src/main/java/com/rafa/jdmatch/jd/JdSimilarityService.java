package com.rafa.jdmatch.jd;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 3 (RAG foundation): indexes extracted JDs into the vector store and finds
 * similar past JDs by embedding similarity. The {@link VectorStore} embeds text using
 * the configured local model (all-MiniLM-L6-v2) — no external call — and searches by
 * cosine distance over the pgvector index.
 */
@Service
public class JdSimilarityService {

    static final String META_JD_ID = "jdId";
    static final String META_ROLE_TITLE = "roleTitle";
    static final String META_SENIORITY = "seniority";

    private static final int DEFAULT_TOP_K = 5;

    private final VectorStore vectorStore;

    public JdSimilarityService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Index a freshly extracted JD. The JD id doubles as the document id, so re-indexing
     * the same JD replaces rather than duplicates it.
     */
    public void index(UUID jdId, String jdText, JdRequirements requirements) {
        Document document = new Document(
                jdId.toString(),
                jdText,
                Map.of(
                        META_JD_ID, jdId.toString(),
                        META_ROLE_TITLE, nullSafe(requirements.roleTitle()),
                        META_SENIORITY, nullSafe(requirements.seniority())));
        vectorStore.add(List.of(document));
    }

    /** Find the {@code topK} previously-indexed JDs most similar to {@code jdText}. */
    public List<SimilarJd> findSimilar(String jdText, int topK) {
        int limit = topK > 0 ? topK : DEFAULT_TOP_K;
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder().query(jdText).topK(limit).build());
        if (matches == null) {
            return List.of();
        }
        return matches.stream().map(JdSimilarityService::toSimilarJd).toList();
    }

    private static SimilarJd toSimilarJd(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        return new SimilarJd(
                UUID.fromString((String) meta.get(META_JD_ID)),
                (String) meta.get(META_ROLE_TITLE),
                (String) meta.get(META_SENIORITY),
                doc.getScore());
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
