package dev.dinesh.items;

/** One stock item. Immutable; updates create a new instance. */
public record Item(long id, String name, int quantity) {
}
