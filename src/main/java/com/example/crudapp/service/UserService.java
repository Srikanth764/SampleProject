package com.example.crudapp.service;

import com.example.crudapp.model.User;
import com.example.crudapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for managing users.
 * This class contains the business logic for user operations.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
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
        logger.info("Attempting to create new user. Name: '{}', Email: '{}'", user.getName(), user.getEmail());
        if (user == null) {
            logger.warn("User creation failed: User object is null.");
            throw new IllegalArgumentException("User object cannot be null.");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            logger.warn("User creation failed: User name is null or empty.");
            throw new IllegalArgumentException("User name cannot be null or empty.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.warn("User creation failed: User email is null or empty.");
            throw new IllegalArgumentException("User email cannot be null or empty.");
        }
        // Add any other business logic before saving, e.g., validation
        User savedUser = userRepository.save(user);
        logger.info("Successfully created user with ID: {}. Name: '{}', Email: '{}'", savedUser.getId(), savedUser.getName(), savedUser.getEmail());
        return savedUser;
    }

    /**
     * Retrieves all users from the database.
     * @return A list of all users.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        logger.info("Attempting to retrieve all users.");
        List<User> users = userRepository.findAll();
        logger.info("Retrieved {} users.", users.size());
        return users;
    }

    /**
     * Retrieves a user by their ID.
     * @param id The ID of the user to retrieve.
     * @return An Optional containing the user if found, or an empty Optional if not.
     * @throws IllegalArgumentException if the user ID is null.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        logger.info("Attempting to retrieve user by ID: {}", id);
        if (id == null) {
            logger.warn("Failed to retrieve user: ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            logger.info("User found with ID: {}", id);
        } else {
            logger.info("User not found with ID: {}", id);
        }
        return userOptional;
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
        logger.info("Attempting to update user with ID: {}. New Name: '{}', New Email: '{}'", id, userDetails.getName(), userDetails.getEmail());
        if (id == null) {
            logger.warn("User update failed: ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (userDetails == null) {
            logger.warn("User update failed for ID {}: User details object is null.", id);
            throw new IllegalArgumentException("User details object cannot be null.");
        }
        if (userDetails.getName() == null || userDetails.getName().trim().isEmpty()) {
            logger.warn("User update failed for ID {}: User name is null or empty.", id);
            throw new IllegalArgumentException("User name cannot be null or empty.");
        }
        if (userDetails.getEmail() == null || userDetails.getEmail().trim().isEmpty()) {
            logger.warn("User update failed for ID {}: User email is null or empty.", id);
            throw new IllegalArgumentException("User email cannot be null or empty.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID {} during update attempt.", id);
                    return new RuntimeException("User not found with id " + id); // Or a custom exception
                });

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        // Add other fields to update as necessary
        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated user with ID: {}. Name: '{}', Email: '{}'", updatedUser.getId(), updatedUser.getName(), updatedUser.getEmail());
        return updatedUser;
    }

    /**
     * Deletes a user by their ID.
     * @param id The ID of the user to delete.
     * @throws IllegalArgumentException if the user ID is null.
     * @throws RuntimeException if the user with the given ID is not found.
     */
    @Transactional
    public void deleteUser(Long id) {
        logger.info("Attempting to delete user with ID: {}", id);
        if (id == null) {
            logger.warn("User deletion failed: ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (!userRepository.existsById(id)) {
            logger.warn("User not found with ID {} during delete attempt.", id);
            // Or throw a custom "NotFoundException"
            throw new RuntimeException("User not found with id " + id);
        }
        userRepository.deleteById(id);
        logger.info("Successfully deleted user with ID: {}", id);
    }
}
