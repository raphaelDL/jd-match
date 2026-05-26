package com.rafa.jdmatch.analysis;

import java.util.UUID;

public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(UUID id) {
        super("No analysis found with id " + id);
    }
}
