package com.rafa.jdmatch.mcp;

import com.rafa.jdmatch.jd.JdSimilarityService;
import com.rafa.jdmatch.jd.SimilarJd;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP tools over the JD corpus. Delegates to {@link JdSimilarityService} so tool calls
 * use the same vector search as the REST endpoint.
 */
@Component
public class JdTools {

    private static final int DEFAULT_TOP_K = 5;

    private final JdSimilarityService similarityService;

    public JdTools(JdSimilarityService similarityService) {
        this.similarityService = similarityService;
    }

    @McpTool(
            name = "find_similar_jds",
            description = """
                    Find previously analyzed job descriptions most similar to the given JD \
                    text, ranked by embedding similarity. Returns each match's JD id, role \
                    title, seniority, and similarity score (higher is more similar).""")
    public List<SimilarJd> findSimilarJds(
            @McpToolParam(description = "The job description text to find similar past JDs for.", required = true)
            String jdText,
            @McpToolParam(description = "How many matches to return (defaults to 5).", required = false)
            Integer topK) {

        if (jdText == null || jdText.isBlank()) {
            throw new IllegalArgumentException("jdText must be non-blank.");
        }
        return similarityService.findSimilar(jdText, topK == null ? DEFAULT_TOP_K : topK);
    }
}
