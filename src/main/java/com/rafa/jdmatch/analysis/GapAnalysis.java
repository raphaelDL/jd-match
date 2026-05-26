package com.rafa.jdmatch.analysis;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Structured result of comparing a resume against a job description. This is the
 * record Claude's response is validated against (see
 * {@link com.rafa.jdmatch.claude.ClaudeClient}).
 */
@JsonClassDescription("Structured gap analysis comparing a candidate resume against a job description.")
public record GapAnalysis(

        @JsonPropertyDescription("Overall fit from 0 (no match) to 100 (excellent match).")
        int overallFitScore,

        @JsonPropertyDescription("A brief, honest summary of how well the candidate fits the role.")
        String fitSummary,

        @JsonPropertyDescription("Assessment of each concrete requirement found in the job description.")
        List<RequirementAssessment> requirements,

        @JsonPropertyDescription("Requirements with weak or no supporting evidence in the resume.")
        List<String> gaps,

        @JsonPropertyDescription("The candidate's standout strengths relative to this role.")
        List<String> strengths
) {
}
