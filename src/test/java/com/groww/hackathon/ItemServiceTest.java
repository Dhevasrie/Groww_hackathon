package com.groww.hackathon;

import com.groww.hackathon.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Example unit test pattern. Even 3-4 tests on core logic (especially edge
 * cases: not-found, invalid input, boundary values) signal engineering
 * maturity to judges without costing much time.
 */
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemService = new ItemService(itemRepository);
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> itemService.getById(99L));
    }

    @Test
    void create_savesAndReturnsItem() {
        ItemRequest request = new ItemRequest();
        request.setName("Test");
        request.setValue(10.0);

        Item saved = new Item(1L, "Test", 10.0);
        when(itemRepository.save(any(Item.class))).thenReturn(saved);

        Item result = itemService.create(request);

        assertEquals("Test", result.getName());
        assertEquals(10.0, result.getValue());
    }
}
