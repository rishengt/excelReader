# Excel Dynamic Mapping Guide

This project now supports dynamic Excel file processing with configurable field mappings. You can process different Excel file types with different header structures without changing code.

## Architecture Overview

The system consists of several key components:

1. **ExcelService**: Generic Excel reader that extracts all headers and data as key-value pairs
2. **ExcelTypeDetector**: Automatically detects Excel type based on title/keywords
3. **ExcelMappingConfig**: Configuration-based mapping system (defined in `application.yml`)
4. **CaseMapperService**: Dynamically maps Excel data to `CaseRequest` using reflection
5. **ExcelRowData**: Flexible data structure that holds any Excel row as a Map

## How It Works

1. **Excel Detection**: When an Excel file is read, the system checks the sheet name and first few rows for title keywords to identify the Excel type.

2. **Dynamic Reading**: All headers are read dynamically, and each row is stored as a `Map<String, String>` where keys are header names.

3. **Field Mapping**: Based on the detected Excel type, the system uses the configured mappings in `application.yml` to map Excel headers to `CaseRequest` field paths.

4. **Case Creation**: The mapped `CaseRequest` is sent to the API to create cases.

## Configuration

### Adding a New Excel Type

To support a new Excel file type, add a new entry in `application.yml`:

```yaml
excel:
  mapping:
    types:
      your-excel-type:
        title-keywords:
          - "Your Excel Title"
          - "Alternative Title"
        field-mappings:
          "Excel Header Name": "caseObj.fieldName"
          "Another Header": "caseObj.primaryParty.eciId"
        required-fields:
          - "Excel Header Name"
```

### Field Path Syntax

Field paths use dot notation to navigate nested objects:
- `caseObj.title` → Sets `CaseRequest.caseObj.title`
- `caseObj.primaryParty.eciId` → Sets `CaseRequest.caseObj.primaryParty.eciId`
- `caseObj.team` → Sets `CaseRequest.caseObj.team`

### Example Mappings

#### Incoming Investigations Data
- Headers: Subject, Description, Stage, Type, Client Name, Teams, Client Due Date
- Maps to: `caseObj.title`, `caseObj.description`, `caseObj.status`, etc.

#### Large Cash Balance
- Headers: Subject, Description, Stage, Type, Priority, ECI, CST Name, Client Due Date, Assigned User Name
- Maps to: `caseObj.title`, `caseObj.priority`, `caseObj.primaryParty.eciId`, etc.

#### Standing Exceptions Report
- Headers: ACCT TYPE, DEBIT ACCOUNT, FREQ, MONTH, DAY, LNECODE, TX REMIT, TITLE, PAYEE, NAME, etc.
- Maps to: Various `caseObj` fields and `caseObj.primaryParty.name`

## Usage

The API endpoint remains the same:

```bash
POST /api/cases/process?filePath=path/to/your/excel.xlsx
```

The system will:
1. Automatically detect the Excel type
2. Read all headers dynamically
3. Apply the appropriate field mappings
4. Create cases via the API
5. Write case numbers back to the Excel file

## Benefits

- **Decoupled**: Excel structure changes don't require code changes
- **Flexible**: Supports any Excel header structure
- **Configurable**: All mappings defined in YAML
- **Type-Safe**: Uses reflection with proper error handling
- **Extensible**: Easy to add new Excel types

## Adding Fields to CaseRequest

If you need to map to new fields in `CaseRequest`, simply:
1. Add the field to `CaseRequest.CaseObject` (or nested class)
2. Add the mapping in `application.yml`
3. No code changes needed in services!
