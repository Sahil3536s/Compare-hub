package com.comparehub.service;

import com.comparehub.dto.SearchIntentResultDto;

public interface SearchIntentService {

    SearchIntentResultDto detectIntent(String query);
}
