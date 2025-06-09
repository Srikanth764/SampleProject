package com.example.crudapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a User entity in the application.
 * This class is mapped to the "users" table in the database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // Optional: specify table name
public class User {

    /**
     * The unique identifier for the user.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the user.
     */
    private String name;

    /**
     * The email address of the user.
     */
    private String email;

    /**
     * Constructs a new User with the specified name and email.
     * ID is not set here, typically for new entities before persistence.
     * @param name The name of the user.
     * @param email The email address of the user.
     */
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Manual constructor for creating users without an ID, if needed for specific scenarios.
    // Lombok's @AllArgsConstructor will create a constructor with all fields (id, name, email).
    // If you only want a constructor for 'name' and 'email' (e.g., for user input before saving),
    // you might need to keep/add that one manually or use a different Lombok setup.
    // For this refactoring, we assume @AllArgsConstructor (id, name, email) and @NoArgsConstructor are sufficient.
    // The original public User(String name, String email) is effectively replaced by @AllArgsConstructor
    // if the ID is also included, or if ID is null then it's similar.
    // If a specific (String name, String email) constructor is still heavily used and expected
    // to NOT include 'id', it should be added back manually or Lombok needs more specific config.
    // For this task, we are removing the manual one as per instructions.
}
