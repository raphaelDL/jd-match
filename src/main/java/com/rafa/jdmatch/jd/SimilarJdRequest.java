package com.rafa.jdmatch.jd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for similarity search. {@code topK} is optional (defaults to 5 when
 * absent); when present it must be positive.
 */
public record SimilarJdRequest(
        @NotBlank String jdText,
        @Positive Integer topK) {
}
