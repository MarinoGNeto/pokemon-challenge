package com.marinogneto.pokemon.domain.pokemon;

/** The six core statistics. */
public record BaseStats(int hp, int attack, int defense, int specialAttack, int specialDefense, int speed) {

    public BaseStats {
        for (int value : new int[] {hp, attack, defense, specialAttack, specialDefense, speed}) {
            if (value < 0) {
                throw new IllegalArgumentException("Base stats must not be negative");
            }
        }
    }

    public int total() {
        return hp + attack + defense + specialAttack + specialDefense + speed;
    }
}
