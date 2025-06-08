package com.example.crudapp.controller;

import com.example.crudapp.model.User;
import com.example.crudapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Test User", "test@example.com");
        user.setId(1L);
    }

    // Test createUser
    @Test
    void createUser_success() {
        when(userService.createUser(any(User.class))).thenReturn(user);
        ResponseEntity<User> response = userController.createUser(new User("Test User", "test@example.com"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test User", response.getBody().getName());
        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void createUser_serviceThrowsException_shouldBeHandledBySpring() {
        // This test verifies that if service throws an exception (e.g. IllegalArgumentException from validation)
        // it's propagated and Spring's default error handling or a @ControllerAdvice would handle it.
        // We are not testing Spring's error handling itself here, just that controller calls the service.
        User newUser = new User(null, "test@example.com"); // Invalid user
        when(userService.createUser(any(User.class))).thenThrow(new IllegalArgumentException("Name is null"));

        assertThrows(IllegalArgumentException.class, () -> {
            userController.createUser(newUser);
        });
        verify(userService, times(1)).createUser(any(User.class));
    }


    // Test getAllUsers
    @Test
    void getAllUsers_success() {
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(user));
        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
        assertEquals(1, response.getBody().size());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsers_emptyList() {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());
        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(userService, times(1)).getAllUsers();
    }

    // Test getUserById
    @Test
    void getUserById_success() {
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        ResponseEntity<User> response = userController.getUserById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(user.getId(), response.getBody().getId());
        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void getUserById_notFound() {
        when(userService.getUserById(1L)).thenReturn(Optional.empty());
        ResponseEntity<User> response = userController.getUserById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void getUserById_serviceThrowsException() {
        // Specifically stub for null argument to avoid PotentialStubbingProblem
        when(userService.getUserById(null)).thenThrow(new IllegalArgumentException("ID is null"));
         assertThrows(IllegalArgumentException.class, () -> {
            userController.getUserById(null); // Or pass an ID that causes service to throw
        });
        verify(userService, times(1)).getUserById(null);
    }


    // Test updateUser
    @Test
    void updateUser_success() {
        User userDetails = new User("Updated Name", "updated@example.com");
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(user); // user object has old name/email here, but it's what updateUser returns

        user.setName("Updated Name"); // Simulate the update for assertion
        user.setEmail("updated@example.com");

        ResponseEntity<User> response = userController.updateUser(1L, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Name", response.getBody().getName());
        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }

    @Test
    void updateUser_notFound() {
        User userDetails = new User("Updated Name", "updated@example.com");
        when(userService.updateUser(eq(1L), any(User.class))).thenThrow(new RuntimeException("User not found"));
        ResponseEntity<User> response = userController.updateUser(1L, userDetails);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }

    @Test
    void updateUser_serviceThrowsIllegalArgumentException() {
        User userDetails = new User(null, "updated@example.com"); // Invalid details
        when(userService.updateUser(eq(1L), any(User.class))).thenThrow(new IllegalArgumentException("Name is null"));

        // Controller currently catches RuntimeException and returns NOT_FOUND.
        // If we want to test for BAD_REQUEST for IllegalArgumentException, controller logic would need to change.
        // For now, testing current behavior:
        ResponseEntity<User> response = userController.updateUser(1L, userDetails);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }


    // Test deleteUser
    @Test
    void deleteUser_success() {
        doNothing().when(userService).deleteUser(1L);
        ResponseEntity<HttpStatus> response = userController.deleteUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_notFound() {
        doThrow(new RuntimeException("User not found")).when(userService).deleteUser(1L);
        ResponseEntity<HttpStatus> response = userController.deleteUser(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_serviceThrowsIllegalArgumentException() {
        doThrow(new IllegalArgumentException("ID is null")).when(userService).deleteUser(null);

        // Similar to updateUser, controller catches RuntimeException and returns NOT_FOUND.
        ResponseEntity<HttpStatus> response = userController.deleteUser(null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(userService, times(1)).deleteUser(null);
    }
}
