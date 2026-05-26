package com.rafa.jdmatch.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafa.jdmatch.claude.ClaudeClient;
import com.rafa.jdmatch.claude.ClaudeException;
import com.rafa.jdmatch.claude.ClaudeProperties;
import com.rafa.jdmatch.common.Hashing;
import com.rafa.jdmatch.jd.ExtractedJd;
import com.rafa.jdmatch.jd.JdService;
import com.rafa.jdmatch.resume.ResumeService;
import com.rafa.jdmatch.resume.StoredResume;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Stage three: compare a resume against a job description's extracted requirements.
 * Orchestrates the pipeline — extract JD requirements, store the resume, then ask
 * Claude to compare. Idempotent per (jd, resume, compare prompt version): the same
 * pair returns the cached analysis instead of calling Claude again.
 */
@Service
public class AnalysisService {

    static final String PROMPT_NAME = "compare-requirements";
    static final String PROMPT_VERSION = "v1";

    private final JdService jdService;
    private final ResumeService resumeService;
    private final ClaudeClient claudeClient;
    private final AnalysisRepository repository;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper objectMapper;

    public AnalysisService(JdService jdService, ResumeService resumeService,
                           ClaudeClient claudeClient, AnalysisRepository repository,
                           ClaudeProperties claudeProperties, ObjectMapper objectMapper) {
        this.jdService = jdService;
        this.resumeService = resumeService;
        this.claudeClient = claudeClient;
        this.repository = repository;
        this.claudeProperties = claudeProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisOutcome analyze(String jdText, String resumeText) {
        ExtractedJd jd = jdService.getOrExtract(jdText);
        StoredResume resume = resumeService.getOrStore(resumeText);

        String key = idempotencyKey(jd.id(), resume.id());
        return repository.findByIdempotencyKey(key)
                .map(this::toResult)
                .map(result -> new AnalysisOutcome(result, false))
                .orElseGet(() -> new AnalysisOutcome(compare(key, jd, resume.id(), resumeText), true));
    }

    @Transactional(readOnly = true)
    public AnalysisResult get(UUID id) {
        return repository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
    }

    private AnalysisResult compare(String key, ExtractedJd jd, UUID resumeId, String resumeText) {
        String content = "EXTRACTED JOB REQUIREMENTS (JSON):\n" + toJson(jd.requirements())
                + "\n\n---\n\nRESUME:\n" + resumeText;
        GapAnalysis analysis = claudeClient.extract(PROMPT_NAME, content, GapAnalysis.class);

        JpaAnalysis saved = repository.save(new JpaAnalysis(
                UUID.randomUUID(), key, jd.id(), resumeId,
                toJson(analysis), claudeProperties.model(), PROMPT_VERSION, Instant.now()));

        return new AnalysisResult(saved.getId(), analysis, saved.getModel(), saved.getCreatedAt());
    }

    private AnalysisResult toResult(JpaAnalysis entity) {
        return new AnalysisResult(
                entity.getId(), fromJson(entity.getAnalysisJson()),
                entity.getModel(), entity.getCreatedAt());
    }

    private static String idempotencyKey(UUID jdId, UUID resumeId) {
        return Hashing.sha256Hex(jdId + ":" + resumeId + ":" + PROMPT_VERSION);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ClaudeException("Could not serialize value", e);
        }
    }

    private GapAnalysis fromJson(String json) {
        try {
            return objectMapper.readValue(json, GapAnalysis.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize stored analysis", e);
        }
    }
}
