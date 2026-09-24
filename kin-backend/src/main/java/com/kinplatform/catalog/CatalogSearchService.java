package com.kinplatform.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogSearchService {

    private final CupsCatalogRepository cupsRepository;
    private final InvimaCatalogRepository invimaRepository;

    public List<CupsSuggestion> searchCups(String query, int limit) {
        String q = normalize(query);
        if (q.length() < 2) {
            return List.of();
        }
        return cupsRepository.searchByFullText(q, cap(limit)).stream()
                .map(c -> new CupsSuggestion(c.getCupsCode(), c.getCupsDescription(), c.getCupsCategory()))
                .toList();
    }

    public List<InvimaSuggestion> searchInvima(String query, int limit) {
        String q = normalize(query);
        if (q.length() < 2) {
            return List.of();
        }
        return invimaRepository.searchByFullText(q, cap(limit)).stream()
                .map(m -> new InvimaSuggestion(
                        m.getCumCode(), m.getCommercialName(), m.getGenericName(),
                        m.getPharmaceuticalForm(), m.getConcentration()))
                .toList();
    }

    private int cap(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }
}
