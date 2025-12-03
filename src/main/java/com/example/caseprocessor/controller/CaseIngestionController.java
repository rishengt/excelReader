package com.example.caseprocessor.controller;

import com.example.caseprocessor.model.ProcessingResult;
import com.example.caseprocessor.service.CaseProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for case processing operations
 */
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseProcessorController {

    private final CaseProcessorService caseProcessorService;

    /**
     * Process Excel file and create cases
     *
     * @param filePath Optional file path. If not provided, uses default from properties
     * @return ProcessingResult with summary
     */
    @PostMapping("/process")
    public Mono<ResponseEntity<ProcessingResult>> processCases(
            @RequestParam(required = false) String filePath) {

        return caseProcessorService.processCases(filePath)
                .map(result -> ResponseEntity.ok(result))
                .onErrorResume(error -> {
                    ProcessingResult errorResult = new ProcessingResult();
                    errorResult.setSuccess(false);
                    errorResult.setMessage("Error processing cases: " + error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(errorResult));
                });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Case Processor Service is running");
    }
}



