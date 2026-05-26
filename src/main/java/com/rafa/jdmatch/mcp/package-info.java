/**
 * MCP server surface: exposes jd-match's analysis pipeline as tools for MCP clients
 * (Claude Code, Claude Desktop) over Streamable HTTP, served from the same Spring Boot
 * app as the REST API. Tools delegate to the service layer and reuse its caching and
 * idempotency; the Anthropic SDK is never called directly from here.
 */
package com.rafa.jdmatch.mcp;
