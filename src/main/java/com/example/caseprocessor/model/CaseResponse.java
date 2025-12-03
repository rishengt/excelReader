package com.example.caseprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO for API response from consumer app
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseResponse {
    private String caseNumber;
}



