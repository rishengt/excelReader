package com.example.caseprocessor.service;

import com.example.caseprocessor.model.CaseData;
import com.example.caseprocessor.model.ExcelRowData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic service for reading and writing Excel files
 * Returns flexible ExcelRowData that can handle any header structure
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelService {

    private static final String CASE_NUMBER_HEADER = "Case Number";
    private final ExcelTypeDetector typeDetector;

    /**
     * Reads Excel file and returns list of ExcelRowData objects
     * Automatically detects Excel type and includes all headers dynamically
     */
    public List<ExcelRowData> readExcel(String filePath) throws IOException {
        List<ExcelRowData> rows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Detect Excel type
            String excelType = typeDetector.detectExcelType(workbook);
            log.info("Detected Excel type: {}", excelType);

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file has no header row");
            }

            // Build header map: column index -> header name
            Map<Integer, String> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String headerName = getCellValueAsString(cell);
                if (headerName != null && !headerName.trim().isEmpty()) {
                    headerMap.put(cell.getColumnIndex(), headerName.trim());
                }
            }

            if (headerMap.isEmpty()) {
                throw new IllegalArgumentException("No headers found in Excel file");
            }

            log.info("Found {} headers: {}", headerMap.size(), headerMap.values());

            // Read data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ExcelRowData rowData = new ExcelRowData();
                rowData.setExcelType(excelType);
                rowData.setRowIndex(i);

                boolean hasData = false;
                // Read all columns based on header map
                for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
                    int colIndex = entry.getKey();
                    String headerName = entry.getValue();
                    String cellValue = getCellValueAsString(row.getCell(colIndex));

                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        rowData.setValue(headerName, cellValue.trim());
                        hasData = true;
                    } else {
                        rowData.setValue(headerName, "");
                    }
                }

                // Skip completely empty rows
                if (hasData) {
                    rows.add(rowData);
                }
            }
        }

        log.info("Read {} data rows from Excel file", rows.size());
        return rows;
    }

    /**
     * Updates Excel file with case numbers
     */
    public void updateExcelWithCaseNumbers(String filePath, List<CaseData> cases) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Check if Case Number column exists, if not create it
            int caseNumberCol = findColumnIndex(headerRow, CASE_NUMBER_HEADER);
            if (caseNumberCol == -1) {
                caseNumberCol = headerRow.getLastCellNum();
                Cell newHeaderCell = headerRow.createCell(caseNumberCol);
                newHeaderCell.setCellValue(CASE_NUMBER_HEADER);

                // Style the new header
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                newHeaderCell.setCellStyle(headerStyle);
            }

            // Update case numbers
            for (CaseData caseData : cases) {
                if (caseData.getCaseNumber() != null) {
                    Row row = sheet.getRow(caseData.getRowIndex());
                    if (row != null) {
                        Cell cell = row.getCell(caseNumberCol);
                        if (cell == null) {
                            cell = row.createCell(caseNumberCol);
                        }
                        cell.setCellValue(caseData.getCaseNumber());
                    }
                }
            }

            // Write back to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    private int findColumnIndex(Row headerRow, String headerName) {
        for (Cell cell : headerRow) {
            if (headerName.equalsIgnoreCase(getCellValueAsString(cell))) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Remove decimal if it's a whole number
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

