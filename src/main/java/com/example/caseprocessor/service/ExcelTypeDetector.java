package com.example.caseprocessor.service;

import com.example.caseprocessor.config.ExcelMappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

/**
 * Service to detect Excel file type based on title or other identifiers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelTypeDetector {

    private final ExcelMappingConfig mappingConfig;

    /**
     * Detects Excel type by checking the title in the first few rows
     * Looks for title in cells A1, A2, A3, or sheet name
     */
    public String detectExcelType(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);

        // Check sheet name first
        String sheetName = sheet.getSheetName();
        String detectedType = mappingConfig.findTypeByTitle(sheetName);
        if (detectedType != null) {
            log.info("Detected Excel type '{}' from sheet name: {}", detectedType, sheetName);
            return detectedType;
        }

        // Check first few rows for title
        for (int rowIndex = 0; rowIndex < Math.min(5, sheet.getLastRowNum() + 1); rowIndex++) {
            if (sheet.getRow(rowIndex) != null && sheet.getRow(rowIndex).getCell(0) != null) {
                String cellValue = getCellValueAsString(sheet.getRow(rowIndex).getCell(0));
                if (cellValue != null && !cellValue.trim().isEmpty()) {
                    detectedType = mappingConfig.findTypeByTitle(cellValue);
                    if (detectedType != null) {
                        log.info("Detected Excel type '{}' from cell A{}: {}", detectedType, rowIndex + 1, cellValue);
                        return detectedType;
                    }
                }
            }
        }

        log.warn("Could not detect Excel type. Using default mapping.");
        return "default";
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
