package com.margins.ai;

import org.springframework.stereotype.Component;

@Component
public class AiAnswerQualityPolicy {

    public String instructions() {
        return """
            Response structure:
            - Include a short "Evidence:" sentence naming the provided quote, note, message, or question that supports the answer when possible.
            - Include a short "Uncertainty:" sentence when context is incomplete, ambiguous, or insufficient.
            """;
    }

    public String ensureSections(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            normalized = "I do not have enough context to answer yet.";
        }

        StringBuilder builder = new StringBuilder(normalized);
        if (!containsSection(normalized, "Evidence:")) {
            builder.append("\n\nEvidence: Based on the reader-provided session context available to this response.");
        }
        if (!containsSection(normalized, "Uncertainty:")) {
            builder.append("\n\nUncertainty: If the current notes or reading position do not cover this point, treat this as a provisional interpretation.");
        }
        return builder.toString();
    }

    private boolean containsSection(String content, String label) {
        return content.toLowerCase().contains(label.toLowerCase());
    }
}
