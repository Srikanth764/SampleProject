package com.example.crudapp.service;

import com.example.crudapp.model.User;
import com.example.crudapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Test User", "test@example.com");
        user.setId(1L);
    }

    // Test createUser
    @Test
    void createUser_success() {
        when(userRepository.save(any(User.class))).thenReturn(user);
        User created = userService.createUser(new User("Test User", "test@example.com"));
        assertNotNull(created);
        assertEquals("Test User", created.getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_nullUser_throwsIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(null);
        });
        assertEquals("User object cannot be null.", exception.getMessage());
    }

    @Test
    void createUser_nullName_throwsIllegalArgumentException() {
        User testUser = new User(null, "test@example.com");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(testUser);
        });
        assertEquals("User name cannot be null or empty.", exception.getMessage());
    }

    @Test
    void createUser_emptyName_throwsIllegalArgumentException() {
        User testUser = new User(" ", "test@example.com");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(testUser);
        });
        assertEquals("User name cannot be null or empty.", exception.getMessage());
    }

    @Test
    void createUser_nullEmail_throwsIllegalArgumentException() {
        User testUser = new User("Test User", null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(testUser);
        });
        assertEquals("User email cannot be null or empty.", exception.getMessage());
    }

    @Test
    void createUser_emptyEmail_throwsIllegalArgumentException() {
        User testUser = new User("Test User", " ");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(testUser);
        });
        assertEquals("User email cannot be null or empty.", exception.getMessage());
    }

    // Test getAllUsers
    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));
        List<User> users = userService.getAllUsers();
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_emptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        List<User> users = userService.getAllUsers();
        assertTrue(users.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    // Test getUserById
    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Optional<User> foundUser = userService.getUserById(1L);
        assertTrue(foundUser.isPresent());
        assertEquals(user.getId(), foundUser.get().getId());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<User> foundUser = userService.getUserById(1L);
        assertFalse(foundUser.isPresent());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_nullId_throwsIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(null);
        });
        assertEquals("User ID cannot be null.", exception.getMessage());
    }

    // Test updateUser
    @Test
    void updateUser_success() {
        User userDetails = new User("Updated Name", "updated@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user); // mock save returning the updated user

        User updatedUser = userService.updateUser(1L, userDetails);

        assertNotNull(updatedUser);
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updated@example.com", updatedUser.getEmail());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUser_nullId_throwsIllegalArgumentException() {
        User userDetails = new User("Updated Name", "updated@example.com");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(null, userDetails);
        });
        assertEquals("User ID cannot be null.", exception.getMessage());
    }

    @Test
    void updateUser_nullUserDetails_throwsIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1L, null);
        });
        assertEquals("User details object cannot be null.", exception.getMessage());
    }

    @Test
    void updateUser_nullName_throwsIllegalArgumentException() {
        User userDetails = new User(null, "updated@example.com");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1L, userDetails);
        });
        assertEquals("User name cannot be null or empty.", exception.getMessage());
    }

    @Test
    void updateUser_emptyName_throwsIllegalArgumentException() {
        User userDetails = new User(" ", "updated@example.com");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1L, userDetails);
        });
        assertEquals("User name cannot be null or empty.", exception.getMessage());
    }

    @Test
    void updateUser_nullEmail_throwsIllegalArgumentException() {
        User userDetails = new User("Updated Name", null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1L, userDetails);
        });
        assertEquals("User email cannot be null or empty.", exception.getMessage());
    }

    @Test
    void updateUser_emptyEmail_throwsIllegalArgumentException() {
        User userDetails = new User("Updated Name", " ");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1L, userDetails);
        });
        assertEquals("User email cannot be null or empty.", exception.getMessage());
    }

    @Test
    void updateUser_userNotFound_throwsRuntimeException() {
        User userDetails = new User("Updated Name", "updated@example.com");
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(1L, userDetails);
        });
        assertTrue(exception.getMessage().contains("User not found with id "));
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    // Test deleteUser
    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);
        userService.deleteUser(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_nullId_throwsIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.deleteUser(null);
        });
        assertEquals("User ID cannot be null.", exception.getMessage());
    }

    @Test
    void deleteUser_userNotFound_throwsRuntimeException() {
        when(userRepository.existsById(1L)).thenReturn(false);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(1L);
        });
        assertTrue(exception.getMessage().contains("User not found with id "));
        verify(userRepository, times(1)).existsById(1L);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
