package com.rafa.jdmatch.resume;

import java.util.UUID;

/** A persisted resume, as returned by {@link ResumeService}. */
public record StoredResume(UUID id) {
}
