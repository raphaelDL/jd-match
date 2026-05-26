package com.rafa.jdmatch.common;

import com.rafa.jdmatch.analysis.AnalysisNotFoundException;
import com.rafa.jdmatch.claude.ClaudeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to HTTP responses. Bean-validation failures (400) are handled
 * by Spring's defaults; this covers the not-found and provider-error cases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ProblemDetail handleNotFound(AnalysisNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ClaudeException.class)
    public ProblemDetail handleClaude(ClaudeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Analysis provider error: " + e.getMessage());
    }
}
