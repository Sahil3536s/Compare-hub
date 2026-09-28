package com.comparehub.service;

import com.comparehub.dto.StructuredQueryUnderstandingDto;

public interface QueryValidationService {

    /**
     * Validates whether all mandatory entities for executing the intent are present.
     * Enriches StructuredQueryUnderstandingDto with isValid, missingFields, and clarificationPrompt.
     */
    void validateQuery(StructuredQueryUnderstandingDto queryUnderstanding);
}
