package com.margins.ai;

import com.margins.persona.dto.PersonaDraftDto;
import com.margins.persona.model.PersonaRoleCatalog;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiSafetyPolicy {

    private static final Pattern UNSAFE_MARKERS = Pattern.compile(
        "(?i)\\b(slur|hate|harass|humiliate|explicit sexual|self-harm|suicide|kill yourself|terrorist instruction|weapon recipe|graphic violence)\\b"
    );

    public String instructions() {
        return """
            Safety policy: keep all generated reading-app content respectful, non-harassing, and book-grounded.
            Do not create personas or answers that use slurs, target protected classes, sexualize minors, encourage self-harm, provide violent wrongdoing instructions, or invent graphic material unrelated to the reader's provided book context.
            If a requested angle is unsafe, redirect to a neutral literary analysis role and keep the answer concise.
            """;
    }

    public PersonaDraftDto safePersonaDraft(PersonaDraftDto draft, String roleKey, String bookTitle) {
        String normalizedRoleKey = PersonaRoleCatalog.normalize(roleKey, 0);
        if (!isUnsafe(draft)) {
            return PersonaDraftDto.builder()
                .displayName(nonBlank(draft.getDisplayName(), fallbackName(normalizedRoleKey)))
                .description(nonBlank(draft.getDescription(), fallbackDescription(normalizedRoleKey, bookTitle)))
                .tone(nonBlank(draft.getTone(), fallbackTone(normalizedRoleKey)))
                .roleKey(normalizedRoleKey)
                .systemPrompt(nonBlank(draft.getSystemPrompt(), fallbackPrompt(normalizedRoleKey, bookTitle)))
                .reason(nonBlank(draft.getReason(), "Keeps the debate useful while staying grounded in the reader's book context."))
                .build();
        }

        return PersonaDraftDto.builder()
            .displayName(fallbackName(normalizedRoleKey))
            .description(fallbackDescription(normalizedRoleKey, bookTitle))
            .tone(fallbackTone(normalizedRoleKey))
            .roleKey(normalizedRoleKey)
            .systemPrompt(fallbackPrompt(normalizedRoleKey, bookTitle))
            .reason("Replaced by the safety policy because the generated draft was not appropriate for the reading room.")
            .build();
    }

    public boolean isUnsafe(PersonaDraftDto draft) {
        if (draft == null) {
            return true;
        }
        String combined = String.join(" ",
            safe(draft.getDisplayName()),
            safe(draft.getDescription()),
            safe(draft.getTone()),
            safe(draft.getSystemPrompt()),
            safe(draft.getReason()))
            .toLowerCase(Locale.ROOT);
        return UNSAFE_MARKERS.matcher(combined).find();
    }

    private String fallbackName(String roleKey) {
        return switch (roleKey) {
            case PersonaRoleCatalog.SKEPTIC -> "Skeptical Reader";
            case PersonaRoleCatalog.CONNECTOR -> "Connection Builder";
            case PersonaRoleCatalog.EMPATHY_READER -> "Empathy Reader";
            case PersonaRoleCatalog.STYLE_READER -> "Style Reader";
            default -> "Evidence Analyst";
        };
    }

    private String fallbackDescription(String roleKey, String bookTitle) {
        String target = nonBlank(bookTitle, "the selected book");
        return switch (roleKey) {
            case PersonaRoleCatalog.SKEPTIC -> "Challenges claims about " + target + " with constructive questions.";
            case PersonaRoleCatalog.CONNECTOR -> "Connects observations from " + target + " into broader themes.";
            case PersonaRoleCatalog.EMPATHY_READER -> "Explores emotional stakes and reader response in " + target + ".";
            case PersonaRoleCatalog.STYLE_READER -> "Looks at language, form, and craft choices in " + target + ".";
            default -> "Grounds discussion of " + target + " in specific textual evidence.";
        };
    }

    private String fallbackTone(String roleKey) {
        return switch (roleKey) {
            case PersonaRoleCatalog.SKEPTIC -> "critical";
            case PersonaRoleCatalog.CONNECTOR -> "synthetic";
            case PersonaRoleCatalog.EMPATHY_READER -> "warm";
            case PersonaRoleCatalog.STYLE_READER -> "observant";
            default -> "analytical";
        };
    }

    private String fallbackPrompt(String roleKey, String bookTitle) {
        String target = nonBlank(bookTitle, "the selected book");
        return switch (roleKey) {
            case PersonaRoleCatalog.SKEPTIC -> "Respond as a skeptical but respectful reader of " + target + ". Challenge weak claims and ask for evidence.";
            case PersonaRoleCatalog.CONNECTOR -> "Respond as a connection builder for " + target + ". Link details, themes, and prior reader notes.";
            case PersonaRoleCatalog.EMPATHY_READER -> "Respond as an empathetic reader of " + target + ". Explore emotional stakes without overclaiming.";
            case PersonaRoleCatalog.STYLE_READER -> "Respond as a style reader of " + target + ". Discuss language, form, and craft using reader-provided context.";
            default -> "Respond as an evidence analyst for " + target + ". Ground replies in quoted or summarized reader context.";
        };
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
