package com.example.transporterassignment.controller;

import com.example.transporterassignment.dto.AssignmentRequest;
import com.example.transporterassignment.dto.AssignmentResponse;
import com.example.transporterassignment.dto.OptimizationResult;
import com.example.transporterassignment.optimizer.TransporterOptimizer;
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
@Tag(name = "Optimization Assignment", description = "Endpoints for computing optimal transporter assignments on trade lanes")
public class TransporterAssignmentController {

    private final TransporterOptimizer transporterOptimizer;

    @PostMapping("/assignment")
    @Operation(summary = "Compute optimal assignments", description = "Computes the minimal cost transporter assignment ensuring 100% lane coverage with at most maxTransporters, tie-breaking on max transporter diversity.")
    public ResponseEntity<AssignmentResponse> getOptimizedAssignments(@Valid @RequestBody AssignmentRequest request) {
        OptimizationResult result = transporterOptimizer.optimize(request.getMaxTransporters());

        AssignmentResponse response = AssignmentResponse.builder()
                .status("success")
                .totalCost(result.getTotalCost())
                .assignments(result.getAssignments())
                .selectedTransporters(result.getSelectedTransporters())
                .build();

        return ResponseEntity.ok(response);
    }
}
