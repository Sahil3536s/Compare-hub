package com.comparehub.service;

import com.comparehub.dto.SmartDealDto;
import com.comparehub.dto.SmartDealsPageDto;
import com.comparehub.model.SmartDealCategory;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DealDiscoveryService {

    SmartDealsPageDto getPersonalizedSmartDeals(SmartDealCategory category, Pageable pageable);

    List<SmartDealDto> discoverSmartDeals();
}
