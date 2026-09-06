package com.example.transporterassignment.optimizer;

import com.example.transporterassignment.dto.OptimizationResult;

public interface TransporterOptimizer {

    OptimizationResult optimize(int maxTransporters);
}
