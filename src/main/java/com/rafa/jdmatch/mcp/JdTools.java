package com.rafa.jdmatch.mcp;

import com.rafa.jdmatch.jd.ExtractedJd;
import com.rafa.jdmatch.jd.JdService;
import com.rafa.jdmatch.jd.JdSimilarityService;
import com.rafa.jdmatch.jd.SimilarJd;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP tools over the JD corpus. Delegates to the JD services so tool calls use the same
 * extraction (cached) and vector search as the REST API.
 */
@Component
public class JdTools {

    private static final int DEFAULT_TOP_K = 5;

    private final JdService jdService;
    private final JdSimilarityService similarityService;

    public JdTools(JdService jdService, JdSimilarityService similarityService) {
        this.jdService = jdService;
        this.similarityService = similarityService;
    }

    @McpTool(
            name = "extract_jd_requirements",
            description = """
                    Extract structured requirements from a job description: the role title, \
                    seniority, and a list of concrete requirements (each categorized and \
                    flagged required vs nice-to-have). Cached, so re-extracting the same JD \
                    is free. Returns the stored JD id and the requirements.""")
    public ExtractedJd extractJdRequirements(
            @McpToolParam(description = "The full job description text.", required = true)
            String jdText) {

        if (jdText == null || jdText.isBlank()) {
            throw new IllegalArgumentException("jdText must be non-blank.");
        }
        return jdService.getOrExtract(jdText);
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
