package com.example.caseprocessor.service;

import com.example.caseprocessor.model.CaseData;
import com.example.caseprocessor.model.CaseRequest;
import com.example.caseprocessor.model.CaseResponse;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseProcessorService {

    private final CaseApiService caseApiService;
    private final ExcelService excelService;

    @Value("${case.processing.concurrent-threads:5}")
    private int concurrentThreads;

    @Value("${case.excel.default-path:cases.xlsx}")
    private String defaultExcelPath;

    /**
     * Processes cases from Excel file using reactive programming
     * Processes cases in batches (5 at a time by default) using Flux
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
                .flatMapMany(cases -> {
                    log.info("Found {} cases to process with concurrency of {}", cases.size(), concurrentThreads);
                    if (cases.isEmpty()) {
                        return Flux.empty();
                    }

                    // Convert to Flux and process with concurrency control
                    // flatMap with concurrency parameter processes up to N items concurrently
                    return Flux.fromIterable(cases)
                            .flatMap(caseData -> {

                                // Create request
                                CaseRequest request = new CaseRequest(
                                        caseData.getSubject(),
                                        caseData.getClient(),
                                        caseData.getCaseDescription()
                                );

                                // Process case and update with response
                                return caseApiService.createCase(request)
                                        .doOnNext(response -> {
                                            if (response != null && response.getCaseNumber() != null
                                                    && !response.getCaseNumber().equals("ERROR")) {
                                                caseData.setCaseNumber(response.getCaseNumber());
                                                log.debug("Case created for row {}: {}",
                                                        caseData.getRowIndex(), response.getCaseNumber());
                                            } else {
                                                caseData.setCaseNumber("ERROR");
                                                log.error("Failed to create case for row {}", caseData.getRowIndex());
                                            }
                                        })
                                        .doOnError(error -> {
                                            caseData.setCaseNumber("ERROR");
                                            log.error("Exception processing row {}: {}",
                                                    caseData.getRowIndex(), error.getMessage());
                                        })
                                        .onErrorReturn(new CaseResponse("ERROR"))
                                        .thenReturn(caseData);
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
                .flatMap(processedCases -> {
                    // Update Excel with case numbers
                    log.info("Updating Excel file with case numbers...");
                    try {
                        excelService.updateExcelWithCaseNumbers(excelPath, processedCases);

                        // Calculate summary
                        long successCount = processedCases.stream()
                                .filter(c -> c.getCaseNumber() != null && !c.getCaseNumber().equals("ERROR"))
                                .count();
                        long errorCount = processedCases.size() - successCount;

                        ProcessingResult result = new ProcessingResult();
                        result.setSuccess(true);
                        result.setMessage("Processing completed successfully");
                        result.setTotalCases(processedCases.size());
                        result.setSuccessfulCases(successCount);
                        result.setErrorCases(errorCount);

                        log.info("Processing complete! Total: {}, Successful: {}, Errors: {}",
                                processedCases.size(), successCount, errorCount);

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

