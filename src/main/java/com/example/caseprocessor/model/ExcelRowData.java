package com.example.caseprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic data structure to hold Excel row data as key-value pairs
 * Key is the header name, value is the cell value
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelRowData {
    private Map<String, String> data = new HashMap<>();
    private int rowIndex;
    private String caseNumber; // Will be populated after API call
    private String excelType; // Type of Excel file (e.g., "Incoming Investigations Data")

    public String getValue(String headerName) {
        return data.getOrDefault(headerName, "");
    }

    public void setValue(String headerName, String value) {
        data.put(headerName, value);
    }
}