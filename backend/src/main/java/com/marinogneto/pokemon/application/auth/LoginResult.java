package com.marinogneto.pokemon.application.auth;

import com.marinogneto.pokemon.application.port.out.AccessToken;
import com.marinogneto.pokemon.domain.user.User;

/** A successful login: the token and who it belongs to. */
public record LoginResult(AccessToken token, User user) {
}
