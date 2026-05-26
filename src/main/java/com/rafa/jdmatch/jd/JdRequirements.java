package com.rafa.jdmatch.jd;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Structured requirements extracted from a job description — stage one of the
 * pipeline. Validated against this record via {@link com.rafa.jdmatch.claude.ClaudeClient}.
 */
@JsonClassDescription("Structured requirements extracted from a job description.")
public record JdRequirements(

        @JsonPropertyDescription("The role title, e.g. 'Senior Backend Engineer'.")
        String roleTitle,

        @JsonPropertyDescription("Seniority level if stated or clearly implied, e.g. 'Senior', 'Mid', 'Staff'.")
        String seniority,

        @JsonPropertyDescription("The concrete requirements found in the job description.")
        List<Requirement> requirements
) {
}
