package com.example.transporterassignment.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "lanes")
public class Lane {

    @Id
    private Long id;

    @Column(name = "origin", nullable = false)
    private String origin;

    @Column(name = "destination", nullable = false)
    private String destination;

    @OneToMany(mappedBy = "lane", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LaneQuote> laneQuotes = new ArrayList<>();

    public Lane() {
    }

    public Lane(Long id, String origin, String destination) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public List<LaneQuote> getLaneQuotes() {
        return laneQuotes;
    }

    public void setLaneQuotes(List<LaneQuote> laneQuotes) {
        this.laneQuotes = laneQuotes;
    }

    public void addLaneQuote(LaneQuote quote) {
        laneQuotes.add(quote);
        quote.setLane(this);
    }

    public void removeLaneQuote(LaneQuote quote) {
        laneQuotes.remove(quote);
        quote.setLane(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lane lane)) return false;
        return Objects.equals(id, lane.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Lane{" +
                "id=" + id +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                '}';
    }
}
