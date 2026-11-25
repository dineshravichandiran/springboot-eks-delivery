package dev.dinesh.items;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ItemService service;

    @Test
    void listReturnsItems() throws Exception {
        when(service.list()).thenReturn(List.of(new Item(1, "bolt", 10)));

        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("bolt"))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }

    @Test
    void getReturnsOneItem() throws Exception {
        when(service.get(1)).thenReturn(new Item(1, "bolt", 10));

        mvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getMissingItemIs404() throws Exception {
        when(service.get(5)).thenThrow(new ItemNotFoundException(5));

        mvc.perform(get("/api/items/5")).andExpect(status().isNotFound());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create("bolt", 10)).thenReturn(new Item(3, "bolt", 10));

        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bolt\",\"quantity\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/items/3"))
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"quantity\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsNegativeQuantity() throws Exception {
        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bolt\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsNewValues() throws Exception {
        when(service.update(1, "bolt-m8", 25)).thenReturn(new Item(1, "bolt-m8", 25));

        mvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bolt-m8\",\"quantity\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(25));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/items/1")).andExpect(status().isNoContent());

        verify(service).delete(1);
    }
}
