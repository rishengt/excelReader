package com.example.caseprocessor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for mapping Excel headers to CaseRequest fields
 * Supports multiple Excel types with different header structures
 */
@Configuration
@ConfigurationProperties(prefix = "excel.mapping")
@Data
public class ExcelMappingConfig {

    /**
     * Maps Excel type identifier to field mappings
     * Key: Excel type identifier (e.g., "incoming-investigations", "large-cash-balance")
     * Value: Map of Excel header names to CaseRequest field paths
     */
    private Map<String, ExcelTypeMapping> types = new HashMap<>();

    @Data
    public static class ExcelTypeMapping {
        /**
         * Excel title keywords to identify this type (case-insensitive matching)
         */
        private String[] titleKeywords;

        /**
         * Mapping from Excel header names to CaseRequest field paths
         * Field paths use dot notation: e.g., "caseObj.title", "caseObj.primaryParty.eciId"
         */
        private Map<String, String> fieldMappings = new HashMap<>();

        /**
         * Required fields that must be present in the Excel
         */
        private String[] requiredFields = new String[0];
    }

    /**
     * Get mapping configuration for a given Excel type
     */
    public ExcelTypeMapping getMappingForType(String excelType) {
        return types.get(excelType);
    }

    /**
     * Find Excel type by title keywords
     */
    public String findTypeByTitle(String title) {
        if (title == null) {
            return null;
        }
        String titleLower = title.toLowerCase();

        for (Map.Entry<String, ExcelTypeMapping> entry : types.entrySet()) {
            ExcelTypeMapping mapping = entry.getValue();
            if (mapping.getTitleKeywords() != null) {
                for (String keyword : mapping.getTitleKeywords()) {
                    if (titleLower.contains(keyword.toLowerCase())) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }
}
