package com.kinplatform.billing.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter @NoArgsConstructor @AllArgsConstructor
public class TariffImportResult {
    private int created;
    private int updated;
    private int errors;
    private List<String> errorMessages;
}