package com.rafa.jdmatch.jd;

import java.util.UUID;

/** A persisted JD and its extracted requirements, as returned by {@link JdService}. */
public record ExtractedJd(UUID id, JdRequirements requirements) {
}
