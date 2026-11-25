package dev.dinesh.items;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemServiceTest {

    private ItemService service;

    @BeforeEach
    void setUp() {
        service = new ItemService();
    }

    @Test
    void createAssignsIncreasingIds() {
        Item first = service.create("bolt", 10);
        Item second = service.create("nut", 5);

        assertThat(first.id()).isEqualTo(1);
        assertThat(second.id()).isEqualTo(2);
    }

    @Test
    void getReturnsStoredItem() {
        Item created = service.create("bolt", 10);

        assertThat(service.get(created.id())).isEqualTo(created);
    }

    @Test
    void getMissingItemThrows() {
        assertThatThrownBy(() -> service.get(99))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("Item 99 not found");
    }

    @Test
    void listIsOrderedById() {
        service.create("b", 1);
        service.create("a", 2);

        assertThat(service.list()).extracting(Item::name).containsExactly("b", "a");
    }

    @Test
    void listIsEmptyAtStart() {
        assertThat(service.list()).isEmpty();
    }

    @Test
    void updateReplacesNameAndQuantity() {
        Item created = service.create("bolt", 10);

        Item updated = service.update(created.id(), "bolt-m8", 25);

        assertThat(updated).isEqualTo(new Item(created.id(), "bolt-m8", 25));
        assertThat(service.get(created.id())).isEqualTo(updated);
    }

    @Test
    void updateMissingItemThrows() {
        assertThatThrownBy(() -> service.update(7, "x", 1)).isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void deleteRemovesItem() {
        Item created = service.create("bolt", 10);

        service.delete(created.id());

        assertThat(service.list()).isEmpty();
    }

    @Test
    void deleteMissingItemThrows() {
        assertThatThrownBy(() -> service.delete(7)).isInstanceOf(ItemNotFoundException.class);
    }
}
