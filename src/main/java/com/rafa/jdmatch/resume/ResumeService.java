package com.rafa.jdmatch.resume;

import com.rafa.jdmatch.common.Hashing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Stage two: store the resume. Idempotent — identical resume text maps to the same
 * stored row, so repeated submissions don't create duplicates.
 */
@Service
public class ResumeService {

    private final ResumeRepository repository;

    public ResumeService(ResumeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StoredResume getOrStore(String resumeText) {
        String hash = Hashing.sha256Hex(Hashing.normalizeWhitespace(resumeText));
        return repository.findByResumeHash(hash)
                .map(resume -> new StoredResume(resume.getId()))
                .orElseGet(() -> {
                    Resume saved = repository.save(
                            new Resume(UUID.randomUUID(), hash, resumeText, Instant.now()));
                    return new StoredResume(saved.getId());
                });
    }
}
