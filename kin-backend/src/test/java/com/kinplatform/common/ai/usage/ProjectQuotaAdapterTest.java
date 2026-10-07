package com.kinplatform.common.ai.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectQuotaAdapterTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    private ProjectQuotaAdapter quota() {
        return new ProjectQuotaAdapter(userRepository);
    }

    private User userWith(int completed) {
        return User.builder().id(USER_ID).completedProjects(completed).build();
    }

    @Test
    void canComplete_limiteNulo_ilimitado() {
        assertTrue(quota().canComplete(USER_ID, null));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void canComplete_bajoLimite_permitido() {
        when(userRepository.rolloverCompletedProjects(eq(USER_ID), any())).thenReturn(0);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWith(2)));

        assertTrue(quota().canComplete(USER_ID, 3));
    }

    @Test
    void canComplete_enLimite_bloqueado() {
        when(userRepository.rolloverCompletedProjects(eq(USER_ID), any())).thenReturn(0);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWith(3)));

        assertFalse(quota().canComplete(USER_ID, 3));
    }

    @Test
    void tryIncrementCompleted_delegaEnIncrementoAtomico() {
        when(userRepository.tryIncrementCompletedProjects(USER_ID, 3)).thenReturn(1);

        assertTrue(quota().tryIncrementCompleted(USER_ID, 3));
        verify(userRepository).tryIncrementCompletedProjects(USER_ID, 3);
    }

    @Test
    void tryIncrementCompleted_limiteSuperado_devuelveFalse() {
        when(userRepository.tryIncrementCompletedProjects(USER_ID, 3)).thenReturn(0);

        assertFalse(quota().tryIncrementCompleted(USER_ID, 3));
    }

    @Test
    void completedProjects_devuelveContador() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWith(4)));

        assertEquals(4, quota().completedProjects(USER_ID));
    }
}


