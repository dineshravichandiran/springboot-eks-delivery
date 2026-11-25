package dev.dinesh.items;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body accepted when creating or replacing an item. */
public record ItemRequest(
        @NotBlank @Size(max = 80) String name,
        @Min(0) int quantity) {
}
