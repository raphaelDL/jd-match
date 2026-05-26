package com.rafa.jdmatch.jd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A job description and its extracted requirements. Keyed by {@code jdHash} so the
 * same JD (under the current extraction prompt version) is extracted only once.
 */
@Entity
@Table(name = "jds")
public class Jd {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "jd_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String jdHash;

    @Column(name = "jd_text", nullable = false, updatable = false, columnDefinition = "text")
    private String jdText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements_json", nullable = false, columnDefinition = "jsonb")
    private String requirementsJson;

    @Column(nullable = false, updatable = false, length = 64)
    private String model;

    @Column(name = "prompt_version", nullable = false, updatable = false, length = 32)
    private String promptVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Jd() {
        // for JPA
    }

    public Jd(UUID id, String jdHash, String jdText, String requirementsJson,
              String model, String promptVersion, Instant createdAt) {
        this.id = id;
        this.jdHash = jdHash;
        this.jdText = jdText;
        this.requirementsJson = requirementsJson;
        this.model = model;
        this.promptVersion = promptVersion;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getJdHash() {
        return jdHash;
    }

    public String getJdText() {
        return jdText;
    }

    public String getRequirementsJson() {
        return requirementsJson;
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
