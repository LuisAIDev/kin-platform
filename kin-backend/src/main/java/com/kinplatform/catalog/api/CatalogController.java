package com.kinplatform.catalog.api;

import com.kinplatform.catalog.CatalogSearchService;
import com.kinplatform.catalog.CupsSuggestion;
import com.kinplatform.catalog.InvimaSuggestion;
import com.kinplatform.catalog.loader.CatalogLoaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private static final String SEARCH_ROLES =
            "hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'IPS_FACTURADOR', 'IPS_MEDICO', 'ADMIN')";

    private final CatalogSearchService searchService;
    private final CatalogLoaderService loaderService;

    @GetMapping("/cups/search")
    @PreAuthorize(SEARCH_ROLES)
    public ResponseEntity<List<CupsSuggestion>> searchCups(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.searchCups(q, limit));
    }

    @GetMapping("/invima/search")
    @PreAuthorize(SEARCH_ROLES)
    public ResponseEntity<List<InvimaSuggestion>> searchInvima(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.searchInvima(q, limit));
    }

    @PostMapping(value = "/cups/load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogLoaderService.LoadResult> loadCups(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(loaderService.loadCupsFromCsv(file));
    }

    @PostMapping(value = "/invima/load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogLoaderService.LoadResult> loadInvima(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(loaderService.loadInvimaFromCsv(file));
    }
}
