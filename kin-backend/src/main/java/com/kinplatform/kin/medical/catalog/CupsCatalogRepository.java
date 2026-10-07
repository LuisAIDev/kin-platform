package com.kinplatform.kin.medical.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CupsCatalogRepository extends JpaRepository<CupsCatalog, UUID> {

    @Query(value = """
        SELECT * FROM cups_catalog
        WHERE is_active = TRUE
          AND (search_vector @@ plainto_tsquery('spanish', :query)
               OR cups_code ILIKE :query || '%'
               OR cups_description ILIKE '%' || :query || '%')
        ORDER BY
            CASE WHEN cups_code ILIKE :query || '%' THEN 1
                 WHEN cups_description ILIKE :query || '%' THEN 2
                 ELSE 3 END,
            similarity(cups_description, :query) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CupsCatalog> searchByFullText(@Param("query") String query, @Param("limit") int limit);

    boolean existsByCupsCode(String cupsCode);
}

