package com.example.caseprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO representing case data from Excel and for API requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseData {
    private String subject;
    private String client;
    private String caseDescription;
    private String caseNumber; // Will be populated after API call
    private int rowIndex; // To track which row in Excel for updating
}



