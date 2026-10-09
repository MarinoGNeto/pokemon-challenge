package com.marinogneto.pokemon.domain.localpokemon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Our own data about a Pokemon (US03): never overwritten by a re-sync, editable through US04.
 * Values are normalised on creation (trimmed, blank → absent, tags lower-cased and de-duplicated) and validated;
 * a violation raises {@link InvalidLocalPokemonException} naming the field.
 *
 * @param localizedName name in another language, e.g. "フシギダネ" or "Bulbizarre"
 * @param region        geographical metadata: game region, e.g. "Kanto"
 * @param habitat       geographical metadata: natural habitat, e.g. "grassland"
 * @param tags          internal classification, e.g. ["starter", "gen-1"]
 * @param notes         free text
 */
public record ProprietaryData(String localizedName, String region, String habitat, List<String> tags,
                              String notes) {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_PLACE_LENGTH = 50;
    public static final int MAX_TAGS = 10;
    public static final int MAX_TAG_LENGTH = 30;
    public static final int MAX_NOTES_LENGTH = 1000;
    /** Lowercase words joined by single dashes: "starter", "gen-1", "fan-favourite". */
    public static final String TAG_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    private static final Pattern TAG = Pattern.compile(TAG_PATTERN);

    public ProprietaryData {
        localizedName = text("localizedName", localizedName, MAX_NAME_LENGTH);
        region = text("region", region, MAX_PLACE_LENGTH);
        habitat = text("habitat", habitat, MAX_PLACE_LENGTH);
        notes = text("notes", notes, MAX_NOTES_LENGTH);
        tags = tags(tags);
    }

    public static ProprietaryData empty() {
        return new ProprietaryData(null, null, null, List.of(), null);
    }

    private static String text(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new InvalidLocalPokemonException(field, "must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static List<String> tags(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> normalised = new LinkedHashSet<>();
        for (String tag : raw) {
            String value = tag == null ? "" : tag.strip().toLowerCase(Locale.ROOT);
            if (value.length() > MAX_TAG_LENGTH || !TAG.matcher(value).matches()) {
                throw new InvalidLocalPokemonException("tags",
                        "each tag must be 1-" + MAX_TAG_LENGTH + " lowercase letters or digits, words joined by '-'");
            }
            normalised.add(value);
        }
        if (normalised.size() > MAX_TAGS) {
            throw new InvalidLocalPokemonException("tags", "must have at most " + MAX_TAGS + " tags");
        }
        return List.copyOf(new ArrayList<>(normalised));
    }
}
