package com.example.crudapp.service;

import com.example.crudapp.model.User;
import com.example.crudapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing users.
 * This class contains the business logic for user operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    /**
     * Constructs a UserService with the specified UserRepository.
     * @param userRepository The repository for user data access.
     */
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user after validating the input.
     * @param user The user object to create.
     * @return The created user.
     * @throws IllegalArgumentException if the user object or its essential fields (name, email) are null or empty.
     */
    @Transactional
    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User object cannot be null.");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty.");
        }
        // Add any other business logic before saving, e.g., validation
        return userRepository.save(user);
    }

    /**
     * Retrieves all users from the database.
     * @return A list of all users.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by their ID.
     * @param id The ID of the user to retrieve.
     * @return An Optional containing the user if found, or an empty Optional if not.
     * @throws IllegalArgumentException if the user ID is null.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        return userRepository.findById(id);
    }

    /**
     * Updates an existing user's details.
     * @param id The ID of the user to update.
     * @param userDetails An object containing the new details for the user.
     * @return The updated user.
     * @throws IllegalArgumentException if the user ID, userDetails object, or its essential fields (name, email) are null or empty.
     * @throws RuntimeException if the user with the given ID is not found.
     */
    @Transactional
    public User updateUser(Long id, User userDetails) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (userDetails == null) {
            throw new IllegalArgumentException("User details object cannot be null.");
        }
        if (userDetails.getName() == null || userDetails.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty.");
        }
        if (userDetails.getEmail() == null || userDetails.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id)); // Or a custom exception

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        // Add other fields to update as necessary
        return userRepository.save(user);
    }

    /**
     * Deletes a user by their ID.
     * @param id The ID of the user to delete.
     * @throws IllegalArgumentException if the user ID is null.
     * @throws RuntimeException if the user with the given ID is not found.
     */
    @Transactional
    public void deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (!userRepository.existsById(id)) {
            // Or throw a custom "NotFoundException"
            throw new RuntimeException("User not found with id " + id);
        }
        userRepository.deleteById(id);
    }
}
