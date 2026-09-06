package com.example.transporterassignment.service;

import com.example.transporterassignment.dto.ApiResponse;
import com.example.transporterassignment.dto.InputDataRequest;
import com.example.transporterassignment.dto.LaneInputDto;
import com.example.transporterassignment.dto.LaneQuoteInputDto;
import com.example.transporterassignment.dto.TransporterInputDto;
import com.example.transporterassignment.model.Lane;
import com.example.transporterassignment.model.LaneQuote;
import com.example.transporterassignment.model.Transporter;
import com.example.transporterassignment.repository.LaneQuoteRepository;
import com.example.transporterassignment.repository.LaneRepository;
import com.example.transporterassignment.repository.TransporterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransporterDataService {

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;

    @Transactional
    public ApiResponse saveInputData(InputDataRequest request) {
        // Clear prior data in child -> parent dependency order to avoid FK constraint violations
        laneQuoteRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();

        // 1. Map and persist Lanes
        List<Lane> lanes = request.getLanes().stream()
                .map(dto -> new Lane(dto.getId(), dto.getOrigin(), dto.getDestination()))
                .collect(Collectors.toList());
        lanes = laneRepository.saveAll(lanes);
        Map<Long, Lane> laneMap = lanes.stream()
                .collect(Collectors.toMap(Lane::getId, Function.identity()));

        // 2. Map and persist Transporters
        List<Transporter> transporters = request.getTransporters().stream()
                .map(dto -> new Transporter(dto.getId(), dto.getName()))
                .collect(Collectors.toList());
        transporters = transporterRepository.saveAll(transporters);
        Map<Long, Transporter> transporterMap = transporters.stream()
                .collect(Collectors.toMap(Transporter::getId, Function.identity()));

        // 3. Map and persist Lane Quotes
        List<LaneQuote> quotes = new ArrayList<>();
        for (TransporterInputDto tDto : request.getTransporters()) {
            Transporter transporter = transporterMap.get(tDto.getId());

            for (LaneQuoteInputDto qDto : tDto.getLaneQuotes()) {
                Lane lane = laneMap.get(qDto.getLaneId());
                if (lane == null) {
                    throw new IllegalArgumentException(
                            "Lane with id " + qDto.getLaneId() + " referenced by transporter '" + tDto.getName() + "' was not found in lanes list"
                    );
                }
                quotes.add(new LaneQuote(lane, transporter, qDto.getQuote()));
            }
        }
        laneQuoteRepository.saveAll(quotes);

        return ApiResponse.builder()
                .status("success")
                .message("Input data saved successfully.")
                .build();
    }
}
