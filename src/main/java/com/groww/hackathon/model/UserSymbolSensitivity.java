package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Learned, per-user "how sensitive should alerts be for this symbol."
 * Starts at 1.0 (default thresholds). Each dismissal raises it, which makes
 * ChangeDetectionService.classify() require a bigger move before flagging
 * NOTABLE/SIGNIFICANT for that specific user+symbol pair — "meaningful"
 * becomes something the system learns per user, not a fixed cutoff.
 */
@Entity
@Table(name = "user_symbol_sensitivity", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSymbolSensitivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    // multiplier applied to the SIGNIFICANT/NOTABLE z-score cutoffs.
    // 1.0 = default. >1.0 = requires a bigger move to flag as notable/significant.
    @Column(name = "threshold_multiplier", nullable = false)
    private double thresholdMultiplier = 1.0;

    @Column(name = "dismiss_count", nullable = false)
    private int dismissCount = 0;

    @Column(name = "updated_at")
    private Instant updatedAt;
}