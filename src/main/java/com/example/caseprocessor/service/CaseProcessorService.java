package com.example.caseprocessor.service;

import com.example.caseprocessor.model.CaseRequest;
import com.example.caseprocessor.model.CaseResponse;
import com.example.caseprocessor.model.ExcelRowData;
import com.example.caseprocessor.model.ProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service for processing cases concurrently using reactive programming
 * Now supports dynamic Excel mapping for different Excel types
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseProcessorService {

    private final CaseApiService caseApiService;
    private final ExcelService excelService;
    private final CaseMapperService caseMapperService;

    @Value("${case.processing.concurrent-threads:5}")
    private int concurrentThreads;

    @Value("${case.excel.default-path:cases.xlsx}")
    private String defaultExcelPath;

    /**
     * Processes cases from Excel file using reactive programming
     * Processes cases in batches (5 at a time by default) using Flux
     * Automatically detects Excel type and applies appropriate field mappings
     *
     * @param filePath Path to Excel file (uses default if null)
     * @return Mono of ProcessingResult
     */
    public Mono<ProcessingResult> processCases(String filePath) {
        String excelPath = filePath != null ? filePath : defaultExcelPath;

        return Mono.fromCallable(() -> {
                    log.info("Reading Excel file: {}", excelPath);
                    return excelService.readExcel(excelPath);
                })
                .flatMapMany(rows -> {
                    log.info("Found {} rows to process with concurrency of {}", rows.size(), concurrentThreads);
                    if (rows.isEmpty()) {
                        return Flux.empty();
                    }

                    // Convert to Flux and process with concurrency control
                    // flatMap with concurrency parameter processes up to N items concurrently
                    return Flux.fromIterable(rows)
                            .flatMap(rowData -> {
                                try {
                                    // Dynamically map Excel row data to CaseRequest
                                    CaseRequest request = caseMapperService.mapToCaseRequest(rowData);

                                    // Process case and update with response
                                    return caseApiService.createCase(request)
                                            .doOnNext(response -> {
                                                if (response != null && response.getCaseNumber() != null
                                                        && !response.getCaseNumber().equals("ERROR")) {
                                                    rowData.setCaseNumber(response.getCaseNumber());
                                                    log.debug("Case created for row {}: {}",
                                                            rowData.getRowIndex(), response.getCaseNumber());
                                                } else {
                                                    rowData.setCaseNumber("ERROR");
                                                    log.error("Failed to create case for row {}", rowData.getRowIndex());
                                                }
                                            })
                                            .doOnError(error -> {
                                                rowData.setCaseNumber("ERROR");
                                                log.error("Exception processing row {}: {}",
                                                        rowData.getRowIndex(), error.getMessage());
                                            })
                                            .onErrorReturn(new CaseResponse("ERROR"))
                                            .thenReturn(rowData);
                                } catch (Exception e) {
                                    log.error("Error mapping row {} to CaseRequest: {}", 
                                            rowData.getRowIndex(), e.getMessage());
                                    rowData.setCaseNumber("ERROR");
                                    return Mono.just(rowData);
                                }
                            }, concurrentThreads) // Process up to concurrentThreads at a time
                            .buffer(concurrentThreads) // Group into batches for progress logging
                            .doOnNext(batch -> {
                                if (!batch.isEmpty()) {
                                    log.info("Completed batch of {} cases (processing up to {} concurrently)",
                                            batch.size(), concurrentThreads);
                                }
                            })
                            .flatMap(Flux::fromIterable); // Flatten back to individual cases
                })
                .collectList()
                .flatMap(processedRows -> {
                    // Update Excel with case numbers
                    log.info("Updating Excel file with case numbers...");
                    try {
                        excelService.updateExcelWithCaseNumbers(excelPath, processedRows);

                        // Calculate summary
                        long successCount = processedRows.stream()
                                .filter(r -> r.getCaseNumber() != null && !r.getCaseNumber().equals("ERROR"))
                                .count();
                        long errorCount = processedRows.size() - successCount;

                        ProcessingResult result = new ProcessingResult();
                        result.setSuccess(true);
                        result.setMessage("Processing completed successfully");
                        result.setTotalCases(processedRows.size());
                        result.setSuccessfulCases(successCount);
                        result.setErrorCases(errorCount);

                        log.info("Processing complete! Total: {}, Successful: {}, Errors: {}",
                                processedRows.size(), successCount, errorCount);

                        return Mono.just(result);
                    } catch (Exception e) {
                        log.error("Error updating Excel file: {}", e.getMessage());
                        ProcessingResult result = new ProcessingResult();
                        result.setSuccess(false);
                        result.setMessage("Error updating Excel file: " + e.getMessage());
                        return Mono.just(result);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error processing cases: {}", error.getMessage(), error);
                    ProcessingResult result = new ProcessingResult();
                    result.setSuccess(false);
                    result.setMessage("Error processing cases: " + error.getMessage());
                    return Mono.just(result);
                });
    }
}

