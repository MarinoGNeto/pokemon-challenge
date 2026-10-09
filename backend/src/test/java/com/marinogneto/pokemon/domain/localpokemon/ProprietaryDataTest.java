package com.marinogneto.pokemon.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** US03 proprietary fields: localized name, geographical metadata (region, habitat), tags, notes. */
class ProprietaryDataTest {

    @Test
    void emptyHasNoValues() {
        ProprietaryData empty = ProprietaryData.empty();

        assertThat(empty.localizedName()).isNull();
        assertThat(empty.tags()).isEmpty();
    }

    @Test
    void textIsTrimmedAndBlankBecomesAbsent() {
        ProprietaryData data = new ProprietaryData("  フシギダネ ", " Kanto ", "   ", List.of(), "");

        assertThat(data.localizedName()).isEqualTo("フシギダネ");
        assertThat(data.region()).isEqualTo("Kanto");
        assertThat(data.habitat()).isNull();
        assertThat(data.notes()).isNull();
    }

    @Test
    void tagsAreNormalisedAndDeduplicatedKeepingOrder() {
        ProprietaryData data = new ProprietaryData(null, null, null, List.of(" Starter", "GRASS", "starter", "gen-1"),
                null);

        assertThat(data.tags()).containsExactly("starter", "grass", "gen-1");
    }

    @Test
    void nullTagListMeansNoTags() {
        assertThat(new ProprietaryData(null, null, null, null, null).tags()).isEmpty();
    }

    @Test
    void tooLongTextIsRejectedNamingTheField() {
        assertThatThrownBy(() -> new ProprietaryData("x".repeat(ProprietaryData.MAX_NAME_LENGTH + 1), null, null,
                List.of(), null))
                .isInstanceOf(InvalidLocalPokemonException.class)
                .extracting("field").isEqualTo("localizedName");
        assertThatThrownBy(() -> new ProprietaryData(null, "x".repeat(ProprietaryData.MAX_PLACE_LENGTH + 1), null,
                List.of(), null))
                .extracting("field").isEqualTo("region");
        assertThatThrownBy(() -> new ProprietaryData(null, null, null, List.of(),
                "x".repeat(ProprietaryData.MAX_NOTES_LENGTH + 1)))
                .extracting("field").isEqualTo("notes");
    }

    @Test
    void notesAtTheLimitAreAccepted() {
        String notes = "x".repeat(ProprietaryData.MAX_NOTES_LENGTH);

        assertThat(new ProprietaryData(null, null, null, List.of(), notes).notes()).hasSize(1000);
    }

    @Test
    void invalidTagsAreRejected() {
        for (String tag : List.of("two words", "", "under_score", "-dash-first", "x".repeat(31))) {
            assertThatThrownBy(() -> new ProprietaryData(null, null, null, List.of(tag), null))
                    .as("tag '%s'", tag)
                    .isInstanceOf(InvalidLocalPokemonException.class)
                    .extracting("field").isEqualTo("tags");
        }
    }

    @Test
    void tooManyTagsAreRejected() {
        List<String> tags = IntStream.rangeClosed(1, ProprietaryData.MAX_TAGS + 1).mapToObj(i -> "tag-" + i).toList();

        assertThatThrownBy(() -> new ProprietaryData(null, null, null, tags, null))
                .isInstanceOf(InvalidLocalPokemonException.class)
                .extracting("field").isEqualTo("tags");
    }
}
