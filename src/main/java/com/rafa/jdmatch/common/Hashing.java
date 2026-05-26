package com.rafa.jdmatch.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Small shared helpers for content-addressed idempotency keys. */
public final class Hashing {

    private Hashing() {
    }

    /** Lowercase hex SHA-256 of the UTF-8 bytes of {@code input}. */
    public static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Trim and collapse internal whitespace runs to a single space. */
    public static String normalizeWhitespace(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }
}
