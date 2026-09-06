package com.example.transporterassignment.optimizer;

import com.example.transporterassignment.dto.LaneAssignmentDto;
import com.example.transporterassignment.dto.OptimizationResult;
import com.example.transporterassignment.model.Lane;
import com.example.transporterassignment.model.LaneQuote;
import com.example.transporterassignment.model.Transporter;
import com.example.transporterassignment.repository.LaneQuoteRepository;
import com.example.transporterassignment.repository.LaneRepository;
import com.example.transporterassignment.repository.TransporterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransporterOptimizerImpl implements TransporterOptimizer {

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;

    @Override
    public OptimizationResult optimize(int maxTransporters) {
        if (maxTransporters < 1) {
            throw new IllegalArgumentException("maxTransporters must be at least 1, but received: " + maxTransporters);
        }

        List<Lane> lanes = laneRepository.findAll();
        if (lanes.isEmpty()) {
            throw new IllegalStateException("No lanes found. Please submit input data first.");
        }

        List<Transporter> transporters = transporterRepository.findAll();
        if (transporters.isEmpty()) {
            throw new IllegalStateException("No transporters found. Please submit input data first.");
        }

        List<LaneQuote> quotes = laneQuoteRepository.findAll();
        if (quotes.isEmpty()) {
            throw new IllegalStateException("No transporter quotes found. Please submit input data first.");
        }

        // 1. Index quotes: laneId -> (transporterId -> quote amount)
        Map<Long, Map<Long, BigDecimal>> quotesByLane = new HashMap<>();
        for (LaneQuote q : quotes) {
            quotesByLane
                    .computeIfAbsent(q.getLane().getId(), k -> new HashMap<>())
                    .put(q.getTransporter().getId(), q.getQuote());
        }

        // Verify that every registered lane has at least one quote
        for (Lane lane : lanes) {
            Map<Long, BigDecimal> laneQuotes = quotesByLane.get(lane.getId());
            if (laneQuotes == null || laneQuotes.isEmpty()) {
                throw new IllegalStateException("Lane " + lane.getId() + " (" + lane.getOrigin() + " -> " + lane.getDestination() + ") has no quotes from any transporter.");
            }
        }

        List<Long> transporterIds = transporters.stream()
                .map(Transporter::getId)
                .distinct()
                .sorted()
                .toList();

        int kLimit = Math.min(maxTransporters, transporterIds.size());

        CandidateSolution bestSolution = null;

        // 2. Explore candidate transporter subsets up to kLimit
        for (int k = 1; k <= kLimit; k++) {
            List<List<Long>> combinations = new ArrayList<>();
            generateCombinations(transporterIds, k, 0, new ArrayList<>(), combinations);

            for (List<Long> subset : combinations) {
                CandidateSolution solution = evaluateSubset(lanes, subset, quotesByLane);
                if (solution == null) {
                    continue; // Subsetting cannot cover all lanes
                }

                if (isBetter(solution, bestSolution)) {
                    bestSolution = solution;
                }
            }
        }

        if (bestSolution == null) {
            throw new IllegalStateException(
                    "Cannot achieve full lane coverage with maxTransporters=" + maxTransporters + ". Please increase maxTransporters or add more lane quotes."
            );
        }

        // Sort assignments by laneId ascending for clean, deterministic output
        bestSolution.assignments.sort(Comparator.comparing(LaneAssignmentDto::getLaneId));
        Collections.sort(bestSolution.utilizedTransporters);

        return OptimizationResult.builder()
                .totalCost(bestSolution.totalCost)
                .assignments(bestSolution.assignments)
                .selectedTransporters(bestSolution.utilizedTransporters)
                .build();
    }

    /**
     * Evaluates whether a subset of transporters can achieve 100% lane coverage,
     * assigning each lane to the transporter in the subset offering the minimum quote.
     */
    private CandidateSolution evaluateSubset(
            List<Lane> lanes,
            List<Long> subset,
            Map<Long, Map<Long, BigDecimal>> quotesByLane) {

        BigDecimal totalCost = BigDecimal.ZERO;
        List<LaneAssignmentDto> assignments = new ArrayList<>();
        Set<Long> utilizedTransporterSet = new HashSet<>();

        for (Lane lane : lanes) {
            Map<Long, BigDecimal> availableQuotes = quotesByLane.get(lane.getId());

            BigDecimal lowestQuote = null;
            Long bestTransporterId = null;

            for (Long tId : subset) {
                BigDecimal quote = availableQuotes.get(tId);
                if (quote != null) {
                    if (lowestQuote == null || quote.compareTo(lowestQuote) < 0) {
                        lowestQuote = quote;
                        bestTransporterId = tId;
                    }
                }
            }

            // If a lane cannot be quoted by any transporter in this subset, full coverage failed
            if (bestTransporterId == null) {
                return null;
            }

            totalCost = totalCost.add(lowestQuote);
            assignments.add(new LaneAssignmentDto(lane.getId(), bestTransporterId));
            utilizedTransporterSet.add(bestTransporterId);
        }

        return new CandidateSolution(totalCost, new ArrayList<>(utilizedTransporterSet), assignments);
    }

    /**
     * Determines if candidate is better according to assignment criteria:
     * 1. Lower total cost (Cost Minimization)
     * 2. If costs are identical, higher number of utilized transporters (Maximize Transporter Usage)
     */
    private boolean isBetter(CandidateSolution candidate, CandidateSolution currentBest) {
        if (currentBest == null) {
            return true;
        }

        int costComparison = candidate.totalCost.compareTo(currentBest.totalCost);
        if (costComparison < 0) {
            return true;
        } else if (costComparison > 0) {
            return false;
        }

        // Tie-breaker on identical cost: maximize transporter usage
        return candidate.utilizedTransporters.size() > currentBest.utilizedTransporters.size();
    }

    /**
     * Backtracking utility to generate all combinations of size k.
     */
    private void generateCombinations(
            List<Long> items,
            int k,
            int startIndex,
            List<Long> current,
            List<List<Long>> result) {

        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = startIndex; i < items.size(); i++) {
            current.add(items.get(i));
            generateCombinations(items, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static class CandidateSolution {
        final BigDecimal totalCost;
        final List<Long> utilizedTransporters;
        final List<LaneAssignmentDto> assignments;

        CandidateSolution(BigDecimal totalCost, List<Long> utilizedTransporters, List<LaneAssignmentDto> assignments) {
            this.totalCost = totalCost;
            this.utilizedTransporters = utilizedTransporters;
            this.assignments = assignments;
        }
    }
}
