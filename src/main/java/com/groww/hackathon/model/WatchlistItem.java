package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "watchlist_item", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId; // no auth system for the hackathon — session-based anonymous id, see README

    @Column(nullable = false)
    private String symbol;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();
}