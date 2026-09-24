package com.kinplatform.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InvimaCatalogRepository extends JpaRepository<InvimaCatalog, UUID> {

    @Query(value = """
        SELECT * FROM invima_catalog
        WHERE is_active = TRUE
          AND (search_vector @@ plainto_tsquery('spanish', :query)
               OR cum_code ILIKE :query || '%'
               OR commercial_name ILIKE '%' || :query || '%'
               OR generic_name ILIKE '%' || :query || '%')
        ORDER BY
            CASE WHEN commercial_name ILIKE :query || '%' THEN 1
                 WHEN generic_name ILIKE :query || '%' THEN 2
                 ELSE 3 END,
            similarity(generic_name, :query) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<InvimaCatalog> searchByFullText(@Param("query") String query, @Param("limit") int limit);

    boolean existsByCumCode(String cumCode);
}
