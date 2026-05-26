package com.rafa.jdmatch.analysis;

import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /analyses}. Both fields are plain text (v1). */
public record AnalyzeRequest(

        @NotBlank(message = "jdText must not be blank")
        String jdText,

        @NotBlank(message = "resumeText must not be blank")
        String resumeText
) {
}
