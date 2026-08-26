package com.kinplatform.kin.health.common;

import com.kinplatform.kin.health.dashboard.config.DashboardProperties;
import com.kinplatform.kin.health.differential.config.DifferentialProperties;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.telemedicine.config.TelemedicineProperties;
import com.kinplatform.kin.health.triage.config.TriageProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Healthcheck específico de los módulos de KIN Health (fase de producción).
 *
 * <p>Exposa en {@code /actuator/health} el estado de cada módulo de salud
 * (triaje, diagnóstico diferencial, dashboard, portal médico y telemedicina)
 * con sus feature flags. UP si el módulo está habilitado; UP también si está
 * deshabilitado por configuración (degradación intencionada), para no marcar el
 * agregado como DOWN.</p>
 */
@Component
public class HealthModuleHealthIndicator implements HealthIndicator {

    private final TriageProperties triage;
    private final DifferentialProperties differential;
    private final DashboardProperties dashboard;
    private final PhysicianProperties physician;
    private final TelemedicineProperties telemedicine;

    public HealthModuleHealthIndicator(
            TriageProperties triage,
            DifferentialProperties differential,
            DashboardProperties dashboard,
            PhysicianProperties physician,
            TelemedicineProperties telemedicine) {
        this.triage = triage;
        this.differential = differential;
        this.dashboard = dashboard;
        this.physician = physician;
        this.telemedicine = telemedicine;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("triage.enabled", triage.isEnabled())
                .withDetail("differential.enabled", differential.isEnabled())
                .withDetail("dashboard.enabled", dashboard.isEnabled())
                .withDetail("physician.enabled", physician.isEnabled())
                .withDetail("telemedicine.enabled", telemedicine.isEnabled())
                .withDetail("catalog.cacheEnabled", triage.isCatalogCacheEnabled())
                .build();
    }
}
