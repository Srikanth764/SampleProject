package com.example.crudapp.controller;

import com.example.crudapp.model.User;
import com.example.crudapp.repository.UserRepository; // For cleaning up
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
// @ActiveProfiles("test") // If you have specific test properties
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository; // Inject repository to clean up data

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        userRepository.deleteAll();

        user1 = new User("Test User 1", "test1@example.com");
        user2 = new User("Test User 2", "test2@example.com");
    }

    @AfterEach
    void tearDown() {
        // Clean up database after each test
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(user1.getName())))
                .andExpect(jsonPath("$.email", is(user1.getEmail())));
    }

    @Test
    void testGetAllUsers() throws Exception {
        userRepository.save(user1);
        userRepository.save(user2);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is(user1.getName())))
                .andExpect(jsonPath("$[1].name", is(user2.getName())));
    }

    @Test
    void testGetUserById() throws Exception {
        User savedUser = userRepository.save(user1);

        mockMvc.perform(get("/api/users/" + savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(savedUser.getName())))
                .andExpect(jsonPath("$.email", is(savedUser.getEmail())));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/999")) // Non-existent ID
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUser() throws Exception {
        User savedUser = userRepository.save(user1);
        User updatedDetails = new User("Updated Name", "updated@example.com");

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(updatedDetails.getName())))
                .andExpect(jsonPath("$.email", is(updatedDetails.getEmail())));
    }

    @Test
    void testUpdateUser_NotFound() throws Exception {
        User updatedDetails = new User("Updated Name", "updated@example.com");
        mockMvc.perform(put("/api/users/999") // Non-existent ID
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUser() throws Exception {
        User savedUser = userRepository.save(user1);

        mockMvc.perform(delete("/api/users/" + savedUser.getId()))
                .andExpect(status().isNoContent());

        // Verify it's actually deleted
        mockMvc.perform(get("/api/users/" + savedUser.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUser_NotFound() throws Exception {
        mockMvc.perform(delete("/api/users/999")) // Non-existent ID
                .andExpect(status().isNotFound());
    }
}
