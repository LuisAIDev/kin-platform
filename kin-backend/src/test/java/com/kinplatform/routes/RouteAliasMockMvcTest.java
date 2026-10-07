package com.kinplatform.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.kin.health.documents.api.DocumentController;
import com.kinplatform.kin.health.documents.api.DocumentService;
import com.kinplatform.kin.health.physician.api.RelationshipService;
import com.kinplatform.kin.health.telemedicine.api.TelemedicineController;
import com.kinplatform.kin.health.telemedicine.api.TelemedicineService;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import com.kinplatform.platform.project.CategoryController;
import com.kinplatform.platform.project.CategoryResponse;
import com.kinplatform.platform.project.CategoryService;
import com.kinplatform.platform.project.ProjectController;
import com.kinplatform.platform.project.ProjectService;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Compatibilidad de rutas (Commit 1): cada controlador con alias debe responder
 * idénticamente en su ruta legacy y en su ruta por contexto.
 *
 * <p>Se usa {@code MockMvc} en modo {@code standalone} (sin contexto Spring, sin
 * base de datos) para aislar la resolución de mappings. Se comparan status y
 * body de ambas rutas para garantizar que son exactamente equivalentes.</p>
 */
@ExtendWith(MockitoExtension.class)
class RouteAliasMockMvcTest {

    private static final String EMAIL = "u@kin.com";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProjectService projectService;

    @Mock
    private TelemedicineService telemedicineService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private UserRepository userRepository;

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());
    }

    private void stubUser() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder().id(USER_ID).email(EMAIL).build()));
    }

    private MvcResult call(MockMvc mockMvc, String path) throws Exception {
        return mockMvc.perform(get(path).principal(principal())).andReturn();
    }

    private void assertSameResponse(MockMvc mockMvc, String legacy, String alias) throws Exception {
        MvcResult legacyResult = call(mockMvc, legacy);
        MvcResult aliasResult = call(mockMvc, alias);

        assertEquals(200, legacyResult.getResponse().getStatus(), "legacy " + legacy);
        assertEquals(200, aliasResult.getResponse().getStatus(), "alias " + alias);
        assertEquals(
                legacyResult.getResponse().getContentAsString(),
                aliasResult.getResponse().getContentAsString(),
                "body distinto entre " + legacy + " y " + alias);
    }

    @Test
    void category_categoriesYEmpresasCategories_deberianSerEquivalentes() throws Exception {
        when(categoryService.getActive())
                .thenReturn(List.of(new CategoryResponse(
                        UUID.randomUUID(), "SALUD", "Salud", "Salud", 1, "icon", "#123456", true)));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(categoryService))
                .build();

        assertSameResponse(mockMvc, "/categories", "/empresas/categories");
    }

    @Test
    void project_projectsYEmpresasProjects_deberianSerEquivalentes() throws Exception {
        stubUser();
        when(projectService.getAllByUser(eq(USER_ID), any()))
                .thenReturn(PageResponse.from(org.springframework.data.domain.Page.empty()));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService, userRepository))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        assertSameResponse(mockMvc, "/projects", "/empresas/projects");
    }

    @Test
    void telemedicine_healthYMedical_deberianSerEquivalentes() throws Exception {
        stubUser();
        when(telemedicineService.allMessagesFor(USER_ID)).thenReturn(List.of());

        var messageRepository = mock(MessageRepository.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new TelemedicineController(telemedicineService, userRepository, relationshipService, messageRepository, mock(AppointmentRepository.class)))
                .build();

        assertSameResponse(
                mockMvc,
                "/health/telemedicine/conversations",
                "/medical/telemedicine/conversations");
    }

    @Test
    void document_healthYMedical_deberianSerEquivalentes() throws Exception {
        stubUser();
        when(documentService.listMyDocuments(USER_ID)).thenReturn(List.of());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(documentService, userRepository))
                .build();

        assertSameResponse(mockMvc, "/health/documents/my", "/medical/documents/my");
    }
}


