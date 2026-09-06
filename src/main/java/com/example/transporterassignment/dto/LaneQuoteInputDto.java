package com.example.transporterassignment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaneQuoteInputDto {

    @NotNull(message = "Lane id is required")
    private Long laneId;

    @NotNull(message = "Quote amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Quote amount must not be negative")
    private BigDecimal quote;
}
