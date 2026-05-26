package com.rafa.jdmatch.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The single gateway to the Anthropic API — every Claude call in the service goes
 * through here. Given a prompt (loaded by name) used as the system prompt and some
 * user content, it asks Claude for a response shaped exactly like {@code responseType}
 * (a Java record) via the SDK's structured-output support, and returns the parsed,
 * schema-validated value.
 */
@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private final AnthropicClient client;
    private final PromptLoader promptLoader;
    private final ClaudeProperties properties;

    public ClaudeClient(AnthropicClient client, PromptLoader promptLoader, ClaudeProperties properties) {
        this.client = client;
        this.promptLoader = promptLoader;
        this.properties = properties;
    }

    public <T> T extract(String promptName, String userContent, Class<T> responseType) {
        String systemPrompt = promptLoader.load(promptName);

        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(properties.maxTokens())
                .system(systemPrompt)
                .addUserMessage(userContent)
                .outputConfig(responseType)
                .build();

        long startNanos = System.nanoTime();
        StructuredMessage<T> response = client.messages().create(params);
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

        T value = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new ClaudeException(
                        "Claude returned no structured content for prompt '" + promptName + "'"));

        Usage usage = response.usage();
        log.info("Claude call prompt={} model={} latencyMs={} inputTokens={} outputTokens={}",
                promptName, properties.model(), latencyMs, usage.inputTokens(), usage.outputTokens());

        return value;
    }
}
