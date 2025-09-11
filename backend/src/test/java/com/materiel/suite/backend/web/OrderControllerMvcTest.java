package com.materiel.suite.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.materiel.suite.backend.Application;
import com.materiel.suite.backend.model.DocumentLine;
import com.materiel.suite.backend.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class OrderControllerMvcTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void createAndGetOrder() throws Exception {
        Order o = new Order();
        DocumentLine line = new DocumentLine();
        line.setDesignation("Test");
        line.setQuantite(BigDecimal.ONE);
        line.setPrixUnitaireHT(new BigDecimal("200"));
        line.setTvaPct(new BigDecimal("20"));
        o.getLines().add(line);

        String json = mapper.writeValueAsString(o);
        String response = mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Order saved = mapper.readValue(response, Order.class);

        mockMvc.perform(get("/api/v1/orders/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHT").value(200))
                .andExpect(jsonPath("$.totalTVA").value(40))
                .andExpect(jsonPath("$.totalTTC").value(240));
    }
}

