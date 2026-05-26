package com.rafa.jdmatch.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<JpaAnalysis, UUID> {

    Optional<JpaAnalysis> findByIdempotencyKey(String idempotencyKey);
}
