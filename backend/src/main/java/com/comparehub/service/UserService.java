package com.comparehub.service;

import com.comparehub.dto.UserCreateRequestDto;
import com.comparehub.dto.UserResponseDto;

import java.util.List;

public interface UserService {

    UserResponseDto createUser(UserCreateRequestDto request);

    UserResponseDto getUserById(Long id);

    UserResponseDto getUserByEmail(String email);

    List<UserResponseDto> getAllUsers();

    void deleteUser(Long id);
}
