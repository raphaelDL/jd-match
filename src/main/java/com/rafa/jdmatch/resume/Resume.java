package com.rafa.jdmatch.resume;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A stored resume. Keyed by {@code resumeHash} so identical resume text is stored
 * once. v1 keeps the text only — no structured parsing.
 */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "resume_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String resumeHash;

    @Column(name = "resume_text", nullable = false, updatable = false, columnDefinition = "text")
    private String resumeText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Resume() {
        // for JPA
    }

    public Resume(UUID id, String resumeHash, String resumeText, Instant createdAt) {
        this.id = id;
        this.resumeHash = resumeHash;
        this.resumeText = resumeText;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getResumeHash() {
        return resumeHash;
    }

    public String getResumeText() {
        return resumeText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
