package com.comparehub.controller;

import com.comparehub.dto.AuthResponseDto;
import com.comparehub.dto.AuthUserDto;
import com.comparehub.dto.LoginRequestDto;
import com.comparehub.dto.RegisterRequestDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        AuthUserDto userDto = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(userDto);
    }
}
