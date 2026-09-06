package com.example.transporterassignment.optimizer;

import com.example.transporterassignment.dto.LaneAssignmentDto;
import com.example.transporterassignment.dto.OptimizationResult;
import com.example.transporterassignment.model.Lane;
import com.example.transporterassignment.model.LaneQuote;
import com.example.transporterassignment.model.Transporter;
import com.example.transporterassignment.repository.LaneQuoteRepository;
import com.example.transporterassignment.repository.LaneRepository;
import com.example.transporterassignment.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TransporterOptimizerTest {

    @Autowired
    private TransporterOptimizer optimizer;

    @Autowired
    private LaneRepository laneRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private LaneQuoteRepository laneQuoteRepository;

    @BeforeEach
    void setUp() {
        laneQuoteRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should solve Test Case 1 with cost minimization, max 3 transporters, and full lane coverage")
    void testOptimizeTestCase1() {
        // Setup Test Case 1 Lanes
        List<Lane> lanes = List.of(
                new Lane(1L, "Mumbai", "Delhi"),
                new Lane(2L, "Delhi", "Bangalore"),
                new Lane(3L, "Chennai", "Kolkata"),
                new Lane(4L, "Pune", "Hyderabad"),
                new Lane(5L, "Ahmedabad", "Jaipur")
        );
        laneRepository.saveAll(lanes);

        // Setup Test Case 1 Transporters
        List<Transporter> transporters = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            transporters.add(new Transporter((long) i, "Transporter T" + i));
        }
        transporterRepository.saveAll(transporters);

        // Test Case 1 Quote matrix
        long[][] quotesMatrix = {
                {20835, 48844, 39020, 14400, 11601, 35095, 26070}, // Lane 1
                {10512, 31326, 20648, 44514, 19760, 12494, 41098}, // Lane 2
                {22105, 18640, 31438, 14316, 40870, 17808, 20932}, // Lane 3
                {42481, 45828, 36447, 10678, 20635, 36210, 16897}, // Lane 4
                {19862, 18297, 12789, 13032, 26421, 39444, 27938}  // Lane 5
        };

        List<LaneQuote> quotes = new ArrayList<>();
        for (int l = 0; l < 5; l++) {
            Lane lane = lanes.get(l);
            for (int t = 0; t < 7; t++) {
                Transporter transporter = transporters.get(t);
                quotes.add(new LaneQuote(lane, transporter, BigDecimal.valueOf(quotesMatrix[l][t])));
            }
        }
        laneQuoteRepository.saveAll(quotes);

        // Execute Optimizer
        OptimizationResult result = optimizer.optimize(3);

        // Assertions
        assertThat(result).isNotNull();
        assertThat(result.getSelectedTransporters()).hasSize(3);
        assertThat(result.getSelectedTransporters()).containsExactly(1L, 4L, 5L);

        // Minimum cost for combinations of size <= 3:
        // Lane 1 -> T5 (11601), Lane 2 -> T1 (10512), Lane 3 -> T4 (14316), Lane 4 -> T4 (10678), Lane 5 -> T4 (13032)
        // Total = 11601 + 10512 + 14316 + 10678 + 13032 = 60139
        assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("60139"));
        assertThat(result.getAssignments()).hasSize(5);

        assertThat(result.getAssignments()).containsExactly(
                new LaneAssignmentDto(1L, 5L),
                new LaneAssignmentDto(2L, 1L),
                new LaneAssignmentDto(3L, 4L),
                new LaneAssignmentDto(4L, 4L),
                new LaneAssignmentDto(5L, 4L)
        );
    }

    @Test
    @DisplayName("Should solve Test Case 2 with sparse disjoint quotes and max 3 transporters")
    void testOptimizeTestCase2() {
        // Setup Test Case 2 Lanes
        List<Lane> lanes = List.of(
                new Lane(1L, "Chandigarh", "Shimla"),
                new Lane(2L, "Agra", "Kanpur"),
                new Lane(3L, "Varanasi", "Gorakhpur"),
                new Lane(4L, "Amritsar", "Ludhiana"),
                new Lane(5L, "Coimbatore", "Madurai")
        );
        laneRepository.saveAll(lanes);

        // Setup Test Case 2 Transporters
        Transporter t1 = new Transporter(1L, "Transporter X");
        Transporter t2 = new Transporter(2L, "Transporter Y");
        Transporter t3 = new Transporter(3L, "Transporter Z");
        Transporter t4 = new Transporter(4L, "Transporter W");
        transporterRepository.saveAll(List.of(t1, t2, t3, t4));

        // Setup Test Case 2 Quotes
        List<LaneQuote> quotes = List.of(
                new LaneQuote(lanes.get(0), t1, new BigDecimal("1000")),
                new LaneQuote(lanes.get(1), t1, new BigDecimal("1500")),
                new LaneQuote(lanes.get(2), t2, new BigDecimal("2000")),
                new LaneQuote(lanes.get(3), t2, new BigDecimal("1800")),
                new LaneQuote(lanes.get(4), t3, new BigDecimal("1200")),
                new LaneQuote(lanes.get(0), t4, new BigDecimal("1100")),
                new LaneQuote(lanes.get(3), t4, new BigDecimal("1750"))
        );
        laneQuoteRepository.saveAll(quotes);

        // Execute Optimizer with maxTransporters = 3
        OptimizationResult result = optimizer.optimize(3);

        assertThat(result).isNotNull();
        // Since Lane 2 requires T1, Lane 3 requires T2, Lane 5 requires T3, selected must be T1, T2, T3
        assertThat(result.getSelectedTransporters()).containsExactly(1L, 2L, 3L);
        assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("7500"));
        assertThat(result.getAssignments()).hasSize(5);
        assertThat(result.getAssignments()).containsExactly(
                new LaneAssignmentDto(1L, 1L),
                new LaneAssignmentDto(2L, 1L),
                new LaneAssignmentDto(3L, 2L),
                new LaneAssignmentDto(4L, 2L),
                new LaneAssignmentDto(5L, 3L)
        );
    }

    @Test
    @DisplayName("Should throw IllegalStateException when full lane coverage cannot be achieved with maxTransporters")
    void testThrowsWhenFullCoverageImpossible() {
        // In Test Case 2, at least 3 transporters (T1, T2, T3) are strictly needed to cover lanes 2, 3, and 5
        List<Lane> lanes = List.of(
                new Lane(1L, "L1", "L1-dest"),
                new Lane(2L, "L2", "L2-dest"),
                new Lane(3L, "L3", "L3-dest")
        );
        laneRepository.saveAll(lanes);

        Transporter t1 = new Transporter(1L, "T1");
        Transporter t2 = new Transporter(2L, "T2");
        Transporter t3 = new Transporter(3L, "T3");
        transporterRepository.saveAll(List.of(t1, t2, t3));

        // Disjoint quotes: T1 only for Lane 1, T2 only for Lane 2, T3 only for Lane 3
        laneQuoteRepository.saveAll(List.of(
                new LaneQuote(lanes.get(0), t1, new BigDecimal("100")),
                new LaneQuote(lanes.get(1), t2, new BigDecimal("200")),
                new LaneQuote(lanes.get(2), t3, new BigDecimal("300"))
        ));

        // Asking for maxTransporters = 2 when 3 are required
        assertThatThrownBy(() -> optimizer.optimize(2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot achieve full lane coverage");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when maxTransporters is less than 1")
    void testThrowsWhenInvalidMaxTransporters() {
        assertThatThrownBy(() -> optimizer.optimize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTransporters must be at least 1");
    }
}
