package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUsersTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void require_deberiaDevolverElUsuario() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .email("a@kin.com")
                .role(UserRole.FREE)
                .build();
        when(userRepository.findByEmail("a@kin.com")).thenReturn(Optional.of(user));
        var auth = new UsernamePasswordAuthenticationToken("a@kin.com", null);

        assertEquals(user, AuthenticatedUsers.require(userRepository, auth));
    }

    @Test
    void require_usuarioInexistente_deberiaFallar() {
        when(userRepository.findByEmail("ghost@kin.com")).thenReturn(Optional.empty());
        var auth = new UsernamePasswordAuthenticationToken("ghost@kin.com", null);

        assertThrows(IllegalArgumentException.class, () -> AuthenticatedUsers.require(userRepository, auth));
    }

    @Test
    void require_sinAutenticacion_deberiaFallar() {
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedUsers.require(userRepository, null));
    }
}
