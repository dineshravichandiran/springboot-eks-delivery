package dev.dinesh.items;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/** In-memory store. State is per pod, which is fine for a pipeline demo and is stated in the README. */
@Service
public class ItemService {

    private final Map<Long, Item> items = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public Item create(String name, int quantity) {
        long id = sequence.incrementAndGet();
        Item item = new Item(id, name, quantity);
        items.put(id, item);
        return item;
    }

    public Item get(long id) {
        Item item = items.get(id);
        if (item == null) {
            throw new ItemNotFoundException(id);
        }
        return item;
    }

    public List<Item> list() {
        return items.values().stream().sorted(Comparator.comparingLong(Item::id)).toList();
    }

    public Item update(long id, String name, int quantity) {
        Item updated = items.computeIfPresent(id, (key, old) -> new Item(key, name, quantity));
        if (updated == null) {
            throw new ItemNotFoundException(id);
        }
        return updated;
    }

    public void delete(long id) {
        if (items.remove(id) == null) {
            throw new ItemNotFoundException(id);
        }
    }
}
