package com.kinplatform.kin.medical.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogSearchServiceTest {

    @Mock
    private CupsCatalogRepository cupsRepository;

    @Mock
    private InvimaCatalogRepository invimaRepository;

    @InjectMocks
    private CatalogSearchService service;

    @Test
    void searchCups_withValidQuery_returnsResults() {
        when(cupsRepository.searchByFullText("consulta", 20)).thenReturn(List.of(
                CupsCatalog.builder().cupsCode("890201").cupsDescription("CONSULTA MEDICINA GENERAL")
                        .cupsCategory("CONSULTA").build()));

        List<CupsSuggestion> result = service.searchCups("Consulta", 20);

        assertEquals(1, result.size());
        assertEquals("890201", result.get(0).cupsCode());
    }

    @Test
    void searchCups_withShortQuery_returnsEmpty() {
        assertTrue(service.searchCups("a", 20).isEmpty());
        assertTrue(service.searchCups("", 20).isEmpty());
        verifyNoInteractions(cupsRepository);
    }

    @Test
    void searchCups_capsLimitAt50() {
        when(cupsRepository.searchByFullText(eq("consulta"), eq(50))).thenReturn(List.of());

        service.searchCups("consulta", 100);

        verify(cupsRepository).searchByFullText("consulta", 50);
    }

    @Test
    void searchInvima_byGenericName_returnsResults() {
        when(invimaRepository.searchByFullText("acetaminofen", 20)).thenReturn(List.of(
                InvimaCatalog.builder().cumCode("CUM-1").commercialName("Dolex")
                        .genericName("acetaminofen").build()));

        List<InvimaSuggestion> result = service.searchInvima("Acetaminofen", 20);

        assertEquals(1, result.size());
        assertEquals("acetaminofen", result.get(0).genericName());
    }

    @Test
    void searchInvima_withShortQuery_returnsEmpty() {
        assertTrue(service.searchInvima("x", 20).isEmpty());
        verifyNoInteractions(invimaRepository);
    }
}

