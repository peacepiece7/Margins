package com.margins.persona.model;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PersonaRoleCatalog {
    public static final String EVIDENCE_ANALYST = "evidence_analyst";
    public static final String SKEPTIC = "skeptic";
    public static final String CONNECTOR = "connector";
    public static final String EMPATHY_READER = "empathy_reader";
    public static final String STYLE_READER = "style_reader";

    private static final List<String> DEFAULT_ORDER = List.of(
        EVIDENCE_ANALYST,
        SKEPTIC,
        CONNECTOR,
        EMPATHY_READER,
        STYLE_READER
    );
    private static final Set<String> ALLOWED = Set.copyOf(DEFAULT_ORDER);

    private PersonaRoleCatalog() {
    }

    public static List<String> defaultOrder() {
        return DEFAULT_ORDER;
    }

    public static String normalize(String roleKey, int fallbackIndex) {
        String normalized = normalize(roleKey);
        if (normalized != null) {
            return normalized;
        }
        return DEFAULT_ORDER.get(Math.floorMod(fallbackIndex, DEFAULT_ORDER.size()));
    }

    public static String normalize(String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            return null;
        }
        String normalized = roleKey.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("(^_+|_+$)", "");
        return ALLOWED.contains(normalized) ? normalized : null;
    }
}
