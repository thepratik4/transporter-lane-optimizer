package com.example.transporterassignment.repository;

import com.example.transporterassignment.model.LaneQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LaneQuoteRepository extends JpaRepository<LaneQuote, Long> {

    Optional<LaneQuote> findByLaneIdAndTransporterId(Long laneId, Long transporterId);

    List<LaneQuote> findByLaneId(Long laneId);

    List<LaneQuote> findByTransporterId(Long transporterId);

    boolean existsByLaneIdAndTransporterId(Long laneId, Long transporterId);
}
