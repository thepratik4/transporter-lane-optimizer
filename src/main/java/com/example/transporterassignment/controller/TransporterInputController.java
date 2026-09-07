package com.example.transporterassignment.controller;

import com.example.transporterassignment.dto.ApiResponse;
import com.example.transporterassignment.dto.InputDataRequest;
import com.example.transporterassignment.service.TransporterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transporters")
@RequiredArgsConstructor
@Tag(name = "Data Ingestion", description = "Endpoints for ingesting logistical trade lanes and transporter quotes")
public class TransporterInputController {

    private final TransporterDataService transporterDataService;

    @PostMapping("/input")
    @Operation(summary = "Submit lanes and quotes", description = "Atomically clears previous database state and ingests new trade lanes, transporters, and quotes.")
    public ResponseEntity<ApiResponse> submitInputData(@Valid @RequestBody InputDataRequest request) {
        ApiResponse response = transporterDataService.saveInputData(request);
        return ResponseEntity.ok(response);
    }
}
