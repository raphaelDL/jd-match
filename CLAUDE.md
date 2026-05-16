# jd-match — Project Context for Claude Code

## What this is
A Spring Boot 3 / Java 21 backend service. Given a JD and a resume,
uses the Anthropic API to extract structured requirements and produce
a gap analysis.

## Stack
- Java 21 (use records, virtual threads, pattern matching where natural)
- Spring Boot 3.x (Web, Data JPA, Validation)
- Postgres (via docker-compose locally; AWS RDS later)
- Anthropic Java SDK (com.anthropic:anthropic-java)
- Maven (not Gradle — keep build files familiar to most Java reviewers)
- JUnit 5 + Mockito for tests

## Conventions
- Records for DTOs. Plain classes only when JPA forces it.
- Constructor injection, no @Autowired on fields.
- Package by feature, not by layer:
  com.rafa.jdmatch.{resume,jd,analysis,claude,common}
- Keep Claude API calls in one dedicated client class; never scatter
  HTTP calls across services.
- Every Claude prompt lives in its own file under
  src/main/resources/prompts/ and is loaded by name.

## My preferences when working with me
- Make small, focused commits with descriptive messages.
- Explain WHY before HOW for non-trivial decisions.
- When suggesting libraries, mention 1-2 alternatives briefly.
- Don't add features I didn't ask for. Scope discipline matters.
- If you're uncertain, say so. Don't invent API signatures.

## Out of scope (for now)
- Frontend / UI
- Auth / users
- PDF parsing (text input only for v1)
- Anything beyond v0.1.0 milestones

## Useful commands
- Run: ./mvnw spring-boot:run
- Tests: ./mvnw test
- Postgres up: docker-compose up -d
- Postgres down: docker-compose down
