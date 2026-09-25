package com.comparehub.service;

import com.comparehub.dto.UserDashboardResponseDto;

public interface UserDashboardService {

    UserDashboardResponseDto getDashboardData(Long userId);
}
