package com.example.transporterassignment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
    name = "lane_quotes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_lane_transporter", columnNames = {"lane_id", "transporter_id"})
    }
)
public class LaneQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lane_id", nullable = false)
    private Lane lane;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;

    @Column(name = "quote", nullable = false, precision = 12, scale = 2)
    private BigDecimal quote;

    public LaneQuote() {
    }

    public LaneQuote(Lane lane, Transporter transporter, BigDecimal quote) {
        this.lane = lane;
        this.transporter = transporter;
        this.quote = quote;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lane getLane() {
        return lane;
    }

    public void setLane(Lane lane) {
        this.lane = lane;
    }

    public Transporter getTransporter() {
        return transporter;
    }

    public void setTransporter(Transporter transporter) {
        this.transporter = transporter;
    }

    public BigDecimal getQuote() {
        return quote;
    }

    public void setQuote(BigDecimal quote) {
        this.quote = quote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LaneQuote that)) return false;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(lane, that.lane) && Objects.equals(transporter, that.transporter);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(lane, transporter);
    }

    @Override
    public String toString() {
        return "LaneQuote{" +
                "id=" + id +
                ", laneId=" + (lane != null ? lane.getId() : null) +
                ", transporterId=" + (transporter != null ? transporter.getId() : null) +
                ", quote=" + quote +
                '}';
    }
}
