package com.groww.hackathon.service;

import com.groww.hackathon.dto.ItemRequest;
import com.groww.hackathon.exception.ResourceNotFoundException;
import com.groww.hackathon.model.Item;
import com.groww.hackathon.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Keep business logic here, not in the controller. Makes it easier to unit
 * test and to reason about (and explain) your core logic separately from
 * HTTP concerns.
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public List<Item> getAll() {
        return itemRepository.findAll();
    }

    public Item getById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    public Item create(ItemRequest request) {
        Item item = new Item(null, request.getName(), request.getValue());
        return itemRepository.save(item);
    }

    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }
}
