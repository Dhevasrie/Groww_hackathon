package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_view_state", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserViewState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "last_seen_price")
    private Double lastSeenPrice; // null = never viewed yet

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "last_seen_tick_id")
    private Long lastSeenTickId;
}