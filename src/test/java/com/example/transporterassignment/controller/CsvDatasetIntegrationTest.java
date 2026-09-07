package com.example.transporterassignment.controller;

import com.example.transporterassignment.dto.InputDataRequest;
import com.example.transporterassignment.dto.LaneInputDto;
import com.example.transporterassignment.dto.LaneQuoteInputDto;
import com.example.transporterassignment.dto.TransporterInputDto;
import com.example.transporterassignment.exception.GlobalExceptionHandler;
import com.example.transporterassignment.repository.LaneQuoteRepository;
import com.example.transporterassignment.repository.LaneRepository;
import com.example.transporterassignment.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class CsvDatasetIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private TransporterInputController inputController;

    @Autowired
    private TransporterAssignmentController assignmentController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private LaneRepository laneRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private LaneQuoteRepository laneQuoteRepository;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(inputController, assignmentController)
                .setControllerAdvice(globalExceptionHandler)
                .build();

        laneQuoteRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should ingest 9-lane CSV benchmark dataset and compute optimal assignments (134876 with maxTransporters=3)")
    void testEndToEndCsvDatasetOptimization() throws Exception {
        // 1. Parse CSV from classpath resource
        ClassPathResource resource = new ClassPathResource("transporter_assignment_data.csv");
        assertThat(resource.exists()).isTrue();

        // City definitions for the 9 lanes from the assignment
        String[][] laneCities = {
                {"Mumbai", "Delhi"},
                {"Delhi", "Bangalore"},
                {"Chennai", "Kolkata"},
                {"Pune", "Hyderabad"},
                {"Ahmedabad", "Jaipur"},
                {"Guwahati", "Shillong"},
                {"Lucknow", "Patna"},
                {"Ranchi", "Bhubaneswar"},
                {"Surat", "Indore"}
        };

        List<LaneInputDto> lanes = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            lanes.add(new LaneInputDto((long) (i + 1), laneCities[i][0], laneCities[i][1]));
        }

        Map<Integer, List<LaneQuoteInputDto>> quotesByTransporter = new HashMap<>();
        for (int t = 1; t <= 7; t++) {
            quotesByTransporter.put(t, new ArrayList<>());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // Trade Lane,T1,T2,T3,T4,T5,T6,T7
            assertThat(header).isNotNull();

            String line;
            int laneIndex = 1;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                for (int t = 1; t <= 7; t++) {
                    BigDecimal quoteValue = new BigDecimal(parts[t].trim());
                    quotesByTransporter.get(t).add(new LaneQuoteInputDto((long) laneIndex, quoteValue));
                }
                laneIndex++;
            }
        }

        List<TransporterInputDto> transporters = new ArrayList<>();
        for (int t = 1; t <= 7; t++) {
            transporters.add(new TransporterInputDto((long) t, "T" + t, quotesByTransporter.get(t)));
        }

        InputDataRequest request = new InputDataRequest(lanes, transporters);

        // 2. Submit CSV dataset directly to inputController
        var response = inputController.submitInputData(request);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("success");

        // Verify database row counts
        assertThat(laneRepository.count()).isEqualTo(9);
        assertThat(transporterRepository.count()).isEqualTo(7);
        assertThat(laneQuoteRepository.count()).isEqualTo(63); // 9 lanes * 7 transporters

        // 3. Request optimized assignment with maxTransporters = 3
        String assignmentPayload = """
                {
                  "maxTransporters": 3
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.totalCost").value(134876))
                .andExpect(jsonPath("$.selectedTransporters", hasSize(3)))
                .andExpect(jsonPath("$.selectedTransporters[0]").value(1))
                .andExpect(jsonPath("$.selectedTransporters[1]").value(4))
                .andExpect(jsonPath("$.selectedTransporters[2]").value(6))
                .andExpect(jsonPath("$.assignments", hasSize(9)))
                .andExpect(jsonPath("$.assignments[0].laneId").value(1))
                .andExpect(jsonPath("$.assignments[0].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[1].laneId").value(2))
                .andExpect(jsonPath("$.assignments[1].transporterId").value(1))
                .andExpect(jsonPath("$.assignments[2].laneId").value(3))
                .andExpect(jsonPath("$.assignments[2].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[3].laneId").value(4))
                .andExpect(jsonPath("$.assignments[3].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[4].laneId").value(5))
                .andExpect(jsonPath("$.assignments[4].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[5].laneId").value(6))
                .andExpect(jsonPath("$.assignments[5].transporterId").value(1))
                .andExpect(jsonPath("$.assignments[6].laneId").value(7))
                .andExpect(jsonPath("$.assignments[6].transporterId").value(1))
                .andExpect(jsonPath("$.assignments[7].laneId").value(8))
                .andExpect(jsonPath("$.assignments[7].transporterId").value(6))
                .andExpect(jsonPath("$.assignments[8].laneId").value(9))
                .andExpect(jsonPath("$.assignments[8].transporterId").value(6));
    }
}
