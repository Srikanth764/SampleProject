package com.example.crudapp.controller;

import com.example.crudapp.model.User;
import com.example.crudapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for managing users.
 * Provides endpoints for CRUD operations on users.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    /**
     * Constructs a UserController with the specified UserService.
     * @param userService The service to manage user data.
     */
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     * @param user The user object to create.
     * @return A ResponseEntity containing the created user and HTTP status CREATED.
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        logger.info("POST /api/users - Received request to create user with email: '{}'", user.getEmail());
        User createdUser = userService.createUser(user);
        logger.info("POST /api/users - Successfully created user with ID: {}. Responding with status 201.", createdUser.getId());
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Retrieves all users.
     * @return A ResponseEntity containing a list of all users and HTTP status OK.
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("GET /api/users - Received request to retrieve all users.");
        List<User> users = userService.getAllUsers();
        logger.info("GET /api/users - Retrieved {} users. Responding with status 200.", users.size());
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Retrieves a user by their ID.
     * @param id The ID of the user to retrieve.
     * @return A ResponseEntity containing the user if found and HTTP status OK,
     *         or HTTP status NOT_FOUND if the user is not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        logger.info("GET /api/users/{} - Received request to retrieve user.", id);
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("GET /api/users/{} - User found. Responding with status 200.", id);
                    return new ResponseEntity<>(user, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("GET /api/users/{} - User not found. Responding with status 404.", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    /**
     * Updates an existing user.
     * @param id The ID of the user to update.
     * @param userDetails The user object containing updated details.
     * @return A ResponseEntity containing the updated user and HTTP status OK,
     *         or HTTP status NOT_FOUND if the user is not found.
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        logger.info("PUT /api/users/{} - Received request to update user with email: '{}'", id, userDetails.getEmail());
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            logger.info("PUT /api/users/{} - User updated successfully. Responding with status 200.", id);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) { // Catching RuntimeException from service for now
            logger.warn("PUT /api/users/{} - Update failed, user not found. Responding with status 404.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Deletes a user by their ID.
     * @param id The ID of the user to delete.
     * @return A ResponseEntity with HTTP status NO_CONTENT if successful,
     *         or HTTP status NOT_FOUND if the user is not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /api/users/{} - Received request to delete user.", id);
        try {
            userService.deleteUser(id);
            logger.info("DELETE /api/users/{} - User deleted successfully. Responding with status 204.", id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) { // Catching RuntimeException from service for now
            logger.warn("DELETE /api/users/{} - Delete failed, user not found. Responding with status 404.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
