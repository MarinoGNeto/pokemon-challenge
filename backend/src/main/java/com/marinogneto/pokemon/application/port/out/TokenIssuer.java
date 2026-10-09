package com.marinogneto.pokemon.application.port.out;

import com.marinogneto.pokemon.domain.user.User;

/** Issues signed access tokens (implemented with JWT). */
public interface TokenIssuer {

    AccessToken issue(User user);
}
