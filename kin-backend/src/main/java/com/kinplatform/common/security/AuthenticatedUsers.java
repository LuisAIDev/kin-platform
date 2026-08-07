package com.kinplatform.common.security;

import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import org.springframework.security.core.Authentication;

/**
 * Utilidad compartida para resolver el {@link User} autenticado desde el
 * {@link Authentication} de Spring Security. Elimina la duplicación de
 * {@code userRepository.findByEmail(auth.getName())...} en los controllers.
 */
public final class AuthenticatedUsers {

    private AuthenticatedUsers() {}

    public static User require(UserRepository userRepository, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }
        return userRepository
                .findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
