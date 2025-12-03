package com.example.caseprocessor.service;

import com.example.caseprocessor.model.CaseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for reading and writing Excel files
 */
@Service
@Slf4j
public class ExcelService {

    private static final String SUBJECT_HEADER = "Subject";
    private static final String CLIENT_HEADER = "Client";
    private static final String CASE_DESCRIPTION_HEADER = "Case Description";
    private static final String CASE_NUMBER_HEADER = "Case Number";

    /**
     * Reads Excel file and returns list of CaseData objects
     */
    public List<CaseData> readExcel(String filePath) throws IOException {
        List<CaseData> cases = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Find column indices
            int subjectCol = -1, clientCol = -1, caseDescCol = -1;

            for (Cell cell : headerRow) {
                String cellValue = getCellValueAsString(cell);
                if (SUBJECT_HEADER.equalsIgnoreCase(cellValue)) {
                    subjectCol = cell.getColumnIndex();
                } else if (CLIENT_HEADER.equalsIgnoreCase(cellValue)) {
                    clientCol = cell.getColumnIndex();
                } else if (CASE_DESCRIPTION_HEADER.equalsIgnoreCase(cellValue)) {
                    caseDescCol = cell.getColumnIndex();
                }
            }

            if (subjectCol == -1 || clientCol == -1 || caseDescCol == -1) {
                throw new IllegalArgumentException("Required headers not found in Excel file");
            }

            // Read data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String subject = getCellValueAsString(row.getCell(subjectCol));
                String client = getCellValueAsString(row.getCell(clientCol));
                String caseDescription = getCellValueAsString(row.getCell(caseDescCol));

                // Skip empty rows
                if (subject == null || subject.trim().isEmpty()) {
                    continue;
                }

                CaseData caseData = new CaseData();
                caseData.setSubject(subject);
                caseData.setClient(client);
                caseData.setCaseDescription(caseDescription);
                caseData.setRowIndex(i);

                cases.add(caseData);
            }
        }

        return cases;
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

