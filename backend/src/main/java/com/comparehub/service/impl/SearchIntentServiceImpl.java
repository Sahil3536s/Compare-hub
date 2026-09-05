package com.comparehub.service.impl;

import com.comparehub.dto.SearchIntentResultDto;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.SearchIntentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SearchIntentServiceImpl implements SearchIntentService {

    // Airport City Mappings
    private static final Map<String, String> CITY_TO_IATA = Map.ofEntries(
            Map.entry("delhi", "DEL"),
            Map.entry("new delhi", "DEL"),
            Map.entry("del", "DEL"),
            Map.entry("mumbai", "BOM"),
            Map.entry("bombay", "BOM"),
            Map.entry("bom", "BOM"),
            Map.entry("bangalore", "BLR"),
            Map.entry("bengaluru", "BLR"),
            Map.entry("blr", "BLR"),
            Map.entry("chennai", "MAA"),
            Map.entry("madras", "MAA"),
            Map.entry("maa", "MAA"),
            Map.entry("hyderabad", "HYD"),
            Map.entry("hyd", "HYD"),
            Map.entry("kolkata", "CCU"),
            Map.entry("calcutta", "CCU"),
            Map.entry("ccu", "CCU"),
            Map.entry("goa", "GOI"),
            Map.entry("goi", "GOI"),
            Map.entry("pune", "PNQ"),
            Map.entry("pnq", "PNQ"),
            Map.entry("bhopal", "BHO"),
            Map.entry("bho", "BHO"),
            Map.entry("jaipur", "JAI"),
            Map.entry("jai", "JAI"),
            Map.entry("ahmedabad", "AMD"),
            Map.entry("amd", "AMD")
    );

    // Known Brand Keyword Mappings
    private static final Map<String, String> BRAND_KEYWORDS = Map.ofEntries(
            Map.entry("apple", "Apple"),
            Map.entry("iphone", "Apple"),
            Map.entry("ipad", "Apple"),
            Map.entry("macbook", "Apple"),
            Map.entry("airpods", "Apple"),
            Map.entry("samsung", "Samsung"),
            Map.entry("galaxy", "Samsung"),
            Map.entry("sony", "Sony"),
            Map.entry("bravia", "Sony"),
            Map.entry("dell", "Dell"),
            Map.entry("asus", "Asus"),
            Map.entry("rog", "Asus"),
            Map.entry("hp", "HP"),
            Map.entry("lenovo", "Lenovo"),
            Map.entry("oneplus", "OnePlus"),
            Map.entry("xiaomi", "Xiaomi"),
            Map.entry("redmi", "Xiaomi"),
            Map.entry("realme", "Realme"),
            Map.entry("lg", "LG"),
            Map.entry("bose", "Bose"),
            Map.entry("boat", "boAt"),
            Map.entry("noise", "Noise")
    );

    // Regex Patterns
    private static final Pattern FLIGHT_KEYWORD_PATTERN = Pattern.compile(
            "\\b(flight|flights|fly|ticket|tickets|airline|airfare)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern FLIGHT_FROM_TO_PATTERN = Pattern.compile(
            "(?:from\\s+)?([a-zA-Z\\s]+?)\\s+to\\s+([a-zA-Z\\s]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RIDE_KEYWORD_PATTERN = Pattern.compile(
            "\\b(ride|cab|cabs|taxi|uber|ola|rapido|auto)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern RIDE_FROM_TO_PATTERN = Pattern.compile(
            "(?:ride|cab|taxi|uber|ola|rapido)?\\s*(?:from\\s+)?(.+?)\\s+to\\s+(.+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PRICE_UNDER_PATTERN = Pattern.compile(
            "\\b(?:under|below|less than|budget|max|within)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+k?|\\d{1,3}(?:,\\d{3})*)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern STORAGE_PATTERN = Pattern.compile(
            "\\b(\\d+\\s*(?:gb|tb))\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public SearchIntentResultDto detectIntent(String query) {
        if (query == null || query.trim().isBlank()) {
            return SearchIntentResultDto.builder()
                    .intent(SearchIntent.UNKNOWN)
                    .originalQuery(query)
                    .query("")
                    .confidence(0.0)
                    .build();
        }

        String raw = query.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        // 1. Check for Flight Intent
        if (isFlightIntent(lower)) {
            return parseFlightIntent(raw, lower);
        }

        // 2. Check for Ride Intent
        if (isRideIntent(lower)) {
            return parseRideIntent(raw, lower);
        }

        // 3. Default to Product Intent (with filter extraction)
        return parseProductIntent(raw, lower);
    }

    private boolean isFlightIntent(String lower) {
        if (FLIGHT_KEYWORD_PATTERN.matcher(lower).find()) {
            return true;
        }
        Matcher matcher = FLIGHT_FROM_TO_PATTERN.matcher(lower);
        if (matcher.find()) {
            String from = matcher.group(1).trim();
            String to = matcher.group(2).trim();
            if (CITY_TO_IATA.containsKey(from) && CITY_TO_IATA.containsKey(to)) {
                return true;
            }
        }
        return false;
    }

    private SearchIntentResultDto parseFlightIntent(String raw, String lower) {
        Map<String, String> flightParams = new HashMap<>();
        String origin = "DEL";
        String destination = "BOM";
        String departureDate = LocalDate.now().plusDays(7).toString();

        if (lower.contains("tomorrow")) {
            departureDate = LocalDate.now().plusDays(1).toString();
        } else if (lower.contains("today")) {
            departureDate = LocalDate.now().toString();
        } else if (lower.contains("next week")) {
            departureDate = LocalDate.now().plusWeeks(1).toString();
        }

        String cleanRoute = lower
                .replaceAll("\\b(flight|flights|fly|ticket|tickets|airline|airfare|tomorrow|today|next week)\\b", "")
                .trim();

        Matcher matcher = FLIGHT_FROM_TO_PATTERN.matcher(cleanRoute);
        if (matcher.find()) {
            String fromText = matcher.group(1).trim();
            String toText = matcher.group(2).trim();

            origin = resolveAirportCode(fromText, "DEL");
            destination = resolveAirportCode(toText, "BOM");
        } else {
            for (Map.Entry<String, String> entry : CITY_TO_IATA.entrySet()) {
                if (lower.contains("from " + entry.getKey())) {
                    origin = entry.getValue();
                } else if (lower.contains("to " + entry.getKey())) {
                    destination = entry.getValue();
                }
            }
        }

        flightParams.put("origin", origin);
        flightParams.put("destination", destination);
        flightParams.put("departureDate", departureDate);

        return SearchIntentResultDto.builder()
                .intent(SearchIntent.FLIGHT_SEARCH)
                .originalQuery(raw)
                .query(origin + " to " + destination + " Flights")
                .confidence(0.95)
                .flightParams(flightParams)
                .build();
    }

    private boolean isRideIntent(String lower) {
        if (RIDE_KEYWORD_PATTERN.matcher(lower).find()) {
            return true;
        }
        if (lower.contains("from ") && lower.contains(" to ")) {
            return !FLIGHT_KEYWORD_PATTERN.matcher(lower).find();
        }
        return false;
    }

    private SearchIntentResultDto parseRideIntent(String raw, String lower) {
        Map<String, String> rideParams = new HashMap<>();
        String pickup = "Connaught Place, New Delhi";
        String destination = "Indira Gandhi Airport, New Delhi";

        String cleanRoute = lower
                .replaceAll("\\b(ride|rides|cab|cabs|taxi|taxis|uber|ola|rapido|auto)\\b", "")
                .trim();

        Matcher matcher = RIDE_FROM_TO_PATTERN.matcher(cleanRoute);
        if (matcher.find()) {
            String fromText = matcher.group(1).trim();
            String toText = matcher.group(2).trim();

            if (!fromText.isBlank()) pickup = capitalizeWords(fromText);
            if (!toText.isBlank()) destination = capitalizeWords(toText);
        }

        rideParams.put("pickup", pickup);
        rideParams.put("destination", destination);

        return SearchIntentResultDto.builder()
                .intent(SearchIntent.RIDE_SEARCH)
                .originalQuery(raw)
                .query("Ride from " + pickup + " to " + destination)
                .confidence(0.92)
                .rideParams(rideParams)
                .build();
    }

    private SearchIntentResultDto parseProductIntent(String raw, String lower) {
        Map<String, Object> filters = new HashMap<>();
        String cleanedQuery = raw;

        // 1. Extract Max Price
        Matcher priceMatcher = PRICE_UNDER_PATTERN.matcher(cleanedQuery);
        if (priceMatcher.find()) {
            String matchedPriceStr = priceMatcher.group(1).toLowerCase().replace(",", "");
            long maxPriceVal = parsePriceValue(matchedPriceStr);
            filters.put("maxPrice", BigDecimal.valueOf(maxPriceVal));
            cleanedQuery = cleanedQuery.replace(priceMatcher.group(0), "").trim();
        }

        // 2. Extract Storage
        Matcher storageMatcher = STORAGE_PATTERN.matcher(cleanedQuery);
        if (storageMatcher.find()) {
            String storage = storageMatcher.group(1).toUpperCase().replace(" ", "");
            filters.put("storage", storage);
            // Remove storage clause and connective "with" from query
            cleanedQuery = cleanedQuery.replaceAll("(?i)\\bwith\\s+" + Pattern.quote(storageMatcher.group(0)) + "\\b", "")
                                       .replaceAll("(?i)\\b" + Pattern.quote(storageMatcher.group(0)) + "\\b", "")
                                       .trim();
        }

        // 3. Extract Brand
        for (Map.Entry<String, String> entry : BRAND_KEYWORDS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                filters.put("brand", entry.getValue());
                break;
            }
        }

        // Clean extra connective words
        cleanedQuery = cleanedQuery.replaceAll("(?i)\\b(with|for|and|in)\\s*$", "")
                                   .replaceAll("\\s{2,}", " ")
                                   .trim();
        if (cleanedQuery.isBlank()) {
            cleanedQuery = raw;
        }

        return SearchIntentResultDto.builder()
                .intent(SearchIntent.PRODUCT_SEARCH)
                .originalQuery(raw)
                .query(cleanedQuery)
                .confidence(0.90)
                .filters(filters)
                .build();
    }

    private String resolveAirportCode(String text, String fallback) {
        String clean = text.toLowerCase(Locale.ROOT).trim();
        if (CITY_TO_IATA.containsKey(clean)) {
            return CITY_TO_IATA.get(clean);
        }
        for (Map.Entry<String, String> entry : CITY_TO_IATA.entrySet()) {
            if (clean.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return clean.length() == 3 ? clean.toUpperCase(Locale.ROOT) : fallback;
    }

    private long parsePriceValue(String str) {
        try {
            if (str.endsWith("k")) {
                return (long) (Double.parseDouble(str.replace("k", "")) * 1000);
            }
            return Long.parseLong(str);
        } catch (Exception e) {
            return 50000;
        }
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
