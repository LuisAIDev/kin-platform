package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.ai.knowledge.adapter.KnowledgeMetricsSummaryService.KnowledgeMetricsSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de resumen operativo del Knowledge Engine (ADR-025, Fase 1):
 * {@code GET /api/v1/knowledge/metrics} — un solo curl, sin parsear Prometheus.
 * Solo ADMIN (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeMetricsController {

    private final KnowledgeMetricsSummaryService summaryService;

    public KnowledgeMetricsController(KnowledgeMetricsSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/metrics")
    public KnowledgeMetricsSummary metrics() {
        return summaryService.summarize();
    }
}
