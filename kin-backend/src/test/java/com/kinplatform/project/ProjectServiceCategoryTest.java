package com.kinplatform.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.chat.ChatMessageRepository;
import com.kinplatform.pricing.service.SubscriptionValidatorService;
import com.kinplatform.project.dto.CreateProjectRequest;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceCategoryTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SubscriptionValidatorService subscriptionValidatorService;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void create_deberiaResolverLaCategoriaPorCodigo() {
        var user = User.builder()
                .id(USER_ID)
                .email("u@kin.com")
                .role(UserRole.FREE)
                .build();
        var categoria = Category.builder()
                .code("SALUD")
                .name("Salud")
                .displayOrder(4)
                .color("#ef4444")
                .active(true)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(subscriptionValidatorService.canCreateProject(USER_ID)).thenReturn(true);
        when(categoryRepository.findByCode("SALUD")).thenReturn(Optional.of(categoria));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var service = new ProjectServiceImpl(
                projectRepository,
                userRepository,
                chatMessageRepository,
                subscriptionValidatorService,
                new CategoryService(categoryRepository));
        var request = new CreateProjectRequest();
        request.setTitle("Centro de salud");
        request.setCategory("SALUD");

        var response = service.create(USER_ID, request);

        assertEquals("SALUD", response.getCategory());
        assertEquals("Salud", response.getCategoryName());
        assertEquals("#ef4444", response.getCategoryColor());
    }

    @Test
    void delete_deberiaInvalidarLaCacheDeProjectLimit() {
        var project = Project.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(USER_ID).build())
                .title("Proyecto a borrar")
                .build();
        when(projectRepository.findById(any())).thenReturn(Optional.of(project));

        var service = new ProjectServiceImpl(
                projectRepository,
                userRepository,
                chatMessageRepository,
                subscriptionValidatorService,
                new CategoryService(categoryRepository));
        service.delete(USER_ID, project.getId());

        verify(subscriptionValidatorService).evictProjectLimitCache(USER_ID);
    }
}
