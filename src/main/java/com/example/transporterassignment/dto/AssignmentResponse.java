package com.example.transporterassignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {

    private String status;
    private BigDecimal totalCost;
    private List<LaneAssignmentDto> assignments;
    private List<Long> selectedTransporters;
}
