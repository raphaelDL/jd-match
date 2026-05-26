/**
 * Gap-analysis pipeline: orchestrates extracted JD requirements and the stored resume
 * into a typed analysis via a compare prompt. Owns the {@code POST /analyses} and
 * {@code GET /analyses/{id}} endpoints; idempotent per (jd, resume, prompt version).
 */
package com.rafa.jdmatch.analysis;
