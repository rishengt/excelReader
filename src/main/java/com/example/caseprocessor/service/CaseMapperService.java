package com.example.caseprocessor.service;

import com.example.caseprocessor.config.ExcelMappingConfig;
import com.example.caseprocessor.model.CaseRequest;
import com.example.caseprocessor.model.ExcelRowData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Service to map Excel row data to CaseRequest using dynamic field mappings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseMapperService {

    private final ExcelMappingConfig mappingConfig;

    /**
     * Maps ExcelRowData to CaseRequest based on the Excel type mapping configuration
     */
    public CaseRequest mapToCaseRequest(ExcelRowData rowData) {
        String excelType = rowData.getExcelType();
        ExcelMappingConfig.ExcelTypeMapping mapping = mappingConfig.getMappingForType(excelType);

        if (mapping == null) {
            log.warn("No mapping found for Excel type: {}. Using default mapping.", excelType);
            mapping = mappingConfig.getMappingForType("default");
            if (mapping == null) {
                throw new IllegalArgumentException("No mapping configuration found for type: " + excelType);
            }
        }

        CaseRequest request = new CaseRequest();
        CaseRequest.CaseObject caseObj = new CaseRequest.CaseObject();
        CaseRequest.PrimaryParty primaryParty = new CaseRequest.PrimaryParty();
        caseObj.setPrimaryParty(primaryParty);
        request.setCaseObj(caseObj);

        // Apply field mappings
        for (Map.Entry<String, String> entry : mapping.getFieldMappings().entrySet()) {
            String excelHeader = entry.getKey();
            String fieldPath = entry.getValue();
            String value = rowData.getValue(excelHeader);

            if (value != null && !value.trim().isEmpty()) {
                setFieldValue(request, fieldPath, value);
            }
        }

        // Validate required fields
        validateRequiredFields(rowData, mapping);

        return request;
    }

    /**
     * Sets a field value using dot notation path (e.g., "caseObj.title", "caseObj.primaryParty.eciId")
     */
    private void setFieldValue(Object target, String fieldPath, String value) {
        try {
            String[] parts = fieldPath.split("\\.");
            Object current = target;

            // Navigate to the parent object
            for (int i = 0; i < parts.length - 1; i++) {
                Field field = getField(current.getClass(), parts[i]);
                if (field == null) {
                    log.warn("Field not found: {} in {}", parts[i], current.getClass().getSimpleName());
                    return;
                }
                field.setAccessible(true);
                Object nested = field.get(current);
                if (nested == null) {
                    // Create nested object if it doesn't exist
                    nested = field.getType().getDeclaredConstructor().newInstance();
                    field.set(current, nested);
                }
                current = nested;
            }

            // Set the final field value
            String finalFieldName = parts[parts.length - 1];
            Field finalField = getField(current.getClass(), finalFieldName);
            if (finalField != null) {
                finalField.setAccessible(true);
                setFieldValueByType(finalField, current, value);
            } else {
                log.warn("Field not found: {} in {}", finalFieldName, current.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Error setting field value for path {}: {}", fieldPath, e.getMessage());
        }
    }

    /**
     * Gets a field from a class (including inherited fields)
     */
    private Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // Check parent class
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getField(superClass, fieldName);
            }
            return null;
        }
    }

    /**
     * Sets field value with appropriate type conversion
     */
    private void setFieldValueByType(Field field, Object target, String value) throws Exception {
        Class<?> fieldType = field.getType();

        if (fieldType == String.class) {
            field.set(target, value);
        } else if (fieldType == Integer.class || fieldType == int.class) {
            try {
                field.set(target, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Cannot convert '{}' to Integer for field {}", value, field.getName());
            }
        } else if (fieldType == Long.class || fieldType == long.class) {
            try {
                field.set(target, Long.parseLong(value));
            } catch (NumberFormatException e) {
                log.warn("Cannot convert '{}' to Long for field {}", value, field.getName());
            }
        } else if (fieldType == Double.class || fieldType == double.class) {
            try {
                field.set(target, Double.parseDouble(value));
            } catch (NumberFormatException e) {
                log.warn("Cannot convert '{}' to Double for field {}", value, field.getName());
            }
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            field.set(target, Boolean.parseBoolean(value));
        } else {
            // Default to string
            field.set(target, value);
        }
    }

    /**
     * Validates that all required fields are present in the Excel row
     */
    private void validateRequiredFields(ExcelRowData rowData, ExcelMappingConfig.ExcelTypeMapping mapping) {
        if (mapping.getRequiredFields() != null) {
            for (String requiredField : mapping.getRequiredFields()) {
                String value = rowData.getValue(requiredField);
                if (value == null || value.trim().isEmpty()) {
                    log.warn("Required field '{}' is missing or empty in row {}", requiredField, rowData.getRowIndex());
                }
            }
        }
    }
}
