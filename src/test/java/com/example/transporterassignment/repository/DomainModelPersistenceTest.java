package com.example.transporterassignment.repository;

import com.example.transporterassignment.model.Lane;
import com.example.transporterassignment.model.LaneQuote;
import com.example.transporterassignment.model.Transporter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DomainModelPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LaneRepository laneRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private LaneQuoteRepository laneQuoteRepository;

    @Test
    @DisplayName("Should persist Lane, Transporter, and LaneQuote with BigDecimal precision")
    void testPersistAndRetrieveEntities() {
        Lane lane = new Lane(1L, "Mumbai", "Delhi");
        Transporter transporter = new Transporter(1L, "Transporter T1");

        laneRepository.save(lane);
        transporterRepository.save(transporter);

        BigDecimal quoteAmount = new BigDecimal("20835.50");
        LaneQuote quote = new LaneQuote(lane, transporter, quoteAmount);
        laneQuoteRepository.save(quote);

        entityManager.flush();
        entityManager.clear();

        Optional<LaneQuote> retrieved = laneQuoteRepository.findByLaneIdAndTransporterId(1L, 1L);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getQuote()).isEqualByComparingTo(quoteAmount);
        assertThat(retrieved.get().getLane().getOrigin()).isEqualTo("Mumbai");
        assertThat(retrieved.get().getLane().getDestination()).isEqualTo("Delhi");
        assertThat(retrieved.get().getTransporter().getName()).isEqualTo("Transporter T1");
    }

    @Test
    @DisplayName("Should prevent duplicate transporter/lane quote relationships")
    void testPreventDuplicateQuotes() {
        Lane lane = new Lane(2L, "Delhi", "Bangalore");
        Transporter transporter = new Transporter(2L, "Transporter T2");

        laneRepository.save(lane);
        transporterRepository.save(transporter);

        LaneQuote quote1 = new LaneQuote(lane, transporter, new BigDecimal("10512.00"));
        laneQuoteRepository.save(quote1);
        entityManager.flush();

        LaneQuote quote2 = new LaneQuote(lane, transporter, new BigDecimal("12000.00"));
        assertThatThrownBy(() -> {
            laneQuoteRepository.save(quote2);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should query lane quotes by lane and by transporter")
    void testQueryQuotes() {
        Lane lane1 = new Lane(3L, "Chennai", "Kolkata");
        Lane lane2 = new Lane(4L, "Pune", "Hyderabad");
        Transporter t1 = new Transporter(3L, "Transporter T3");
        Transporter t2 = new Transporter(4L, "Transporter T4");

        laneRepository.saveAll(List.of(lane1, lane2));
        transporterRepository.saveAll(List.of(t1, t2));

        LaneQuote lq1 = new LaneQuote(lane1, t1, new BigDecimal("31438.00"));
        LaneQuote lq2 = new LaneQuote(lane1, t2, new BigDecimal("14316.00"));
        LaneQuote lq3 = new LaneQuote(lane2, t1, new BigDecimal("36447.00"));

        laneQuoteRepository.saveAll(List.of(lq1, lq2, lq3));
        entityManager.flush();
        entityManager.clear();

        List<LaneQuote> lane1Quotes = laneQuoteRepository.findByLaneId(3L);
        assertThat(lane1Quotes).hasSize(2);

        List<LaneQuote> t1Quotes = laneQuoteRepository.findByTransporterId(3L);
        assertThat(t1Quotes).hasSize(2);

        assertThat(laneQuoteRepository.existsByLaneIdAndTransporterId(3L, 3L)).isTrue();
        assertThat(laneQuoteRepository.existsByLaneIdAndTransporterId(4L, 4L)).isFalse();
    }
}
