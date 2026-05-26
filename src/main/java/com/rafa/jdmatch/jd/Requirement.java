package com.rafa.jdmatch.jd;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** A single requirement extracted from a job description. */
@JsonClassDescription("A single concrete requirement from a job description.")
public record Requirement(

        @JsonPropertyDescription("The requirement, quoted or tightly paraphrased from the job description.")
        String text,

        @JsonPropertyDescription("Category of the requirement: skill, experience, or education.")
        String category,

        @JsonPropertyDescription("True if the requirement is mandatory; false if merely nice-to-have.")
        boolean required
) {
}
