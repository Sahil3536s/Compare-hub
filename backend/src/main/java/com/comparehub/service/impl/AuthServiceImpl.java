package com.comparehub.service.impl;

import com.comparehub.dto.AuthResponseDto;
import com.comparehub.dto.AuthUserDto;
import com.comparehub.dto.LoginRequestDto;
import com.comparehub.dto.RegisterRequestDto;
import com.comparehub.exception.DuplicateResourceException;
import com.comparehub.model.User;
import com.comparehub.repository.UserRepository;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("User already exists with email: " + normalizedEmail);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateTokenFromUserIdAndEmail(
                savedUser.getId(), savedUser.getEmail(), savedUser.getName());

        return AuthResponseDto.builder()
                .token(token)
                .user(AuthUserDto.builder()
                        .id(savedUser.getId())
                        .name(savedUser.getName())
                        .email(savedUser.getEmail())
                        .build())
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(authentication);

        return AuthResponseDto.builder()
                .token(token)
                .user(AuthUserDto.builder()
                        .id(userPrincipal.getId())
                        .name(userPrincipal.getName())
                        .email(userPrincipal.getEmail())
                        .build())
                .build();
    }

    @Override
    public AuthUserDto getCurrentUser(UserPrincipal currentUser) {
        return AuthUserDto.builder()
                .id(currentUser.getId())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .build();
    }
}
