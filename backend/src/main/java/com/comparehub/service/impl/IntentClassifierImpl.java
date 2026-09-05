package com.comparehub.service.impl;

import com.comparehub.model.SearchIntent;
import com.comparehub.service.IntentClassifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class IntentClassifierImpl implements IntentClassifier {

    private static final Pattern FLIGHT_PATTERN = Pattern.compile(
            "\\b(flight|flights|fly|flying|airline|airfare|non-stop|nonstop|layover)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RIDE_PATTERN = Pattern.compile(
            "\\b(ride|rides|cab|cabs|taxi|taxis|uber|ola|rapido|auto|auto-rickshaw|driver)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PRODUCT_PATTERN = Pattern.compile(
            "\\b(phone|smartphone|laptop|macbook|headphone|earphones|camera|tv|television|watch|smartwatch|shoes|under|price|buy|deal|gb|tb|ram|ssd|mrp|discount)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> INDIAN_AIRPORT_CITIES = Set.of(
            "delhi", "new delhi", "del", "mumbai", "bombay", "bom", "bangalore", "bengaluru", "blr",
            "chennai", "madras", "maa", "hyderabad", "hyd", "kolkata", "calcutta", "ccu",
            "goa", "goi", "pune", "pnq", "bhopal", "bho", "jaipur", "jai", "ahmedabad", "amd", "cochin", "cok"
    );

    @Override
    public SearchIntent classifyIntent(String query) {
        if (query == null || query.isBlank()) {
            return SearchIntent.UNKNOWN;
        }

        String lower = query.toLowerCase(Locale.ROOT).trim();

        // 1. Explicit Ride keywords take precedence over general destinations
        if (RIDE_PATTERN.matcher(lower).find()) {
            return SearchIntent.RIDE_SEARCH;
        }

        // 2. Explicit Flight keywords
        if (FLIGHT_PATTERN.matcher(lower).find()) {
            return SearchIntent.FLIGHT_SEARCH;
        }

        // 3. City to City flight route heuristic if both ends are major airports
        if (lower.contains(" to ") && isAirportRoute(lower)) {
            return SearchIntent.FLIGHT_SEARCH;
        }

        // 4. Product Keywords or default product intent
        if (PRODUCT_PATTERN.matcher(lower).find()) {
            return SearchIntent.PRODUCT_SEARCH;
        }

        return SearchIntent.PRODUCT_SEARCH;
    }

    @Override
    public double calculateConfidence(String query, SearchIntent intent) {
        if (query == null || query.isBlank() || intent == SearchIntent.UNKNOWN) {
            return 0.50;
        }

        String lower = query.toLowerCase(Locale.ROOT);
        return switch (intent) {
            case FLIGHT_SEARCH -> FLIGHT_PATTERN.matcher(lower).find() ? 0.96 : 0.88;
            case RIDE_SEARCH -> RIDE_PATTERN.matcher(lower).find() ? 0.94 : 0.85;
            case PRODUCT_SEARCH -> PRODUCT_PATTERN.matcher(lower).find() ? 0.93 : 0.82;
            default -> 0.50;
        };
    }

    private boolean isAirportRoute(String lower) {
        String[] parts = lower.split(" to ");
        if (parts.length == 2) {
            String p1 = parts[0].replaceAll("^(from|flight|cheap|non-stop)\\s+", "").trim();
            String p2 = parts[1].replaceAll("\\s+(on|this|next|tomorrow|friday|morning|evening|cheap|tickets|flight|flights).*$", "").trim();
            return INDIAN_AIRPORT_CITIES.contains(p1) && INDIAN_AIRPORT_CITIES.contains(p2);
        }
        return false;
    }
}
