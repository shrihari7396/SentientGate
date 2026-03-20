package edu.pict.dummyservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DummyController.class)
class DummyControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void index_ShouldReturnHelloWorld() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World!"));
    }

    @Test
    void dummy_ShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(get("/dummy"))
                .andExpect(status().isOk())
                .andExpect(content().string("Service Is running Successfully!    "));
    }
}
