package com.comparehub.service.impl;

import com.comparehub.dto.HealthResponseDto;
import com.comparehub.service.HealthService;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public HealthResponseDto getHealthStatus() {
        return HealthResponseDto.builder()
                .status("UP")
                .build();
    }
}
