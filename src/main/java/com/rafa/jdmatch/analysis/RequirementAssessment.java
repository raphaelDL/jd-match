package com.rafa.jdmatch.analysis;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Assessment of a single job requirement against the resume. */
@JsonClassDescription("Assessment of a single job requirement against the resume.")
public record RequirementAssessment(

        @JsonPropertyDescription("The requirement, quoted or paraphrased from the job description.")
        String requirement,

        @JsonPropertyDescription("True if the resume provides clear evidence the requirement is met.")
        boolean met,

        @JsonPropertyDescription("Specific evidence from the resume, or why the requirement is unmet.")
        String evidence
) {
}
