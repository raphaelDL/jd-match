package com.rafa.jdmatch.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted analysis row. A plain class (not a record) because JPA requires a
 * no-arg constructor and mutable mapping. The structured result is stored as
 * {@code jsonb}; callers deserialize {@code analysisJson} into a {@link GapAnalysis}.
 */
@Entity
@Table(name = "analyses")
public class JpaAnalysis {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "jd_text", nullable = false, updatable = false, columnDefinition = "text")
    private String jdText;

    @Column(name = "resume_text", nullable = false, updatable = false, columnDefinition = "text")
    private String resumeText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_json", nullable = false, columnDefinition = "jsonb")
    private String analysisJson;

    @Column(nullable = false, updatable = false, length = 64)
    private String model;

    @Column(name = "prompt_version", nullable = false, updatable = false, length = 32)
    private String promptVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JpaAnalysis() {
        // for JPA
    }

    public JpaAnalysis(UUID id, String idempotencyKey, String jdText, String resumeText,
                       String analysisJson, String model, String promptVersion, Instant createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.jdText = jdText;
        this.resumeText = resumeText;
        this.analysisJson = analysisJson;
        this.model = model;
        this.promptVersion = promptVersion;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getJdText() {
        return jdText;
    }

    public String getResumeText() {
        return resumeText;
    }

    public String getAnalysisJson() {
        return analysisJson;
    }

    public String getModel() {
        return model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
