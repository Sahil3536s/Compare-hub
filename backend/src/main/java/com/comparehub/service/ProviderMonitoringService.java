package com.comparehub.service;

import com.comparehub.dto.ProviderStatusDto;

import java.util.List;

public interface ProviderMonitoringService {

    List<ProviderStatusDto> getProvidersStatus();
}
