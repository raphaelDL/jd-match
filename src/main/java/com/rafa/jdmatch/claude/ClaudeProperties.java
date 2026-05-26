package com.rafa.jdmatch.claude;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anthropic configuration, bound from {@code anthropic.*}. {@code apiKey} comes from
 * the environment (see {@code .env}); {@code model} and {@code maxTokens} have sane
 * defaults so only the key is strictly required.
 */
@ConfigurationProperties(prefix = "anthropic")
public record ClaudeProperties(String apiKey, String model, int maxTokens) {

    public ClaudeProperties {
        if (model == null || model.isBlank()) {
            model = "claude-sonnet-4-6";
        }
        if (maxTokens <= 0) {
            maxTokens = 2048;
        }
    }
}
