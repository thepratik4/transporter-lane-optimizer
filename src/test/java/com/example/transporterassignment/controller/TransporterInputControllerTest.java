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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TransporterInputControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private TransporterInputController controller;

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
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    @Test
    @DisplayName("Should successfully submit valid input data and persist all records (Test Case 1)")
    void testSubmitValidInputData() throws Exception {
        String payload = """
                {
                  "lanes": [
                    {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                    {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                    {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                    {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                    {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"}
                  ],
                  "transporters": [
                    {
                      "id": 1,
                      "name": "Transporter T1",
                      "laneQuotes": [
                        {"laneId": 1, "quote": 20835},
                        {"laneId": 2, "quote": 10512},
                        {"laneId": 3, "quote": 22105},
                        {"laneId": 4, "quote": 42481},
                        {"laneId": 5, "quote": 19862}
                      ]
                    },
                    {
                      "id": 2,
                      "name": "Transporter T2",
                      "laneQuotes": [
                        {"laneId": 1, "quote": 48844},
                        {"laneId": 2, "quote": 31326},
                        {"laneId": 3, "quote": 18640},
                        {"laneId": 4, "quote": 45828},
                        {"laneId": 5, "quote": 18297}
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Input data saved successfully."));

        assertThat(laneRepository.count()).isEqualTo(5);
        assertThat(transporterRepository.count()).isEqualTo(2);
        assertThat(laneQuoteRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should reject input when lanes or transporters are empty")
    void testRejectEmptyLists() throws Exception {
        String invalidPayload = """
                {
                  "lanes": [],
                  "transporters": []
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("Should reject input when quote is negative")
    void testRejectNegativeQuote() throws Exception {
        String invalidPayload = """
                {
                  "lanes": [
                    {"id": 1, "origin": "Mumbai", "destination": "Delhi"}
                  ],
                  "transporters": [
                    {
                      "id": 1,
                      "name": "Transporter T1",
                      "laneQuotes": [
                        {"laneId": 1, "quote": -500}
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("Should reject input when a quote references a non-existent lane id")
    void testRejectUnknownLaneReference() throws Exception {
        String payloadWithUnknownLane = """
                {
                  "lanes": [
                    {"id": 1, "origin": "Mumbai", "destination": "Delhi"}
                  ],
                  "transporters": [
                    {
                      "id": 1,
                      "name": "Transporter T1",
                      "laneQuotes": [
                        {"laneId": 999, "quote": 5000}
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithUnknownLane))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("999")));
    }
}
