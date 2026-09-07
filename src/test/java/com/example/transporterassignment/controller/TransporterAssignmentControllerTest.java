package com.example.transporterassignment.controller;

import com.example.transporterassignment.exception.GlobalExceptionHandler;
import com.example.transporterassignment.repository.LaneQuoteRepository;
import com.example.transporterassignment.repository.LaneRepository;
import com.example.transporterassignment.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class TransporterAssignmentControllerTest {

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
    @DisplayName("Should successfully return optimized assignments for Test Case 1 with maxTransporters = 3")
    void testGetOptimizedAssignmentsTestCase1() throws Exception {
        // 1. Submit Test Case 1 input data
        String inputPayload = """
                {
                  "lanes": [
                    {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                    {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                    {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                    {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                    {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"}
                  ],
                  "transporters": [
                    {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 20835}, {"laneId": 2, "quote": 10512}, {"laneId": 3, "quote": 22105}, {"laneId": 4, "quote": 42481}, {"laneId": 5, "quote": 19862}]},
                    {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 1, "quote": 48844}, {"laneId": 2, "quote": 31326}, {"laneId": 3, "quote": 18640}, {"laneId": 4, "quote": 45828}, {"laneId": 5, "quote": 18297}]},
                    {"id": 3, "name": "Transporter T3", "laneQuotes": [{"laneId": 1, "quote": 39020}, {"laneId": 2, "quote": 20648}, {"laneId": 3, "quote": 31438}, {"laneId": 4, "quote": 36447}, {"laneId": 5, "quote": 12789}]},
                    {"id": 4, "name": "Transporter T4", "laneQuotes": [{"laneId": 1, "quote": 14400}, {"laneId": 2, "quote": 44514}, {"laneId": 3, "quote": 14316}, {"laneId": 4, "quote": 10678}, {"laneId": 5, "quote": 13032}]},
                    {"id": 5, "name": "Transporter T5", "laneQuotes": [{"laneId": 1, "quote": 11601}, {"laneId": 2, "quote": 19760}, {"laneId": 3, "quote": 40870}, {"laneId": 4, "quote": 20635}, {"laneId": 5, "quote": 26421}]},
                    {"id": 6, "name": "Transporter T6", "laneQuotes": [{"laneId": 1, "quote": 35095}, {"laneId": 2, "quote": 12494}, {"laneId": 3, "quote": 17808}, {"laneId": 4, "quote": 36210}, {"laneId": 5, "quote": 39444}]},
                    {"id": 7, "name": "Transporter T7", "laneQuotes": [{"laneId": 1, "quote": 26070}, {"laneId": 2, "quote": 41098}, {"laneId": 3, "quote": 20932}, {"laneId": 4, "quote": 16897}, {"laneId": 5, "quote": 27938}]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inputPayload))
                .andExpect(status().isOk());

        // 2. Request optimized assignments
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
                .andExpect(jsonPath("$.totalCost").value(60139))
                .andExpect(jsonPath("$.selectedTransporters", hasSize(3)))
                .andExpect(jsonPath("$.selectedTransporters[0]").value(1))
                .andExpect(jsonPath("$.selectedTransporters[1]").value(4))
                .andExpect(jsonPath("$.selectedTransporters[2]").value(5))
                .andExpect(jsonPath("$.assignments", hasSize(5)))
                .andExpect(jsonPath("$.assignments[0].laneId").value(1))
                .andExpect(jsonPath("$.assignments[0].transporterId").value(5))
                .andExpect(jsonPath("$.assignments[1].laneId").value(2))
                .andExpect(jsonPath("$.assignments[1].transporterId").value(1))
                .andExpect(jsonPath("$.assignments[2].laneId").value(3))
                .andExpect(jsonPath("$.assignments[2].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[3].laneId").value(4))
                .andExpect(jsonPath("$.assignments[3].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[4].laneId").value(5))
                .andExpect(jsonPath("$.assignments[4].transporterId").value(4));
    }

    @Test
    @DisplayName("Should reject assignment request when maxTransporters is less than 1")
    void testRejectInvalidMaxTransporters() throws Exception {
        String invalidPayload = """
                {
                  "maxTransporters": 0
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Should reject assignment request when maxTransporters is null/missing")
    void testRejectMissingMaxTransporters() throws Exception {
        String invalidPayload = "{}";

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when database has no data")
    void testEmptyDatabaseReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "maxTransporters": 3
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("No lanes found")));
    }
}
