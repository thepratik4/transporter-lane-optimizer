package com.example.transporterassignment.optimizer;

import com.example.transporterassignment.dto.OptimizationResult;

/**
 * Strategy interface for optimizing transporter assignments across logistical trade lanes.
 * Defines the contract for combinatorial, mathematical, or heuristic optimization strategies.
 */
public interface TransporterOptimizer {

    /**
     * Solves the optimal transporter assignment minimizing total quote cost under the constraint
     * of utilizing at most maxTransporters and ensuring 100% lane coverage.
     *
     * @param maxTransporters maximum allowable distinct transporters
     * @return OptimizationResult containing minimal cost, selected transporters, and lane assignments
     */
    OptimizationResult optimize(int maxTransporters);
}
