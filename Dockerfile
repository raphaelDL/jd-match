# syntax=docker/dockerfile:1

# --- build stage -----------------------------------------------------------
# Packages the app with the linux/amd64 PyTorch native libs (linux-image profile)
# and bakes the embedding model + tokenizer into the image, so the runtime never
# downloads anything (see ai.djl.offline=true in the runtime stage).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .
RUN mvn -B -Plinux-image -DskipTests clean package

# The default model URI is a GitHub LFS-media link; fetch the real ~90MB model
# (and the tokenizer) once, here, where the build has network access.
ARG MODEL_URL="https://media.githubusercontent.com/media/spring-projects/spring-ai/refs/heads/main/models/spring-ai-transformers/src/main/resources/onnx/all-MiniLM-L6-v2/model.onnx"
ARG TOKENIZER_URL="https://raw.githubusercontent.com/spring-projects/spring-ai/main/models/spring-ai-transformers/src/main/resources/onnx/all-MiniLM-L6-v2/tokenizer.json"
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /models/all-MiniLM-L6-v2 \
    && curl -fsSL -o /models/all-MiniLM-L6-v2/model.onnx "${MODEL_URL}" \
    && curl -fsSL -o /models/all-MiniLM-L6-v2/tokenizer.json "${TOKENIZER_URL}"

# --- runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user (no fixed UID — the Ubuntu base already uses 1000).
RUN useradd --system --create-home --home-dir /home/appuser appuser

COPY --from=build /workspace/target/jd-match-*.jar /app/app.jar
COPY --from=build /models /app/models

# Offline embeddings: use the bundled native libs and the baked model/tokenizer,
# so nothing is fetched at startup (fast, network-free cold starts). DJL still
# unpacks the bundled native libs to a cache dir at load, so give it a writable one.
ENV JAVA_TOOL_OPTIONS="-Dai.djl.offline=true \
-Dspring.ai.embedding.transformer.onnx.model-uri=file:/app/models/all-MiniLM-L6-v2/model.onnx \
-Dspring.ai.embedding.transformer.tokenizer.uri=file:/app/models/all-MiniLM-L6-v2/tokenizer.json" \
    DJL_CACHE_DIR=/home/appuser/.djl

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
