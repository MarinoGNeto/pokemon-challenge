package com.marinogneto.pokemon.application.port.out;

import java.time.Instant;

/** A signed bearer token and when it stops being accepted. */
public record AccessToken(String value, Instant expiresAt) {
}
