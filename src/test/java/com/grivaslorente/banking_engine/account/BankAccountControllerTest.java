package com.grivaslorente.banking_engine.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void creatingAccountWithEmptyNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "ownerName": "",
                                  "currency": "EUR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail")
                        .value("Owner name cannot be empty"));
    }
}
