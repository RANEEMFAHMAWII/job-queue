package com.example.jobqueue.service;

import com.example.jobqueue.dto.AuthResponse;
import com.example.jobqueue.dto.LoginRequest;
import com.example.jobqueue.dto.RegisterRequest;
import com.example.jobqueue.entity.User;
import com.example.jobqueue.exception.InvalidCredentialsException;
import com.example.jobqueue.exception.UsernameAlreadyExistsException;
import com.example.jobqueue.repository.UserRepository;
import com.example.jobqueue.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        User user = new User(
                UUID.randomUUID(),
                request.username(),
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);
        log.info("User registered: {}", request.username());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        log.info("User logged in: {}", request.username());
        return new AuthResponse(token);
    }
}
