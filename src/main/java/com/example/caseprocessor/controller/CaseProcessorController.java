package com.example.caseprocessor.controller;

import com.example.caseprocessor.model.ProcessingResult;
import com.example.caseprocessor.service.CaseProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
     * Process uploaded Excel file and create cases
     *
     * @param file The Excel file to process
     * @return ProcessingResult with summary
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<ProcessingResult>> processUploadedFile(
            @RequestParam("file") MultipartFile file) {

        try {
            // Create temporary file
            File tempFile = File.createTempFile("upload_", ".xlsx");
            Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Process the uploaded file
            return caseProcessorService.processCases(tempFile.getAbsolutePath())
                    .map(result -> ResponseEntity.ok(result))
                    .doFinally(signal -> tempFile.deleteOnExit())
                    .onErrorResume(error -> {
                        ProcessingResult errorResult = new ProcessingResult();
                        errorResult.setSuccess(false);
                        errorResult.setMessage("Error processing uploaded file: " + error.getMessage());
                        tempFile.deleteOnExit();
                        return Mono.just(ResponseEntity.internalServerError().body(errorResult));
                    });
        } catch (Exception e) {
            ProcessingResult errorResult = new ProcessingResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("Error handling uploaded file: " + e.getMessage());
            return Mono.just(ResponseEntity.internalServerError().body(errorResult));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Case Processor Service is running");
    }
}



