package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_symbol_stat", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol", "stat_date"}))
@Getter @Setter @NoArgsConstructor
public class DailySymbolStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "notable_events", nullable = false)
    private int notableEvents = 0;

    @Column(name = "significant_events", nullable = false)
    private int significantEvents = 0;

    @Column(name = "market_wide_events", nullable = false)
    private int marketWideEvents = 0;

    @Column(name = "isolated_events", nullable = false)
    private int isolatedEvents = 0;

    // last-recorded state, used to detect edges rather than counting every poll
    @Column(name = "last_severity")
    private String lastSeverity;

    @Column(name = "last_context")
    private String lastContext;

    @Column(name = "updated_at")
    private Instant updatedAt;
}