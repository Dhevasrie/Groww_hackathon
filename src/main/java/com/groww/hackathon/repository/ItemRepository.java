package com.groww.hackathon.repository;

import com.groww.hackathon.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // Add custom query methods here as needed, e.g.:
    // List<Item> findByName(String name);
}
