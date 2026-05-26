package com.rafa.jdmatch.mcp;

import com.rafa.jdmatch.analysis.AnalysisResult;
import com.rafa.jdmatch.analysis.AnalysisService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * jd-match's MCP tools. Each method is exposed to MCP clients (e.g. Claude Code,
 * Claude Desktop) and delegates to the existing service layer, so a tool call reuses
 * the same two-stage pipeline, caching, and idempotency as the REST API.
 */
@Component
public class AnalysisTools {

    private final AnalysisService analysisService;

    public AnalysisTools(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @McpTool(
            name = "analyze_fit",
            description = """
                    Analyze how well a resume matches a job description. Returns a structured \
                    gap analysis: an overall fit score (0-100), a fit summary, per-requirement \
                    assessments (met/not-met with evidence), candidate strengths, and gaps.""")
    public AnalysisResult analyzeFit(
            @McpToolParam(description = "The full job description text.", required = true)
            String jdText,
            @McpToolParam(description = "The full resume text.", required = true)
            String resumeText) {

        if (jdText == null || jdText.isBlank() || resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("Both jdText and resumeText must be non-blank.");
        }
        // The REST layer enforces non-blank via @Valid; the tool path guards it here.
        return analysisService.analyze(jdText, resumeText).result();
    }

    @McpTool(
            name = "get_analysis",
            description = """
                    Fetch a previously created gap analysis by its id. Returns the stored \
                    analysis, the model that produced it, and when it was created.""")
    public AnalysisResult getAnalysis(
            @McpToolParam(description = "The analysis id (a UUID) returned when it was created.", required = true)
            String analysisId) {

        UUID id;
        try {
            id = UUID.fromString(analysisId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("analysisId must be a valid UUID: " + analysisId);
        }
        return analysisService.get(id);
    }
}
