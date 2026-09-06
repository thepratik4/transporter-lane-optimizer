package com.example.transporterassignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransporterInputDto {

    @NotNull(message = "Transporter id is required")
    private Long id;

    @NotBlank(message = "Transporter name is required")
    private String name;

    @NotEmpty(message = "Lane quotes list cannot be empty")
    private List<@Valid LaneQuoteInputDto> laneQuotes;
}
