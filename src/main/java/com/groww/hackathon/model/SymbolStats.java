package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "symbol_stats")
@Getter @Setter @NoArgsConstructor
public class SymbolStats {

    @Id
    private String symbol;

    @Column(name = "mean_return", nullable = false)
    private double meanReturn = 0.0;

    @Column(name = "stddev_return", nullable = false)
    private double stddevReturn = 0.0001; // small non-zero floor so a brand-new symbol doesn't divide by zero

    @Column(name = "avg_volume", nullable = false)
    private double avgVolume = 0.0;

    @Column(name = "sample_count", nullable = false)
    private long sampleCount = 0;
}