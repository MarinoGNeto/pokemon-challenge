package com.marinogneto.pokemon;

import org.springframework.boot.SpringApplication;

public class TestPokemonApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(PokemonApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
