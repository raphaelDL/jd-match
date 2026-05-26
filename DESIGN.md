# jd-match — design decisions and tradeoffs

jd-match takes a job description and a resume and returns a structured gap analysis. It's
a small service, but it's built as a deliberate exploration of production patterns for
AI-assisted backends: treating the model as a reliable component rather than a chatty
wrapper. This document records the decisions that shaped it and the tradeoffs behind them.

## Structured outputs over free text

Every Claude call returns JSON validated against a Java record (`GapAnalysis`,
`JdRequirements`), using the Anthropic SDK's structured-output support. The schema is
derived from the record's components, so the model's output is type-checked before the
service ever touches it. This is the central bet: the AI becomes a typed function, and the
rest of the system (persistence, caching, REST/MCP surfaces) is ordinary, testable code.

A single `ClaudeClient` is the only place that talks to the model — prompts in, records
out — which keeps the model's surface area small and easy to log and reason about.

## Prompts as versioned resources

Prompts live in `src/main/resources/prompts/*.txt`, loaded by name, never inlined as Java
strings. They can be diffed and reviewed like code, and a `PROMPT_VERSION` constant ties
each prompt to the cache (below), so changing a prompt naturally invalidates stale results
instead of silently serving them.

## Idempotency by content hash

Re-analyzing the same JD + resume returns the cached result rather than paying for another
model call. The key is a SHA-256 of the normalized inputs **plus the prompt version**, so
identical requests are cheap and deterministic, but a prompt change produces fresh output.
The HTTP layer reflects this honestly: `POST /analyses` returns `201` for a freshly
produced analysis and `200` when it served a cached one.

## Two-stage pipeline

Extraction and comparison are separate stages. A JD's requirements are extracted **once**
(cached by JD hash); comparing that JD against many resumes reuses the extraction. This
matters because JD extraction is the expensive, reusable artifact — a candidate iterating
on their resume against one role shouldn't re-extract the JD each time. The two stages also
made the MCP surface and similarity search natural to add later, since the JD corpus
already existed as a first-class thing.

## Persistence: Postgres, jsonb, Flyway

Structured results are stored as `jsonb`, so the typed shape survives round-trips without a
rigid relational mapping of every nested field. **Flyway owns the schema** and Hibernate
runs with `ddl-auto=validate` — the app never mutates schema implicitly. This discipline
paid off in Phase 3: Spring AI's `PgVectorStore` can auto-create its table, but instead a
Flyway migration creates it and the store runs with `initialize-schema=false`, keeping a
single source of truth for the schema.

## MCP: Spring AI starter, Streamable HTTP

The MCP server uses the Spring AI MCP starter (annotation-based `@McpTool`, auto JSON
schema) over the **Streamable HTTP** transport, served from the same WebMVC app as the REST
API. Streamable HTTP (rather than stdio) was chosen because jd-match is an always-on
service: one process serves both REST and MCP, and both Claude Code and Claude Desktop can
reach it remotely. Tools delegate to the existing services, so an MCP call reuses the same
pipeline, caching, and idempotency as the REST endpoints.

## Embeddings and RAG: local model, pgvector

Similarity search embeds each extracted JD and finds similar past roles by cosine distance
over `pgvector`. The embedding model is **local** (all-MiniLM-L6-v2 via Spring AI's ONNX
transformers integration) rather than a hosted API.

Tradeoff: a local model means no extra API key, no per-call cost, and no third AI vendor —
good for a self-contained service. The price is a heavy dependency: the stack pulls DJL's
PyTorch engine (libtorch), which inflates the image to ~1.5 GB and, by default, downloads
native libraries at startup. For a production system at scale, a hosted embedding model
(OpenAI, or Voyage — Anthropic's recommended partner, which tops 2026 retrieval benchmarks)
would shrink the image and likely improve retrieval quality; switching is a config + vector
`dimensions` change, not a redesign. The local model was the right call for *this* project's
goals (self-contained, free, offline-capable), and the seam is clean enough to revisit.

## Deployment: App Runner, offline-bundled image

The deployment target is **AWS App Runner** with **RDS Postgres** (pgvector is a supported
RDS extension). App Runner over ECS Fargate: it's fully managed with built-in TLS and load
balancing, and scales to zero — the cheapest option for a low-traffic portfolio service,
at the cost of less network control and a per-vCPU premium. Fargate would be the choice for
finer-grained networking/IAM or multi-container orchestration.

Because App Runner scales to zero, **cold starts matter**, and the embedding stack's default
behavior — downloading hundreds of MB of native libs (and a ~90 MB model) on first start —
would make every cold start slow and network-dependent. So the image bundles the linux/amd64
PyTorch native libs (via a `linux-image` Maven profile) and bakes the model + tokenizer in
at build time, with `ai.djl.offline=true` so nothing is fetched at runtime. The image is
large, but instances start cold without touching the network. A readiness probe
(`/actuator/health/readiness`) gates traffic until the model has loaded and the database is
reachable. Secrets (`ANTHROPIC_API_KEY`, DB password) come from Secrets Manager, injected as
runtime env; nothing sensitive is baked into the image.

## Testing

Unit tests are hermetic — the Anthropic SDK and `VectorStore` are mocked, so they need no
network or API key. A small set of Testcontainers integration tests exercises the real
database layer (Flyway migrations, `validate`, jsonb round-trips, and a true
embeddings + pgvector similarity check). SDK signatures were verified against the resolved
jars rather than assumed, which caught real surprises (the MCP annotations live in
`org.springaicommunity`, not `org.springframework.ai`; the bundled embedding model is a Git
LFS pointer, not the model).

## Out of scope / future work

- **Auth and multi-tenancy** — deliberately omitted for v0.1.
- **Retrieval-augmented analysis** — Phase 3 shipped similarity search; the natural next
  step is feeding similar past JDs to Claude during analysis to surface cross-role patterns.
- **Embedding backfill** — JDs extracted before Phase 3 aren't indexed; a backfill job would
  cover them.
- **Hosted embeddings** — would resolve the image-size/cold-start tension if the service
  grew beyond a demo.
