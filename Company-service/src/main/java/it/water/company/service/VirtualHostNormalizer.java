package it.water.company.service;

import java.net.URI;
import java.util.Locale;

/**
 * Canonicalizes a host value so HTTP Host headers and persisted Company values match.
 */
final class VirtualHostNormalizer {

    private VirtualHostNormalizer() {
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String candidate = value.trim().toLowerCase(Locale.ROOT);
        try {
            URI uri = candidate.contains("://")
                    ? URI.create(candidate)
                    : URI.create("http://" + candidate);
            if (uri.getHost() != null) {
                candidate = uri.getHost();
            }
        } catch (IllegalArgumentException ignored) {
            int colon = candidate.lastIndexOf(':');
            if (colon > 0 && candidate.indexOf(':') == colon) {
                candidate = candidate.substring(0, colon);
            }
        }

        while (candidate.endsWith(".")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }
}
