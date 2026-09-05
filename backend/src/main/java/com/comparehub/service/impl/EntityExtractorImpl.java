package com.comparehub.service.impl;

import com.comparehub.dto.FlightQueryEntitiesDto;
import com.comparehub.dto.ProductQueryEntitiesDto;
import com.comparehub.dto.RideQueryEntitiesDto;
import com.comparehub.service.EntityExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EntityExtractorImpl implements EntityExtractor {

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
            Map.entry("amd", "AMD"),
            Map.entry("cochin", "COK"),
            Map.entry("kochi", "COK")
    );

    private static final Map<String, String> BRANDS = Map.ofEntries(
            Map.entry("apple", "Apple"),
            Map.entry("iphone", "Apple"),
            Map.entry("macbook", "Apple"),
            Map.entry("ipad", "Apple"),
            Map.entry("airpods", "Apple"),
            Map.entry("samsung", "Samsung"),
            Map.entry("galaxy", "Samsung"),
            Map.entry("sony", "Sony"),
            Map.entry("oneplus", "OnePlus"),
            Map.entry("xiaomi", "Xiaomi"),
            Map.entry("redmi", "Xiaomi"),
            Map.entry("realme", "Realme"),
            Map.entry("dell", "Dell"),
            Map.entry("hp", "HP"),
            Map.entry("asus", "Asus"),
            Map.entry("lenovo", "Lenovo"),
            Map.entry("boat", "boAt"),
            Map.entry("noise", "Noise"),
            Map.entry("bose", "Bose")
    );

    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile(
            "\\b(?:under|below|less than|budget|max|within|upto)\\s*(?:rs\\.?|inr|\\u20B9)?\\s*(\\d+k?|\\d{1,3}(?:,\\d{3})*)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STORAGE_PATTERN = Pattern.compile(
            "\\b(\\d+\\s*(?:gb|tb))\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern RAM_PATTERN = Pattern.compile(
            "\\b(\\d+\\s*gb)\\s*ram\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern FROM_TO_PATTERN = Pattern.compile(
            "(?:from\\s+)?([a-zA-Z0-9\\s,.-]+?)\\s+to\\s+([a-zA-Z0-9\\s,.-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public ProductQueryEntitiesDto extractProductEntities(String query) {
        if (query == null || query.isBlank()) {
            return ProductQueryEntitiesDto.builder().build();
        }

        String lower = query.toLowerCase(Locale.ROOT);

        // 1. Category extraction
        String category = "general";
        if (lower.contains("phone") || lower.contains("smartphone") || lower.contains("mobile")) {
            category = "smartphones";
        } else if (lower.contains("laptop") || lower.contains("macbook") || lower.contains("notebook")) {
            category = "laptops";
        } else if (lower.contains("headphone") || lower.contains("earphone") || lower.contains("earbuds") || lower.contains("tws") || lower.contains("audio")) {
            category = "audio & headphones";
        } else if (lower.contains("watch") || lower.contains("smartwatch")) {
            category = "smartwatches";
        } else if (lower.contains("tv") || lower.contains("television")) {
            category = "televisions";
        }

        // 2. Brand extraction
        String brand = null;
        for (Map.Entry<String, String> b : BRANDS.entrySet()) {
            if (lower.contains(b.getKey())) {
                brand = b.getValue();
                break;
            }
        }

        // 3. Price extraction
        BigDecimal maxPrice = null;
        Matcher priceMatcher = MAX_PRICE_PATTERN.matcher(query);
        if (priceMatcher.find()) {
            maxPrice = BigDecimal.valueOf(parsePrice(priceMatcher.group(1)));
        }

        // 4. Storage & RAM
        String storage = null;
        Matcher storageMatcher = STORAGE_PATTERN.matcher(query);
        if (storageMatcher.find()) {
            storage = storageMatcher.group(1).toUpperCase().replace(" ", "");
        }

        String ram = null;
        Matcher ramMatcher = RAM_PATTERN.matcher(query);
        if (ramMatcher.find()) {
            ram = ramMatcher.group(1).toUpperCase().replace(" ", "");
        }

        // 5. Priority (Camera, Battery, Gaming, Performance, Sound)
        String priority = null;
        if (lower.contains("camera") || lower.contains("photo") || lower.contains("video")) {
            priority = "camera";
        } else if (lower.contains("battery") || lower.contains("long lasting") || lower.contains("backup")) {
            priority = "battery";
        } else if (lower.contains("gaming") || lower.contains("game") || lower.contains("gpu")) {
            priority = "gaming";
        } else if (lower.contains("performance") || lower.contains("fast") || lower.contains("speed")) {
            priority = "performance";
        } else if (lower.contains("sound") || lower.contains("bass") || lower.contains("anc") || lower.contains("noise cancelling")) {
            priority = "sound";
        }

        // 6. Sort Preference
        String sortPref = "best_value";
        if (lower.contains("cheap") || lower.contains("lowest price") || lower.contains("under")) {
            sortPref = "price_asc";
        } else if (lower.contains("best") || lower.contains("top rated") || lower.contains("highest rated")) {
            sortPref = "rating_desc";
        }

        return ProductQueryEntitiesDto.builder()
                .category(category)
                .brand(brand)
                .maxPrice(maxPrice)
                .storage(storage)
                .ram(ram)
                .priority(priority)
                .sortPreference(sortPref)
                .build();
    }

    @Override
    public FlightQueryEntitiesDto extractFlightEntities(String query) {
        if (query == null || query.isBlank()) {
            return FlightQueryEntitiesDto.builder().build();
        }

        String lower = query.toLowerCase(Locale.ROOT);

        // 1. Origin & Destination
        String origin = null;
        String destination = null;

        // Clean out keywords before route matching
        String routeString = lower
                .replaceAll("\\b(flight|flights|fly|cheap|cheapest|ticket|tickets|non-stop|nonstop|direct|evening|morning|night|afternoon|friday|saturday|sunday|monday|tuesday|wednesday|thursday|tomorrow|today|next week)\\b", "")
                .replaceAll("\\s{2,}", " ")
                .trim();

        Matcher routeMatcher = FROM_TO_PATTERN.matcher(routeString);
        if (routeMatcher.find()) {
            origin = resolveAirportCode(routeMatcher.group(1).trim());
            destination = resolveAirportCode(routeMatcher.group(2).trim());
        }

        if (origin == null) {
            for (Map.Entry<String, String> e : CITY_TO_IATA.entrySet()) {
                if (lower.contains("from " + e.getKey())) {
                    origin = e.getValue();
                    break;
                }
            }
        }
        if (destination == null) {
            for (Map.Entry<String, String> e : CITY_TO_IATA.entrySet()) {
                if (lower.contains("to " + e.getKey())) {
                    destination = e.getValue();
                    break;
                }
            }
        }

        // 2. Departure Date
        String departureDate = extractDate(lower);

        // 3. Stops (0 for non-stop)
        Integer stops = null;
        if (lower.contains("non-stop") || lower.contains("nonstop") || lower.contains("direct") || lower.contains("0 stop")) {
            stops = 0;
        } else if (lower.contains("1 stop") || lower.contains("one stop")) {
            stops = 1;
        }

        // 4. Time Preference
        String timePref = "ANY";
        if (lower.contains("evening")) {
            timePref = "EVENING";
        } else if (lower.contains("morning")) {
            timePref = "MORNING";
        } else if (lower.contains("afternoon")) {
            timePref = "AFTERNOON";
        } else if (lower.contains("night") || lower.contains("late night")) {
            timePref = "NIGHT";
        }

        // 5. Sort Preference
        String sortPref = "PRICE";
        if (lower.contains("fastest") || lower.contains("duration")) {
            sortPref = "DURATION";
        } else if (lower.contains("best")) {
            sortPref = "BEST";
        }

        return FlightQueryEntitiesDto.builder()
                .origin(origin)
                .destination(destination)
                .departureDate(departureDate)
                .stops(stops)
                .timePreference(timePref)
                .sortPreference(sortPref)
                .build();
    }

    @Override
    public RideQueryEntitiesDto extractRideEntities(String query) {
        if (query == null || query.isBlank()) {
            return RideQueryEntitiesDto.builder().build();
        }

        String lower = query.toLowerCase(Locale.ROOT);
        String pickup = null;
        String destination = null;

        String clean = lower
                .replaceAll("\\b(find|book|get|me|a|cheap|ride|rides|cab|cabs|taxi|taxis|uber|ola|rapido|auto)\\b", "")
                .trim();

        Matcher m = FROM_TO_PATTERN.matcher(clean);
        if (m.find()) {
            pickup = capitalizeWords(m.group(1).trim());
            destination = capitalizeWords(m.group(2).trim());
        } else if (clean.startsWith("to ")) {
            destination = capitalizeWords(clean.substring(3).trim());
        }

        String rideType = "all";
        if (lower.contains("bike") || lower.contains("moto")) {
            rideType = "bike";
        } else if (lower.contains("auto")) {
            rideType = "auto";
        } else if (lower.contains("cab") || lower.contains("car") || lower.contains("taxi")) {
            rideType = "cab";
        }

        String sortPref = lower.contains("fastest") || lower.contains("quick") ? "ETA" : "CHEAPEST";

        return RideQueryEntitiesDto.builder()
                .pickup(pickup)
                .destination(destination)
                .rideType(rideType)
                .sortPreference(sortPref)
                .build();
    }

    @Override
    public String cleanQuery(String query) {
        if (query == null) return "";
        return query.replaceAll("(?i)\\b(find|me|a|the|best|cheapest|cheap|under|below|with|for|in)\\b", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String resolveAirportCode(String text) {
        if (text == null || text.isBlank()) return null;
        String clean = text.toLowerCase(Locale.ROOT).trim();
        if (CITY_TO_IATA.containsKey(clean)) {
            return CITY_TO_IATA.get(clean);
        }
        for (Map.Entry<String, String> e : CITY_TO_IATA.entrySet()) {
            if (clean.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return clean.length() == 3 ? clean.toUpperCase(Locale.ROOT) : null;
    }

    private String extractDate(String lower) {
        LocalDate today = LocalDate.now();

        if (lower.contains("today")) {
            return today.toString();
        }
        if (lower.contains("tomorrow")) {
            return today.plusDays(1).toString();
        }
        if (lower.contains("next week")) {
            return today.plusWeeks(1).toString();
        }

        // Day of week matching
        DayOfWeek[] days = DayOfWeek.values();
        for (DayOfWeek day : days) {
            String dayName = day.name().toLowerCase(Locale.ROOT);
            if (lower.contains(dayName)) {
                LocalDate target = today.with(TemporalAdjusters.nextOrSame(day));
                if (target.equals(today)) {
                    target = target.plusWeeks(1);
                }
                return target.toString();
            }
        }

        return today.plusDays(7).toString();
    }

    private long parsePrice(String s) {
        try {
            String clean = s.toLowerCase(Locale.ROOT).replace(",", "").trim();
            if (clean.endsWith("k")) {
                return (long) (Double.parseDouble(clean.replace("k", "")) * 1000);
            }
            return Long.parseLong(clean);
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
