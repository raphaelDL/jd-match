package com.rafa.jdmatch.jd;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JdRepository extends JpaRepository<Jd, UUID> {

    Optional<Jd> findByJdHash(String jdHash);
}
