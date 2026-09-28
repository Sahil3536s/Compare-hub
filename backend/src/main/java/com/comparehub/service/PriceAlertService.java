package com.comparehub.service;

import com.comparehub.dto.PriceAlertRequestDto;
import com.comparehub.dto.PriceAlertResponseDto;

import java.util.List;

public interface PriceAlertService {

    PriceAlertResponseDto createPriceAlert(PriceAlertRequestDto request);

    List<PriceAlertResponseDto> getAlertsByUserId(Long userId);

    List<PriceAlertResponseDto> getAllActiveAlerts();

    void toggleAlertStatus(Long alertId, boolean active);

    void toggleAlertStatus(Long userId, Long alertId, boolean active);

    void deleteAlert(Long alertId);

    void deleteAlert(Long userId, Long alertId);
}
