package com.comparehub.service.impl;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.provider.AirportLocationProvider;
import com.comparehub.service.AirportSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating dynamic airport and city discovery.
 * Primary source is an external airport location provider (e.g. Amadeus Reference Data Locations).
 * A tiny fallback list is retained solely for offline resilience and local demo/unit testing.
 */
@Slf4j
@Service
public class AirportSearchServiceImpl implements AirportSearchService {

    private final AirportLocationProvider airportLocationProvider;

    // Tiny fallback demo list for offline resilience / test environments (NOT authoritative)
    private static final List<AirportResultDto> TINY_FALLBACK_CATALOG = new ArrayList<>();
    private static final Map<String, AirportResultDto> FALLBACK_IATA_INDEX = new HashMap<>();

    static {
        // India sample hubs
        addFallback("Indira Gandhi International Airport", "DEL", "Delhi", "India", 28.5562, 77.1000);
        addFallback("Chhatrapati Shivaji Maharaj International Airport", "BOM", "Mumbai", "India", 19.0896, 72.8656);
        addFallback("Raja Bhoj Airport", "BHO", "Bhopal", "India", 23.2875, 77.3378);
        addFallback("Devi Ahilyabai Holkar Airport", "IDR", "Indore", "India", 22.7218, 75.8011);
        addFallback("Kempegowda International Airport", "BLR", "Bengaluru", "India", 13.1986, 77.7066);

        // Middle East
        addFallback("Dubai International Airport", "DXB", "Dubai", "United Arab Emirates", 25.2532, 55.3657);

        // United Kingdom (Multiple airports for London)
        addFallback("London Heathrow Airport", "LHR", "London", "United Kingdom", 51.4700, -0.4543);
        addFallback("London Gatwick Airport", "LGW", "London", "United Kingdom", 51.1537, -0.1821);
        addFallback("London Stansted Airport", "STN", "London", "United Kingdom", 51.8860, 0.2389);
        addFallback("London Luton Airport", "LTN", "London", "United Kingdom", 51.8747, -0.3683);
        addFallback("London City Airport", "LCY", "London", "United Kingdom", 51.5053, 0.0553);

        // United States (Multiple airports for New York & Los Angeles)
        addFallback("John F. Kennedy International Airport", "JFK", "New York", "United States", 40.6413, -73.7781);
        addFallback("LaGuardia Airport", "LGA", "New York", "United States", 40.7769, -73.8740);
        addFallback("Newark Liberty International Airport", "EWR", "New York", "United States", 40.6895, -74.1745);
        addFallback("Los Angeles International Airport", "LAX", "Los Angeles", "United States", 33.9416, -118.4085);
    }

    private static void addFallback(String name, String iata, String city, String country, Double lat, Double lon) {
        AirportResultDto dto = AirportResultDto.builder()
                .name(name)
                .iataCode(iata)
                .cityName(city)
                .countryName(country)
                .airportType("AIRPORT")
                .latitude(lat)
                .longitude(lon)
                .displayName(iata + " — " + name + ", " + city)
                .build();
        TINY_FALLBACK_CATALOG.add(dto);
        FALLBACK_IATA_INDEX.putIfAbsent(iata.toUpperCase(), dto);
    }

    @Autowired
    public AirportSearchServiceImpl(AirportLocationProvider airportLocationProvider) {
        this.airportLocationProvider = airportLocationProvider;
    }

    @Override
    @Cacheable(
            value = "airport-searches",
            key = "#query != null ? #query.trim().toLowerCase() : ''",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<AirportResultDto> searchAirports(String query) {
        if (query == null) {
            return Collections.emptyList();
        }

        String trimmed = query.trim();
        if (trimmed.length() < 2) {
            return Collections.emptyList();
        }

        // 1. PRIMARY: External provider-backed dynamic airport/city lookup
        if (airportLocationProvider != null && airportLocationProvider.isAvailable()) {
            try {
                List<AirportResultDto> providerResults = airportLocationProvider.searchAirports(trimmed, 10);
                if (providerResults != null && !providerResults.isEmpty()) {
                    log.info("Resolved {} airport results from provider '{}' for query '{}'",
                            providerResults.size(), airportLocationProvider.getProviderName(), trimmed);
                    return providerResults;
                }
            } catch (Exception e) {
                log.warn("External airport provider lookup failed for query '{}': {}. Falling back to demo list.",
                        trimmed, e.getMessage());
            }
        }

        // 2. FALLBACK: Search tiny fallback demo list when external provider is unavailable/offline
        return searchFallbackCatalog(trimmed);
    }

    @Override
    public AirportResultDto getAirportByIataCode(String iataCode) {
        if (iataCode == null) return null;
        String upper = iataCode.trim().toUpperCase();

        // 1. Try external provider if available
        if (airportLocationProvider != null && airportLocationProvider.isAvailable()) {
            try {
                List<AirportResultDto> results = airportLocationProvider.searchAirports(upper, 5);
                for (AirportResultDto dto : results) {
                    if (upper.equalsIgnoreCase(dto.getIataCode())) {
                        return dto;
                    }
                }
            } catch (Exception e) {
                log.debug("Provider lookup by IATA code '{}' failed: {}", upper, e.getMessage());
            }
        }

        // 2. Try fallback index
        AirportResultDto fallback = FALLBACK_IATA_INDEX.get(upper);
        if (fallback != null) {
            return fallback;
        }

        // 3. Fallback catalog search
        return TINY_FALLBACK_CATALOG.stream()
                .filter(a -> a.getIataCode().equalsIgnoreCase(upper))
                .findFirst()
                .orElse(null);
    }

    private List<AirportResultDto> searchFallbackCatalog(String trimmed) {
        String lower = trimmed.toLowerCase();
        Map<AirportResultDto, Integer> scored = new LinkedHashMap<>();

        for (AirportResultDto apt : TINY_FALLBACK_CATALOG) {
            int score = calculateMatchScore(apt, lower, trimmed);
            if (score > 0) {
                Integer current = scored.get(apt);
                if (current == null || score > current) {
                    scored.put(apt, score);
                }
            }
        }

        return scored.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .limit(10)
                .collect(Collectors.toList());
    }

    private int calculateMatchScore(AirportResultDto apt, String lower, String rawQuery) {
        String iata = apt.getIataCode().toLowerCase();
        String city = apt.getCityName().toLowerCase();
        String name = apt.getName().toLowerCase();
        String country = apt.getCountryName().toLowerCase();

        // Exact IATA match has highest priority
        if (iata.equalsIgnoreCase(rawQuery)) {
            return 1000;
        }
        // Exact city name
        if (city.equalsIgnoreCase(rawQuery)) {
            return 800;
        }
        // IATA prefix match
        if (iata.startsWith(lower)) {
            return 750;
        }
        // City starts with query
        if (city.startsWith(lower)) {
            return 600;
        }
        // Airport name starts with query
        if (name.startsWith(lower)) {
            return 500;
        }
        // City contains query
        if (city.contains(lower)) {
            return 400;
        }
        // Airport name contains query
        if (name.contains(lower)) {
            return 300;
        }
        // Country matches
        if (country.startsWith(lower) || country.contains(lower)) {
            return 150;
        }

        return 0;
    }
}
