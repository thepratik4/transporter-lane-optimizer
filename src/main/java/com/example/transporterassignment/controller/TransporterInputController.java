package com.example.transporterassignment.controller;

import com.example.transporterassignment.dto.ApiResponse;
import com.example.transporterassignment.dto.InputDataRequest;
import com.example.transporterassignment.service.TransporterDataService;
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
public class TransporterInputController {

    private final TransporterDataService transporterDataService;

    @PostMapping("/input")
    public ResponseEntity<ApiResponse> submitInputData(@Valid @RequestBody InputDataRequest request) {
        ApiResponse response = transporterDataService.saveInputData(request);
        return ResponseEntity.ok(response);
    }
}
