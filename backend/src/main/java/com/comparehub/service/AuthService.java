package com.comparehub.service;

import com.comparehub.dto.AuthResponseDto;
import com.comparehub.dto.AuthUserDto;
import com.comparehub.dto.LoginRequestDto;
import com.comparehub.dto.RegisterRequestDto;
import com.comparehub.security.UserPrincipal;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    AuthUserDto getCurrentUser(UserPrincipal currentUser);
}
