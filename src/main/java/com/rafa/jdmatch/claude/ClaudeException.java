package com.rafa.jdmatch.claude;

/** Raised when a Claude call or its supporting resources (e.g. prompts) fail. */
public class ClaudeException extends RuntimeException {

    public ClaudeException(String message) {
        super(message);
    }

    public ClaudeException(String message, Throwable cause) {
        super(message, cause);
    }
}
