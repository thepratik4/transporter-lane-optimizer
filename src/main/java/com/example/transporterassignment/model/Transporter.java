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
@Table(name = "transporters")
public class Transporter {

    @Id
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LaneQuote> laneQuotes = new ArrayList<>();

    public Transporter() {
    }

    public Transporter(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LaneQuote> getLaneQuotes() {
        return laneQuotes;
    }

    public void setLaneQuotes(List<LaneQuote> laneQuotes) {
        this.laneQuotes = laneQuotes;
    }

    public void addLaneQuote(LaneQuote quote) {
        laneQuotes.add(quote);
        quote.setTransporter(this);
    }

    public void removeLaneQuote(LaneQuote quote) {
        laneQuotes.remove(quote);
        quote.setTransporter(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transporter that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transporter{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
