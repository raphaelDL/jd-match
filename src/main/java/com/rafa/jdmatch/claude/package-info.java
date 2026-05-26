/**
 * The single Anthropic API client and prompt loading. Every Claude call funnels
 * through here; no HTTP calls live in feature services. Prompts are loaded by
 * name from {@code src/main/resources/prompts/}.
 * Skeleton only — no logic yet.
 */
package com.rafa.jdmatch.claude;
