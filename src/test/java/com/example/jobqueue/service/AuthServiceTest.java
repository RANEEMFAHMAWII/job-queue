package com.example.jobqueue.service;

import com.example.jobqueue.dto.AuthResponse;
import com.example.jobqueue.dto.LoginRequest;
import com.example.jobqueue.dto.RegisterRequest;
import com.example.jobqueue.entity.User;
import com.example.jobqueue.exception.InvalidCredentialsException;
import com.example.jobqueue.exception.UsernameAlreadyExistsException;
import com.example.jobqueue.repository.UserRepository;
import com.example.jobqueue.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_shouldSaveUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("newuser", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("newuser", saved.getUsername());
        assertTrue(passwordEncoder.matches("password123", saved.getPasswordHash()));
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> authService.register(new RegisterRequest("existing", "password123")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnToken() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "testuser", passwordEncoder.encode("password123"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(userId, "testuser")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(new LoginRequest("testuser", "password123"));

        assertEquals("mock.jwt.token", response.token());
    }

    @Test
    void login_shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("unknown", "password123")));
    }

    @Test
    void login_shouldThrowWhenPasswordWrong() {
        User user = new User(UUID.randomUUID(), "testuser", passwordEncoder.encode("correct"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("testuser", "wrongpassword")));
    }
}
