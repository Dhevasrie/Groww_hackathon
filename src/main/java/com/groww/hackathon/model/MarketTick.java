package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "market_tick")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MarketTick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "source_id", nullable = false)
    private String sourceId; // simulated "feed A" / "feed B" — used to demo conflict handling
}