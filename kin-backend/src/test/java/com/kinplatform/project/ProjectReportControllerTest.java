package com.kinplatform.project;

import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.kin.reporting.report.ReportRepository;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;
import com.kinplatform.kin.reporting.report.model.FinancialSection;
import com.kinplatform.kin.reporting.report.model.InnovationSection;
import com.kinplatform.kin.reporting.report.model.MarketSection;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;
import com.kinplatform.kin.reporting.report.model.ReportBuilder;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import com.kinplatform.kin.reporting.report.model.RisksSection;
import com.kinplatform.kin.reporting.report.model.ScoresSection;
import com.kinplatform.kin.reporting.report.model.SourcesSection;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectReportControllerTest {

    private static final String EMAIL = "user@kin.test";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    private MockMvc mockMvc;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    private Project ownedProject;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProjectReportController(reportRepository, projectRepository, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        ownedProject = Project.builder()
                .id(PROJECT_ID)
                .user(User.builder().id(USER_ID).email(EMAIL).role(UserRole.FREE).build())
                .title("Proyecto")
                .status(ProjectStatus.DRAFT)
                .build();
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());
    }

    private void stubUser() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_ID).email(EMAIL).role(UserRole.FREE).build()));
    }

    private void stubOwnedProject() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(ownedProject));
    }

    private ConsultingReport report() {
        return ReportBuilder.create(PROJECT_ID)
                .executiveSummary(ExecutiveSummary.empty())
                .scores(ScoresSection.empty())
                .recommendations(RecommendationsSection.empty())
                .risks(RisksSection.empty())
                .opportunities(OpportunitiesSection.empty())
                .financial(FinancialSection.empty())
                .market(MarketSection.empty())
                .innovation(InnovationSection.empty())
                .nextSteps(NextStepsSection.empty())
                .sources(SourcesSection.empty())
                .metadata(new ReportMetadata("v1", "2.0.0-alpha.1", OffsetDateTime.now(),
                        "ReportEngine", Map.of(), 80.0, 0.85, List.of()))
                .build();
    }

    @Test
    void getReport_deberiaDevolverLaUltimaVersion() throws Exception {
        stubUser();
        stubOwnedProject();
        when(reportRepository.findLatest(PROJECT_ID))
                .thenReturn(Optional.of(new ReportRepository.StoredReport(3, report())));

        mockMvc.perform(get("/projects/{id}/report", PROJECT_ID)
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.reportVersion").value("v1"))
                .andExpect(jsonPath("$.reportId").value(report().id().toString()))
                .andExpect(jsonPath("$.report.executiveSummary").exists())
                .andExpect(jsonPath("$.report.metadata.reportVersion").value("v1"));
    }

    @Test
    void getReport_sinReporte_deberiaDevolver404() throws Exception {
        stubUser();
        stubOwnedProject();
        when(reportRepository.findLatest(PROJECT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{id}/report", PROJECT_ID)
                        .principal(principal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getReport_conVersion_deberiaDevolverEsaVersion() throws Exception {
        stubUser();
        stubOwnedProject();
        when(reportRepository.findByVersion(PROJECT_ID, 2))
                .thenReturn(Optional.of(new ReportRepository.StoredReport(2, report())));

        mockMvc.perform(get("/projects/{id}/report", PROJECT_ID)
                        .param("version", "2")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        verify(reportRepository).findByVersion(PROJECT_ID, 2);
    }

    @Test
    void getReport_proyectoAjeno_deberiaDevolver404() throws Exception {
        stubUser();
        var other = Project.builder()
                .id(PROJECT_ID)
                .user(User.builder().id(UUID.randomUUID()).email("other@kin.test")
                        .role(UserRole.FREE).build())
                .title("Ajeno")
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(other));

        mockMvc.perform(get("/projects/{id}/report", PROJECT_ID)
                        .principal(principal()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReport_proyectoInexistente_deberiaDevolver404() throws Exception {
        stubUser();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{id}/report", PROJECT_ID)
                        .principal(principal()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReports_deberiaDevolverMetadataDeVersiones() throws Exception {
        stubUser();
        stubOwnedProject();
        when(reportRepository.listVersions(PROJECT_ID)).thenReturn(List.of(
                new ReportRepository.ReportVersionInfo(1, UUID.randomUUID(), "v1", OffsetDateTime.now()),
                new ReportRepository.ReportVersionInfo(2, UUID.randomUUID(), "v1", OffsetDateTime.now())));

        mockMvc.perform(get("/projects/{id}/reports", PROJECT_ID)
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[1].version").value(2))
                .andExpect(jsonPath("$[0].reportVersion").value("v1"));
    }

    @Test
    void getReport_sinAutenticacion_deberiaRequerirUsuario() throws Exception {
        mockMvc.perform(get("/projects/{id}/report", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(reportRepository, org.mockito.Mockito.never())
                .findLatest(any());
        verify(projectRepository, org.mockito.Mockito.never())
                .findById(eq(PROJECT_ID));
    }
}
