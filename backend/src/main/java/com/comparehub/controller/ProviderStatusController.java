package com.comparehub.controller;

import com.comparehub.dto.ProviderStatusDto;
import com.comparehub.service.ProviderMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderStatusController {

    private final ProviderMonitoringService providerMonitoringService;

    @GetMapping("/status")
    public ResponseEntity<List<ProviderStatusDto>> getProvidersStatus() {
        List<ProviderStatusDto> statuses = providerMonitoringService.getProvidersStatus();
        return ResponseEntity.ok(statuses);
    }
}
