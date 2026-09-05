package com.comparehub.service;

import com.comparehub.dto.SavingsEventDto;
import com.comparehub.dto.SavingsSummaryDto;

public interface UserSavingsService {

    SavingsSummaryDto getSavingsSummary(Long userId);

    SavingsEventDto recordSavingsEvent(Long userId, SavingsEventDto eventDto);

    SavingsEventDto confirmSavings(Long eventId);
}
