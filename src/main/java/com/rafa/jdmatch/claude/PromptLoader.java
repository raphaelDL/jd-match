package com.rafa.jdmatch.claude;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads a prompt by name from {@code src/main/resources/prompts/<name>.txt}.
 * Keeping prompts as resources lets them be versioned and diff-reviewed without
 * touching code.
 */
@Component
public class PromptLoader {

    public String load(String name) {
        ClassPathResource resource = new ClassPathResource("prompts/" + name + ".txt");
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ClaudeException("Could not load prompt '" + name + "'", e);
        }
    }
}
