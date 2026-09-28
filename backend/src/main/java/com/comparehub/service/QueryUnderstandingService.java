package com.comparehub.service;

import com.comparehub.dto.StructuredQueryUnderstandingDto;

public interface QueryUnderstandingService {

    /**
     * Parses and understands raw user natural language queries into structured intents and entities.
     */
    StructuredQueryUnderstandingDto understandQuery(String naturalLanguageQuery);
}
