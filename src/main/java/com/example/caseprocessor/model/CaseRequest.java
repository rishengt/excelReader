package com.example.caseprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO for API request to consumer app
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseRequest {
    private String subject;
    private String client;
    private String caseDescription;
}



