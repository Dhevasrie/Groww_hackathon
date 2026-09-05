package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "change_event_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ChangeEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "percent_change")
    private Double percentChange;

    @Column(name = "z_score")
    private Double zScore;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}