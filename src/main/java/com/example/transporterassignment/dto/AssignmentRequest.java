package com.example.transporterassignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequest {

    @NotNull(message = "maxTransporters is required")
    @Min(value = 1, message = "maxTransporters must be at least 1")
    private Integer maxTransporters;
}
