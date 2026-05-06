package com.travelplanningplatform.service;

import com.travelplanningplatform.dto.AuthResponse;
import com.travelplanningplatform.dto.LoginRequest;
import com.travelplanningplatform.dto.RegisterRequest;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.exception.BadRequestException;
import com.travelplanningplatform.exception.ResourceNotFoundException;
import com.travelplanningplatform.repository.UserRepository;
import com.travelplanningplatform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .username(request.username())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .build();

        userRepository.save(user);
        if (emailService != null) {
            emailService.sendRegistrationEmail(user);
        }

        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getUsername(),
                "User registered successfully"
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getUsername(),
                "Login successful"
        );
    }
}

