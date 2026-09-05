package com.comparehub.service;

import com.comparehub.model.SearchIntent;

public interface IntentClassifier {

    /**
     * Classifies a natural language query into a target SearchIntent with confidence.
     */
    SearchIntent classifyIntent(String query);

    /**
     * Calculates the classification confidence score (0.0 - 1.0).
     */
    double calculateConfidence(String query, SearchIntent intent);
}
