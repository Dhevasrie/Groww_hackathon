package com.groww.hackathon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Placeholder entity. Rename/reshape this to match tomorrow's problem
 * (e.g. Transaction, Portfolio, Alert, Order...).
 */
@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "item_value")
    private Double value;

}
