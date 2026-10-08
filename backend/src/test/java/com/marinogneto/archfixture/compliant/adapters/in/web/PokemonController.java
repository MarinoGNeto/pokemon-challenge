package com.marinogneto.archfixture.compliant.adapters.in.web;

import com.marinogneto.archfixture.compliant.application.GetPokemon;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PokemonController {

    private final GetPokemon getPokemon;

    public PokemonController(GetPokemon getPokemon) {
        this.getPokemon = getPokemon;
    }

    public String name(int id) {
        return getPokemon.handle(id).name();
    }
}
