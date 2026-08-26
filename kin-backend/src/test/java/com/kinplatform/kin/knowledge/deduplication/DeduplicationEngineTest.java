package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeduplicationEngineTest {

    private final DeduplicationEngine engine = new DeduplicationEngine(List.of(
            new ExactMatchStrategy(),
            new FuzzyMatchStrategy(0.85),
            new SemanticMatchStrategy(0.90)
    ));

    @Test
    void exactMatch_removesExactDuplicates() {
        UUID projectId = UUID.randomUUID();
        UUID projectId2 = UUID.randomUUID();

        KnowledgeFact f1 = KnowledgeFact.of("PIB Colombia 2023: 350B USD", "worldbank", "https://wb.org", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");
        KnowledgeFact f2 = KnowledgeFact.of("PIB Colombia 2023: 350B USD", "worldbank", "https://wb.org", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");
        KnowledgeFact f3 = KnowledgeFact.of("Inflación Colombia 2023: 11.5%", "banrep", "https://banrep.gov.co", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");

        var input = DeduplicationInput.of(projectId, List.of(f1, f2, f3));
        var result = engine.evaluate(input);

        assertThat(result.uniqueFacts()).hasSize(2);
        assertThat(result.metrics().duplicatesRemoved()).isEqualTo(1);
        assertThat(result.duplicateGroups()).hasSize(1);
        assertThat(result.duplicateGroups().keySet()).anyMatch(f -> f.claim().contains("PIB"));
    }

    @Test
    void fuzzyMatch_removesSimilarText() {
        UUID projectId = UUID.randomUUID();

        // Claims casi idénticos - misma fuente
        KnowledgeFact f1 = KnowledgeFact.of("PIB Colombia 2023 350 billones USD", "worldbank", "https://wb.org", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");
        KnowledgeFact f2 = KnowledgeFact.of("PIB Colombia 2023 350 billones USD", "worldbank", "https://banrep.gov.co", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");

        var input = DeduplicationInput.of(projectId, List.of(f1, f2), DeduplicationPolicy.EXACT_THEN_FUZZY, 0.6);
        var result = engine.evaluate(input);

        assertThat(result.uniqueFacts()).hasSize(1);
        assertThat(result.metrics().duplicatesRemoved()).isEqualTo(1);
    }

@Test
    void exactAndFuzzyCombined() {
        UUID projectId = UUID.randomUUID();

        // Exact duplicate - same source
        KnowledgeFact f1 = KnowledgeFact.of("PIB Colombia 350B", "worldbank", "url1", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");
        KnowledgeFact f2 = KnowledgeFact.of("PIB Colombia 350B", "worldbank", "url2", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");

        // Fuzzy duplicate - same source
        KnowledgeFact f3 = KnowledgeFact.of("Tasa de interés Colombia 2023: 13.25%", "worldbank", "url3", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");
        KnowledgeFact f4 = KnowledgeFact.of("Tasa de interés Colombia 2023: 13.25%", "worldbank", "url4", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");

        // Unique
        KnowledgeFact f5 = KnowledgeFact.of("Inflación 11.5%", "banrep", "url5", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");

        var input = DeduplicationInput.of(projectId, List.of(f1, f2, f3, f4, f5), DeduplicationPolicy.EXACT_THEN_FUZZY, 0.6);
        var result = engine.evaluate(input);

        assertThat(result.uniqueFacts()).hasSize(3); // f1/f2 merged, f3/f4 merged, f5 unique
        assertThat(result.metrics().duplicatesRemoved()).isEqualTo(2);
    }

    @Test
    void winnerSelection_byTrust() {
        UUID projectId = UUID.randomUUID();

        KnowledgeFact f1 = new KnowledgeFact(
                UUID.randomUUID(),
                "PIB Colombia 350B",
                "worldbank",
                "url1",
                OffsetDateTime.now(),
                SourceTrust.OFFICIAL_PUBLIC,
                "ECONOMIA",
                null
        );
        KnowledgeFact f2 = new KnowledgeFact(
                UUID.randomUUID(),
                "PIB Colombia 350B",
                "worldbank",
                "url2",
                OffsetDateTime.now(),
                SourceTrust.SECONDARY,
                "ECONOMIA",
                null
        );

        var input = DeduplicationInput.of(UUID.randomUUID(), List.of(f1, f2));
        var result = engine.evaluate(input);

        // OFFICIAL_PUBLIC should win over SECONDARY
        assertThat(result.uniqueFacts()).hasSize(1);
        assertThat(result.uniqueFacts().get(0).trust()).isEqualTo(SourceTrust.OFFICIAL_PUBLIC);
    }

    @Test
    void noDuplicates_returnsAll() {
        UUID projectId = UUID.randomUUID();

        KnowledgeFact f1 = KnowledgeFact.of("PIB Colombia 350B", "worldbank", "url1", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");
        KnowledgeFact f2 = KnowledgeFact.of("Inflación 11.5%", "banrep", "url2", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");

        var input = DeduplicationInput.of(UUID.randomUUID(), List.of(f1, f2));
        var result = engine.evaluate(input);

        assertThat(result.uniqueFacts()).hasSize(2);
        assertThat(result.metrics().duplicatesRemoved()).isEqualTo(0);
    }

    @Test
    void disabledOutbox_returnsAll() {
        UUID projectId = UUID.randomUUID();

        KnowledgeFact f1 = KnowledgeFact.of("PIB Colombia 350B", "worldbank", "url1", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");
        KnowledgeFact f2 = KnowledgeFact.of("PIB Colombia 350B", "worldbank", "url2", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "ECONOMIA");

        var input = DeduplicationInput.of(UUID.randomUUID(), List.of(f1, f2));
        // Use disabled engine
        var disabledEngine = new DeduplicationEngine(List.of()); // No strategies = disabled
        var result = disabledEngine.evaluate(input);

        assertThat(result.uniqueFacts()).hasSize(2);
    }
}