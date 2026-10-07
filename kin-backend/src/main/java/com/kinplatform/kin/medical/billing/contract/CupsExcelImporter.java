package com.kinplatform.kin.medical.billing.contract;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
@Slf4j
public class CupsExcelImporter {

    private static final Map<String, Integer> COLUMN_MAP = Map.of(
            "CUPS_CODE", 0, "DESCRIPTION", 1, "UNIT_PRICE_COPS", 2,
            "CUPS_CATEGORY", 3, "REQUIRES_AUTH", 4, "AUTH_VALIDITY_DAYS", 5,
            "EFFECTIVE_FROM", 6, "EFFECTIVE_TO", 7
    );

    public List<TariffCups> parse(MultipartFile file) throws IOException {
        List<TariffCups> tariffs = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(sheet.getRow(0));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                TariffCups tariff = parseRow(row);
                if (tariff != null) tariffs.add(tariff);
            }
        }
        return tariffs;
    }

    private void validateHeaders(Row headerRow) {
        String cupsCodeHeader = getCellString(headerRow, COLUMN_MAP.get("CUPS_CODE"));
        if (!"CUPS_CODE".equalsIgnoreCase(cupsCodeHeader.trim())) {
            throw new IllegalArgumentException("Invalid Excel format: first column must be CUPS_CODE");
        }
    }

    private TariffCups parseRow(Row row) {
        try {
            String cupsCode = getCellString(row, COLUMN_MAP.get("CUPS_CODE")).trim().toUpperCase();
            if (cupsCode.isEmpty()) return null;

            String description = getCellString(row, COLUMN_MAP.get("DESCRIPTION"));
            BigDecimal price = getCellBigDecimal(row, COLUMN_MAP.get("UNIT_PRICE_COPS"));
            String categoryStr = getCellString(row, COLUMN_MAP.get("CUPS_CATEGORY")).trim().toUpperCase();
            Boolean requiresAuth = getCellBoolean(row, COLUMN_MAP.get("REQUIRES_AUTH"));
            Integer authDays = getCellInteger(row, COLUMN_MAP.get("AUTH_VALIDITY_DAYS"));
            LocalDate effectiveFrom = getCellDate(row, COLUMN_MAP.get("EFFECTIVE_FROM"));
            LocalDate effectiveTo = getCellDate(row, COLUMN_MAP.get("EFFECTIVE_TO"));

            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Invalid price for CUPS " + cupsCode);
            }
            if (effectiveFrom == null) effectiveFrom = LocalDate.now();

            return TariffCups.builder()
                    .cupsCode(cupsCode)
                    .cupsVersion("2024")
                    .description(description)
                    .unitPriceCop(price)
                    .requiresAuth(requiresAuth != null && requiresAuth)
                    .authValidityDays(authDays)
                    .cupsCategory(parseCategory(categoryStr))
                    .effectiveFrom(effectiveFrom)
                    .effectiveTo(effectiveTo)
                    .build();
        } catch (Exception e) {
            log.warn("Error parsing row: {}", e.getMessage());
            throw new IllegalArgumentException("Row parse error: " + e.getMessage());
        }
    }

    private TariffCups.CupsCategory parseCategory(String str) {
        try { return TariffCups.CupsCategory.valueOf(str); }
        catch (Exception e) { return TariffCups.CupsCategory.OTRO; }
    }

    private String getCellString(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private BigDecimal getCellBigDecimal(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        try { return new BigDecimal(getCellString(row, colIdx)); }
        catch (Exception e) { return null; }
    }

    private Boolean getCellBoolean(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return false;
        if (cell.getCellType() == CellType.BOOLEAN) return cell.getBooleanCellValue();
        String val = getCellString(row, colIdx).trim().toLowerCase();
        return val.equals("true") || val.equals("1") || val.equals("si") || val.equals("yes");
    }

    private Integer getCellInteger(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(getCellString(row, colIdx)); }
        catch (Exception e) { return null; }
    }

    private LocalDate getCellDate(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String str = getCellString(row, colIdx).trim();
        if (str.isEmpty()) return null;
        try { return LocalDate.parse(str); }
        catch (Exception e) { return null; }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < 3; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellString(row, i).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
