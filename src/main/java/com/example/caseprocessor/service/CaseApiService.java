package com.example.caseprocessor.service;

import com.example.caseprocessor.model.CaseRequest;
import com.example.caseprocessor.model.CaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for calling the consumer app API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseApiService {

    private final WebClient webClient;

    @Value("${case.api.endpoint:/cases}")
    private String apiEndpoint;

    /**
     * Creates a case by posting to the consumer app
     * @param request The case request
     * @return CaseResponse containing the case number
     */
    public Mono<CaseResponse> createCase(CaseRequest request) {
        return webClient.post()
                .uri(apiEndpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CaseResponse.class)
                .doOnError(error -> log.error("Error creating case: {}", error.getMessage()))
                .onErrorReturn(new CaseResponse("ERROR"));
    }
}