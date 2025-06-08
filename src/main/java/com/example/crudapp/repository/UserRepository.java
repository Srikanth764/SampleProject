package com.example.crudapp.repository;

import com.example.crudapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 * This interface handles all database operations related to Users.
 * It extends JpaRepository, which provides standard CRUD operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository provides all basic CRUD methods:
    // save(), findById(), findAll(), deleteById(), etc.
    // Custom query methods can be added here if needed.
}
