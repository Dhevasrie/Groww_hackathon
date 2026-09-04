package com.groww.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Example DTO showing validation pattern. Adapt fields to the real problem.
 * Keeping DTOs separate from entities is a cheap way to show good judgement
 * (don't leak persistence details through your API).
 */
@Data
public class ItemRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "value is required")
    @Positive(message = "value must be positive")
    private Double value;

}
