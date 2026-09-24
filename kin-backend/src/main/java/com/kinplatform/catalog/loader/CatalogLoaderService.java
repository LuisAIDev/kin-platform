package com.kinplatform.catalog.loader;

import com.kinplatform.catalog.CupsCatalog;
import com.kinplatform.catalog.CupsCatalogRepository;
import com.kinplatform.catalog.InvimaCatalog;
import com.kinplatform.catalog.InvimaCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogLoaderService {

    private static final int BATCH_SIZE = 500;
    private static final String CSV_SPLIT = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    private final CupsCatalogRepository cupsRepository;
    private final InvimaCatalogRepository invimaRepository;

    @Transactional
    public LoadResult loadCupsFromCsv(MultipartFile file) throws IOException {
        int total = 0;
        int loaded = 0;
        int errors = 0;
        List<CupsCatalog> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                total++;
                try {
                    String[] f = line.split(CSV_SPLIT, -1);
                    String code = unquote(f[0]);
                    String description = unquote(f[1]);
                    if (code.isBlank() || description.isBlank()) {
                        errors++;
                        continue;
                    }
                    batch.add(CupsCatalog.builder()
                            .cupsCode(code)
                            .cupsDescription(description)
                            .cupsCategory(f.length > 2 && !unquote(f[2]).isBlank() ? unquote(f[2]) : "OTRO")
                            .cupsGroup(f.length > 3 ? blankToNull(unquote(f[3])) : null)
                            .build());
                    if (batch.size() >= BATCH_SIZE) {
                        loaded += flushCups(batch);
                        log.info("CUPS cargados: {}", loaded);
                    }
                } catch (Exception e) {
                    errors++;
                    log.warn("CUPS linea invalida: {}", e.getMessage());
                }
            }
            loaded += flushCups(batch);
        }
        log.info("CUPS carga completa: total={} cargados={} errores={}", total, loaded, errors);
        return new LoadResult(total, loaded, errors);
    }

    @Transactional
    public LoadResult loadInvimaFromCsv(MultipartFile file) throws IOException {
        int total = 0;
        int loaded = 0;
        int errors = 0;
        List<InvimaCatalog> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                total++;
                try {
                    String[] f = line.split(CSV_SPLIT, -1);
                    String registration = unquote(f[0]);
                    String commercial = unquote(f[1]);
                    if (registration.isBlank() || commercial.isBlank()) {
                        errors++;
                        continue;
                    }
                    batch.add(InvimaCatalog.builder()
                            .invimaRegistration(registration)
                            .cumCode(f.length > 2 ? blankToNull(unquote(f[2])) : null)
                            .commercialName(commercial)
                            .genericName(f.length > 3 ? blankToNull(unquote(f[3])) : null)
                            .pharmaceuticalForm(f.length > 4 ? blankToNull(unquote(f[4])) : null)
                            .concentration(f.length > 5 ? blankToNull(unquote(f[5])) : null)
                            .laboratory(f.length > 6 ? blankToNull(unquote(f[6])) : null)
                            .atcCode(f.length > 7 ? blankToNull(unquote(f[7])) : null)
                            .build());
                    if (batch.size() >= BATCH_SIZE) {
                        loaded += flushInvima(batch);
                        log.info("INVIMA cargados: {}", loaded);
                    }
                } catch (Exception e) {
                    errors++;
                    log.warn("INVIMA linea invalida: {}", e.getMessage());
                }
            }
            loaded += flushInvima(batch);
        }
        log.info("INVIMA carga completa: total={} cargados={} errores={}", total, loaded, errors);
        return new LoadResult(total, loaded, errors);
    }

    private int flushCups(List<CupsCatalog> batch) {
        if (batch.isEmpty()) return 0;
        int size = batch.size();
        cupsRepository.saveAll(batch);
        batch.clear();
        return size;
    }

    private int flushInvima(List<InvimaCatalog> batch) {
        if (batch.isEmpty()) return 0;
        int size = batch.size();
        invimaRepository.saveAll(batch);
        batch.clear();
        return size;
    }

    private static String unquote(String value) {
        String v = value == null ? "" : value.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1).replace("\"\"", "\"");
        }
        return v;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record LoadResult(int total, int loaded, int errors) {
    }
}
