package com.example.transporterassignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class InputDataRequest {

    @NotEmpty(message = "Lanes list cannot be empty")
    private List<@Valid LaneInputDto> lanes;

    @NotEmpty(message = "Transporters list cannot be empty")
    private List<@Valid TransporterInputDto> transporters;
}
