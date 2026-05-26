package com.rafa.jdmatch.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findByResumeHash(String resumeHash);
}
