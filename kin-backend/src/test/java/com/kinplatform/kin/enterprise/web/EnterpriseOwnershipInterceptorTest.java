package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.ports.EnterpriseProjectAccessControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests del interceptor de autorización Enterprise (remediación C1, IDOR):
 * bloquea con 404 el acceso a proyectos ajenos y permite el propio.
 */
class EnterpriseOwnershipInterceptorTest {

    private final EnterpriseProjectAccessControl accessControl =
        mock(EnterpriseProjectAccessControl.class);
    private final EnterpriseOwnershipInterceptor interceptor =
        new EnterpriseOwnershipInterceptor(accessControl);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_conProyectoPropio_permite() throws Exception {
        var projectId = UUID.randomUUID();
        authenticate("owner@kin.test");
        when(accessControl.isOwner(projectId, "owner@kin.test")).thenReturn(true);

        boolean result = interceptor.preHandle(
            request("/api/v1/enterprise/" + projectId + "/1/dashboard"),
            new MockHttpServletResponse(), new Object());

        assertTrue(result);
    }

    @Test
    void preHandle_conProyectoAjeno_lanzaNotFound() {
        var projectId = UUID.randomUUID();
        authenticate("other@kin.test");
        when(accessControl.isOwner(projectId, "other@kin.test")).thenReturn(false);

        assertThrows(EnterpriseNotFoundException.class,
            () -> interceptor.preHandle(
                request("/api/v1/enterprise/" + projectId + "/1/export/LEAN_CANVAS/PDF"),
                new MockHttpServletResponse(), new Object()));
    }

    @Test
    void preHandle_sinUsuarioAutenticado_lanzaNotFound() {
        var projectId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
        when(accessControl.isOwner(projectId, null)).thenReturn(false);

        assertThrows(EnterpriseNotFoundException.class,
            () -> interceptor.preHandle(
                request("/enterprise/" + projectId + "/latest"),
                new MockHttpServletResponse(), new Object()));
    }

    @Test
    void preHandle_rutaNoEnterprise_permite() throws Exception {
        boolean result = interceptor.preHandle(
            request("/api/v1/projects"), new MockHttpServletResponse(), new Object());

        assertTrue(result);
    }

    @Test
    void preHandle_projectIdInvalido_permiteAlController() throws Exception {
        boolean result = interceptor.preHandle(
            request("/api/v1/enterprise/not-a-uuid/1/status"),
            new MockHttpServletResponse(), new Object());

        assertTrue(result);
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(email, "x"));
    }

    private MockHttpServletRequest request(String uri) {
        var request = new MockHttpServletRequest("GET", uri);
        request.setContextPath("/api/v1");
        return request;
    }
}
