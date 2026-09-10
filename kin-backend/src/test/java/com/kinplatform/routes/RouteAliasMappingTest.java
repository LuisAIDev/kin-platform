package com.kinplatform.routes;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.enterprise.web.EnterpriseController;
import com.kinplatform.kin.enterprise.web.EnterpriseDashboardController;
import com.kinplatform.kin.enterprise.web.EnterpriseInformationController;
import com.kinplatform.kin.enterprise.web.EnterpriseProgressController;
import com.kinplatform.kin.health.aiassist.api.AIAssistController;
import com.kinplatform.kin.health.audit.api.AuditAdminController;
import com.kinplatform.kin.health.audit.api.AuditPatientController;
import com.kinplatform.kin.health.automation.api.AutomationController;
import com.kinplatform.kin.health.dashboard.api.DashboardController;
import com.kinplatform.kin.health.differential.api.DifferentialController;
import com.kinplatform.kin.health.documents.api.DocumentController;
import com.kinplatform.kin.health.followup.api.FollowUpController;
import com.kinplatform.kin.health.physician.api.PatientConsentController;
import com.kinplatform.kin.health.physician.api.PatientRelationshipController;
import com.kinplatform.kin.health.physician.api.PhysicianController;
import com.kinplatform.kin.health.scheduling.api.SchedulingController;
import com.kinplatform.kin.health.telemedicine.api.TelemedicineController;
import com.kinplatform.kin.health.triage.api.TriageController;
import com.kinplatform.kin.health.triage.api.TriageExportController;
import com.kinplatform.project.CategoryController;
import com.kinplatform.project.ProjectController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Compatibilidad de rutas (Commit 1): verifica de forma exhaustiva que TODOS los
 * controladores con alias declaran tanto la ruta legacy como la ruta por
 * contexto en su {@link RequestMapping}.
 *
 * <p>Es una prueba de registro de mappings: no requiere contexto Spring, base de
 * datos ni seguridad, por lo que es determinista y rápida.</p>
 */
class RouteAliasMappingTest {

    private record Alias(Class<?> controller, String legacy, String alias) {}

    private static final List<Alias> ALIASES = List.of(
            // Empresas
            new Alias(EnterpriseController.class, "/enterprise", "/empresas/enterprise"),
            new Alias(EnterpriseDashboardController.class, "/enterprise", "/empresas/enterprise"),
            new Alias(EnterpriseInformationController.class, "/enterprise", "/empresas/enterprise"),
            new Alias(EnterpriseProgressController.class, "/enterprise", "/empresas/enterprise"),
            new Alias(ProjectController.class, "/projects", "/empresas/projects"),
            new Alias(CategoryController.class, "/categories", "/empresas/categories"),
            // Medical
            new Alias(AIAssistController.class, "/health/aiassist", "/medical/aiassist"),
            new Alias(AuditAdminController.class, "/admin/health/audit", "/medical/admin/audit"),
            new Alias(AuditPatientController.class, "/health/audit", "/medical/audit"),
            new Alias(AutomationController.class, "/health/automation", "/medical/automation"),
            new Alias(DashboardController.class, "/health/dashboard", "/medical/dashboard"),
            new Alias(DifferentialController.class, "/health/differential", "/medical/differential"),
            new Alias(DocumentController.class, "/health/documents", "/medical/documents"),
            new Alias(FollowUpController.class, "/health/followup", "/medical/followup"),
            new Alias(PatientConsentController.class, "/health/patient/consent", "/medical/patient/consent"),
            new Alias(
                    PatientRelationshipController.class,
                    "/health/patient/relationships",
                    "/medical/patient/relationships"),
            new Alias(PhysicianController.class, "/health/physician", "/medical/physician"),
            new Alias(SchedulingController.class, "/health/scheduling", "/medical/scheduling"),
            new Alias(TelemedicineController.class, "/health/telemedicine", "/medical/telemedicine"),
            new Alias(TriageController.class, "/health/triage", "/medical/triage"),
            new Alias(TriageExportController.class, "/health/triage", "/medical/triage"));

    @Test
    void todosLosControladoresDeclaranRutaLegacyYAlias() {
        for (Alias alias : ALIASES) {
            RequestMapping mapping = alias.controller().getAnnotation(RequestMapping.class);
            assertNotNull(mapping, alias.controller().getSimpleName() + " no tiene @RequestMapping");

            List<String> paths = List.of(mapping.value());
            assertTrue(
                    paths.contains(alias.legacy()),
                    alias.controller().getSimpleName() + " debe declarar la ruta legacy " + alias.legacy()
                            + " pero declara " + paths);
            assertTrue(
                    paths.contains(alias.alias()),
                    alias.controller().getSimpleName() + " debe declarar el alias " + alias.alias()
                            + " pero declara " + paths);
        }
    }
}
