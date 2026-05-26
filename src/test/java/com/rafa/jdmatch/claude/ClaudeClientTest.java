package com.rafa.jdmatch.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.StructuredContentBlock;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.services.blocking.MessageService;
import com.rafa.jdmatch.analysis.GapAnalysis;
import com.rafa.jdmatch.analysis.RequirementAssessment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for the gateway: the SDK client is mocked, so this runs with no network
 * call and no API key. The real {@link PromptLoader} loads compare-requirements.txt from the
 * classpath.
 */
class ClaudeClientTest {

    private final AnthropicClient anthropic = mock(AnthropicClient.class);
    private final MessageService messages = mock(MessageService.class);
    private final ClaudeClient client = new ClaudeClient(
            anthropic, new PromptLoader(),
            new ClaudeProperties("test-key", "claude-sonnet-4-6", 2048));

    @Test
    void extractsTypedRecordFromStructuredResponse() {
        GapAnalysis expected = new GapAnalysis(
                82, "Strong fit",
                List.of(new RequirementAssessment("Java", true, "8 years")),
                List.of("Kubernetes"),
                List.of("Distributed systems"));
        stubResponseReturning(expected);

        GapAnalysis result = client.extract("compare-requirements", "JD + resume", GapAnalysis.class);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void throwsWhenNoStructuredContent() {
        stubEmptyResponse();

        assertThatThrownBy(() -> client.extract("compare-requirements", "x", GapAnalysis.class))
                .isInstanceOf(ClaudeException.class)
                .hasMessageContaining("no structured content");
    }

    @SuppressWarnings("unchecked")
    private void stubResponseReturning(GapAnalysis value) {
        StructuredTextBlock<GapAnalysis> textBlock = mock(StructuredTextBlock.class);
        when(textBlock.text()).thenReturn(value);
        StructuredContentBlock<GapAnalysis> block = mock(StructuredContentBlock.class);
        when(block.text()).thenReturn(Optional.of(textBlock));
        StructuredMessage<GapAnalysis> message = mock(StructuredMessage.class);
        when(message.content()).thenReturn(List.of(block));

        when(anthropic.messages()).thenReturn(messages);
        when(messages.create(any(StructuredMessageCreateParams.class))).thenReturn(message);
    }

    @SuppressWarnings("unchecked")
    private void stubEmptyResponse() {
        StructuredMessage<GapAnalysis> message = mock(StructuredMessage.class);
        when(message.content()).thenReturn(List.of());
        when(anthropic.messages()).thenReturn(messages);
        when(messages.create(any(StructuredMessageCreateParams.class))).thenReturn(message);
    }
}
