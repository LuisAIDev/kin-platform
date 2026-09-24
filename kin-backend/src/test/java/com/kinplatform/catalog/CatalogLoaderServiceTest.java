package com.kinplatform.catalog;

import com.kinplatform.catalog.loader.CatalogLoaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CatalogLoaderServiceTest {

    private CupsCatalogRepository cupsRepository;
    private InvimaCatalogRepository invimaRepository;
    private CatalogLoaderService service;

    @BeforeEach
    void setUp() {
        cupsRepository = mock(CupsCatalogRepository.class);
        invimaRepository = mock(InvimaCatalogRepository.class);
        when(cupsRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(invimaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        service = new CatalogLoaderService(cupsRepository, invimaRepository);
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "cups.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadCups_fromCsv_loadsAllRecords() throws Exception {
        String content = "cups_code,description,category,group\n"
                + "890201,CONSULTA MEDICINA GENERAL,CONSULTA,Consultas\n"
                + "890301,PROCEDIMIENTO X,PROCEDIMIENTO,\n";

        CatalogLoaderService.LoadResult result = service.loadCupsFromCsv(csv(content));

        assertEquals(2, result.total());
        assertEquals(2, result.loaded());
        assertEquals(0, result.errors());
        verify(cupsRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void loadCups_invalidLines_countAsErrors() throws Exception {
        String content = "cups_code,description,category,group\n"
                + "890201,OK,CONSULTA,\n"
                + "890202,,PROCEDIMIENTO,\n"; // sin descripcion

        CatalogLoaderService.LoadResult result = service.loadCupsFromCsv(csv(content));

        assertEquals(2, result.total());
        assertEquals(1, result.loaded());
        assertEquals(1, result.errors());
    }

    @Test
    void loadInvima_fromCsv_loadsAllRecords() throws Exception {
        String content = "registration,cum_code,commercial_name,generic_name,form,concentration,lab,atc\n"
                + "INVIMA-1,CUM-1,Dolex,acetaminofen,Tableta,500mg,LabX,N02BE01\n";

        CatalogLoaderService.LoadResult result = service.loadInvimaFromCsv(csv(content));

        assertEquals(1, result.total());
        assertEquals(1, result.loaded());
        assertEquals(0, result.errors());
        verify(invimaRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void loadCups_emptyBody_loadsNothing() throws Exception {
        CatalogLoaderService.LoadResult result = service.loadCupsFromCsv(csv("cups_code,description\n"));

        assertEquals(0, result.total());
        assertEquals(List.of().size(), result.loaded());
    }
}
