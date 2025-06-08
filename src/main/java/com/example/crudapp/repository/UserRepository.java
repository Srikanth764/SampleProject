package com.example.crudapp.repository;

import com.example.crudapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository provides all basic CRUD methods:
    // save(), findById(), findAll(), deleteById(), etc.
    // Custom query methods can be added here if needed.
}
