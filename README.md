# jd-match

A Spring Boot 3 / Java 21 backend service that uses the Anthropic API to extract structured requirements from job descriptions and produce a gap analysis against a resume.

**Status:** In active development, May 2026.

## Why this exists

Job descriptions are dense, inconsistent, and full of buried signals — some "requirements" are real, others are wishlist, and the distinction matters when deciding whether to apply, how to tailor a resume, or how to prepare for an interview. Doing this comparison manually for every JD is slow and not consistent across attempts.

jd-match treats this as a structured-extraction problem: given a JD and a resume, get back a typed, comparable representation of what the role wants and where the candidate matches or doesn't. The Anthropic API does the reading; the service does the orchestration, persistence, and consistency.

It's also a focused exploration of production patterns for AI-assisted backend services — structured outputs over free text, prompt versioning, observability around model calls, and idempotent caching of expensive generations.

## Architecture

```
┌────────────────────────────────────────────┐
│  REST API (Spring Boot 3, Java 21)         │
│   POST /analyses   GET /analyses/{id}      │
└─────────────────────┬──────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   Resume Service  JD Service  Analysis Service
                                    │
                                    ▼
                          Anthropic API
                          (structured outputs)
                                    │
                                    ▼
                          Postgres (pgvector-ready)
```

## Tech stack

Java 21 · Spring Boot 3 · Postgres · Docker · Anthropic Java SDK · AWS

## Roadmap

- [ ] **Phase 1** — Core REST API, JD requirement extraction, resume storage, gap analysis pipeline
- [ ] **Phase 2** — MCP server wrapper so Claude Code and Claude Desktop can use jd-match as a tool
- [ ] **Phase 3** — RAG layer over a corpus of past JDs for pattern recognition across similar roles
- [ ] **Phase 4** — AWS deployment and a write-up of the design decisions and tradeoffs

## Running locally

```bash
# Postgres
docker-compose up -d

# Service
./mvnw spring-boot:run

# Tests
./mvnw test
```

Set your `ANTHROPIC_API_KEY` in `.env` before running the service. See `.env.example`.

## Design notes

A few decisions worth calling out:

- **Structured outputs over free-form generation.** Every call to Claude returns JSON validated against a Java record. This makes the AI a reliable component of a larger system rather than a chatty wrapper.
- **Prompts as resources, not strings.** Prompts live in `src/main/resources/prompts/` so they can be versioned, diff-reviewed, and swapped without touching code.
- **Postgres from day one.** Sets up for vector search in Phase 3 without a migration.
- **Idempotent analyses.** Re-running the same JD and resume returns the cached analysis. Cheap and predictable, and a useful guardrail when iterating on prompts.

## License

MIT
