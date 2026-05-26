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

- [x] **Phase 1** — Core REST API, JD requirement extraction, resume storage, gap analysis pipeline
- [x] **Phase 2** — MCP server wrapper so Claude Code and Claude Desktop can use jd-match as a tool
- [x] **Phase 3** — RAG layer over a corpus of past JDs for pattern recognition across similar roles *(foundation: embedding + similarity search)*
- [x] **Phase 4** — AWS deployment and a write-up of the design decisions and tradeoffs

## Running locally

```bash
# Postgres
docker-compose up -d

# Service
./mvnw spring-boot:run

# Tests (a Postgres integration test runs via Testcontainers, so Docker must be running)
./mvnw test
```

Set your `ANTHROPIC_API_KEY` in `.env` before running the service (see `.env.example`); it is loaded automatically at startup.

## Use as an MCP server

The running service also speaks the Model Context Protocol over Streamable HTTP at
`/mcp`, so MCP clients can call it as a tool. Start the service, then register it:

```bash
# Claude Code
claude mcp add --transport http jd-match http://localhost:8080/mcp
```

For **Claude Desktop**, add a custom connector pointing at `http://localhost:8080/mcp`
(Settings → Connectors → Add custom connector).

### Tools

- **`analyze_fit(jdText, resumeText)`** — returns a structured gap analysis (overall
  fit score, per-requirement assessments with evidence, strengths, and gaps). Backed by
  the same pipeline as `POST /analyses`, so results are cached and idempotent.
- **`extract_jd_requirements(jdText)`** — returns the structured requirements for a JD
  (role title, seniority, categorized requirements). Cached per JD.
- **`get_analysis(analysisId)`** — fetches a previously created analysis by id.
- **`find_similar_jds(jdText, topK?)`** — returns previously analyzed JDs most similar to
  the given text, ranked by embedding similarity. Same search as `POST /jds/similar`.

## Similarity search

Every JD extracted during an analysis is embedded (locally, via all-MiniLM-L6-v2 — no
API key) and stored in Postgres with `pgvector`. `POST /jds/similar {jdText, topK?}`
(and the `find_similar_jds` MCP tool) returns the closest past JDs by cosine distance.
This needs the `pgvector` extension, so local Postgres runs the `pgvector/pgvector:pg16`
image (see `docker-compose.yml`); AWS RDS supports it as an extension.

## Deploying to AWS

A container image and Terraform for **AWS App Runner + RDS Postgres (pgvector)** live under
[`deploy/terraform/`](deploy/terraform/README.md). Build it with `docker build .` — the
image bundles the embedding model and native libs so it starts without downloading anything
(`ai.djl.offline=true`). App Runner health-checks `/actuator/health/readiness`.

## Design notes

A few decisions worth calling out (full write-up in [`DESIGN.md`](DESIGN.md)):

- **Structured outputs over free-form generation.** Every call to Claude returns JSON validated against a Java record. This makes the AI a reliable component of a larger system rather than a chatty wrapper.
- **Prompts as resources, not strings.** Prompts live in `src/main/resources/prompts/` so they can be versioned, diff-reviewed, and swapped without touching code.
- **Postgres from day one.** Sets up for vector search in Phase 3 without a migration.
- **Idempotent analyses.** Re-running the same JD and resume returns the cached analysis. Cheap and predictable, and a useful guardrail when iterating on prompts.

## License

MIT
