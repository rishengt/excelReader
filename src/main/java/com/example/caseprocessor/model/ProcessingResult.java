package com.example.caseprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of case processing operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {
    private boolean success;
    private String message;
    private int totalCases;
    private long successfulCases;
    private long errorCases;
}