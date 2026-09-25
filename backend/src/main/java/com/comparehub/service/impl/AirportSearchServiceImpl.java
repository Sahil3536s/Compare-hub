package com.comparehub.service.impl;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.service.AirportSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AirportSearchServiceImpl implements AirportSearchService {

    @Value("${app.providers.amadeus.enabled:true}")
    private boolean amadeusEnabled;

    @Value("${app.providers.amadeus.client-id:}")
    private String amadeusClientId;

    @Value("${app.providers.amadeus.client-secret:}")
    private String amadeusClientSecret;

    @Value("${app.providers.amadeus.auth-url:https://test.api.amadeus.com/v1/security/oauth2/token}")
    private String amadeusAuthUrl;

    @Value("${app.providers.amadeus.location-url:https://test.api.amadeus.com/v1/reference-data/locations}")
    private String amadeusLocationUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.builder().build();

    // Comprehensive catalog of commercial airports across India and global international hubs
    private static final List<AirportResultDto> AIRPORT_CATALOG = new ArrayList<>();
    private static final Map<String, AirportResultDto> IATA_LOOKUP = new HashMap<>();

    static {
        // --- INDIA PRIMARY HUBS & REGIONAL AIRPORTS ---
        addAirport("Indira Gandhi International Airport", "DEL", "Delhi", "India", 28.5562, 77.1000);
        addAirport("Chhatrapati Shivaji Maharaj International Airport", "BOM", "Mumbai", "India", 19.0896, 72.8656);
        addAirport("Raja Bhoj Airport", "BHO", "Bhopal", "India", 23.2875, 77.3378);
        addAirport("Devi Ahilyabai Holkar Airport", "IDR", "Indore", "India", 22.7218, 75.8011);
        addAirport("Kempegowda International Airport", "BLR", "Bangalore", "India", 13.1986, 77.7066);
        addAirport("Kempegowda International Airport", "BLR", "Bengaluru", "India", 13.1986, 77.7066);
        addAirport("Chennai International Airport", "MAA", "Chennai", "India", 12.9941, 80.1709);
        addAirport("Netaji Subhash Chandra Bose International Airport", "CCU", "Kolkata", "India", 22.6547, 88.4467);
        addAirport("Rajiv Gandhi International Airport", "HYD", "Hyderabad", "India", 17.2403, 78.4294);
        addAirport("Dabolim Airport", "GOI", "Goa", "India", 15.3800, 73.8314);
        addAirport("Manohar International Airport", "GOX", "Goa", "India", 15.7667, 73.8667);
        addAirport("Pune Airport", "PNQ", "Pune", "India", 18.5822, 73.9197);
        addAirport("Sardar Vallabhbhai Patel International Airport", "AMD", "Ahmedabad", "India", 23.0772, 72.6347);
        addAirport("Jaipur International Airport", "JAI", "Jaipur", "India", 26.8242, 75.8122);
        addAirport("Cochin International Airport", "COK", "Kochi", "India", 10.1520, 76.3920);
        addAirport("Chaudhary Charan Singh International Airport", "LKO", "Lucknow", "India", 26.7606, 80.8893);
        addAirport("Lal Bahadur Shastri International Airport", "VNS", "Varanasi", "India", 25.4524, 82.8593);
        addAirport("Sri Guru Ram Dass Jee International Airport", "ATQ", "Amritsar", "India", 31.7096, 74.7973);
        addAirport("Jay Prakash Narayan Airport", "PAT", "Patna", "India", 25.5913, 85.0880);
        addAirport("Sheikh ul-Alam International Airport", "SXR", "Srinagar", "India", 33.9871, 74.7744);
        addAirport("Biju Patnaik International Airport", "BBI", "Bhubaneswar", "India", 20.2444, 85.8178);
        addAirport("Lokpriya Gopinath Bordoloi International Airport", "GAU", "Guwahati", "India", 26.1061, 91.5859);
        addAirport("Shaheed Bhagat Singh International Airport", "IXC", "Chandigarh", "India", 30.6735, 76.7885);
        addAirport("Dr. Babasaheb Ambedkar International Airport", "NAG", "Nagpur", "India", 21.0922, 79.0472);
        addAirport("Coimbatore International Airport", "CJB", "Coimbatore", "India", 11.0300, 77.0434);
        addAirport("Thiruvananthapuram International Airport", "TRV", "Thiruvananthapuram", "India", 8.4821, 76.9200);
        addAirport("Mangalore International Airport", "IXE", "Mangalore", "India", 12.9613, 74.8900);
        addAirport("Tiruchirappalli International Airport", "TRZ", "Tiruchirappalli", "India", 10.7654, 78.7107);
        addAirport("Visakhapatnam International Airport", "VTZ", "Visakhapatnam", "India", 17.7212, 83.2245);
        addAirport("Vadodara Airport", "BDQ", "Vadodara", "India", 22.3327, 73.2263);
        addAirport("Surat International Airport", "STV", "Surat", "India", 21.1136, 72.7419);
        addAirport("Rajkot International Airport", "RAJ", "Rajkot", "India", 22.3092, 70.7794);
        addAirport("Swami Vivekananda Airport", "RPR", "Raipur", "India", 21.1804, 81.7388);
        addAirport("Dehradun Airport", "DED", "Dehradun", "India", 30.1897, 78.1803);
        addAirport("Maharana Pratap Airport", "UDR", "Udaipur", "India", 24.6177, 73.8961);
        addAirport("Jodhpur Airport", "JDH", "Jodhpur", "India", 26.2511, 73.0489);
        addAirport("Rajmata Vijaya Raje Scindia Airport", "GWL", "Gwalior", "India", 26.2933, 78.2278);
        addAirport("Jabalpur Airport", "JLR", "Jabalpur", "India", 23.1778, 80.0522);

        // --- MIDDLE EAST HUBS ---
        addAirport("Dubai International Airport", "DXB", "Dubai", "United Arab Emirates", 25.2532, 55.3657);
        addAirport("Al Maktoum International Airport", "DWC", "Dubai", "United Arab Emirates", 24.8960, 55.1614);
        addAirport("Zayed International Airport", "AUH", "Abu Dhabi", "United Arab Emirates", 24.4330, 54.6511);
        addAirport("Hamad International Airport", "DOH", "Doha", "Qatar", 25.2731, 51.6081);
        addAirport("King Khalid International Airport", "RUH", "Riyadh", "Saudi Arabia", 24.9576, 46.6988);
        addAirport("King Abdulaziz International Airport", "JED", "Jeddah", "Saudi Arabia", 21.6796, 39.1565);
        addAirport("Muscat International Airport", "MCT", "Muscat", "Oman", 23.5933, 58.2844);
        addAirport("Bahrain International Airport", "BAH", "Manama", "Bahrain", 26.2708, 50.6336);
        addAirport("Kuwait International Airport", "KWI", "Kuwait City", "Kuwait", 29.2267, 47.9689);

        // --- UNITED KINGDOM & EUROPE (MULTIPLE AIRPORTS) ---
        addAirport("London Heathrow Airport", "LHR", "London", "United Kingdom", 51.4700, -0.4543);
        addAirport("London Gatwick Airport", "LGW", "London", "United Kingdom", 51.1537, -0.1821);
        addAirport("London Stansted Airport", "STN", "London", "United Kingdom", 51.8860, 0.2389);
        addAirport("London Luton Airport", "LTN", "London", "United Kingdom", 51.8747, -0.3683);
        addAirport("London City Airport", "LCY", "London", "United Kingdom", 51.5053, 0.0553);
        addAirport("Charles de Gaulle Airport", "CDG", "Paris", "France", 49.0097, 2.5479);
        addAirport("Paris Orly Airport", "ORY", "Paris", "France", 48.7262, 2.3652);
        addAirport("Frankfurt Airport", "FRA", "Frankfurt", "Germany", 50.0379, 8.5622);
        addAirport("Munich Airport", "MUC", "Munich", "Germany", 48.3537, 11.7750);
        addAirport("Amsterdam Airport Schiphol", "AMS", "Amsterdam", "Netherlands", 52.3105, 4.7683);
        addAirport("Zurich Airport", "ZRH", "Zurich", "Switzerland", 47.4582, 8.5555);
        addAirport("Leonardo da Vinci-Fiumicino Airport", "FCO", "Rome", "Italy", 41.8003, 12.2389);
        addAirport("Milan Malpensa Airport", "MXP", "Milan", "Italy", 45.6301, 8.7255);
        addAirport("Milan Linate Airport", "LIN", "Milan", "Italy", 45.4451, 9.2767);
        addAirport("Adolfo Suárez Madrid-Barajas Airport", "MAD", "Madrid", "Spain", 40.4839, -3.5680);
        addAirport("Josep Tarradellas Barcelona-El Prat Airport", "BCN", "Barcelona", "Spain", 41.2974, 2.0833);
        addAirport("Istanbul Airport", "IST", "Istanbul", "Turkey", 41.2753, 28.7519);
        addAirport("Sabiha Gokcen International Airport", "SAW", "Istanbul", "Turkey", 40.8986, 29.3092);

        // --- NORTH AMERICA (MULTIPLE AIRPORTS) ---
        addAirport("John F. Kennedy International Airport", "JFK", "New York", "United States", 40.6413, -73.7781);
        addAirport("LaGuardia Airport", "LGA", "New York", "United States", 40.7769, -73.8740);
        addAirport("Newark Liberty International Airport", "EWR", "New York", "United States", 40.6895, -74.1745);
        addAirport("Los Angeles International Airport", "LAX", "Los Angeles", "United States", 33.9416, -118.4085);
        addAirport("O'Hare International Airport", "ORD", "Chicago", "United States", 41.9742, -87.9073);
        addAirport("Midway International Airport", "MDW", "Chicago", "United States", 41.7868, -87.7522);
        addAirport("San Francisco International Airport", "SFO", "San Francisco", "United States", 37.6213, -122.3790);
        addAirport("Seattle-Tacoma International Airport", "SEA", "Seattle", "United States", 47.4502, -122.3088);
        addAirport("Miami International Airport", "MIA", "Miami", "United States", 25.7959, -80.2870);
        addAirport("Hartsfield-Jackson Atlanta International Airport", "ATL", "Atlanta", "United States", 33.6407, -84.4277);
        addAirport("Dallas/Fort Worth International Airport", "DFW", "Dallas", "United States", 32.8998, -97.0403);
        addAirport("Logan International Airport", "BOS", "Boston", "United States", 42.3656, -71.0096);
        addAirport("Washington Dulles International Airport", "IAD", "Washington", "United States", 38.9531, -77.4565);
        addAirport("Ronald Reagan Washington National Airport", "DCA", "Washington", "United States", 38.8512, -77.0402);
        addAirport("Toronto Pearson International Airport", "YYZ", "Toronto", "Canada", 43.6777, -79.6248);
        addAirport("Vancouver International Airport", "YVR", "Vancouver", "Canada", 49.1967, -123.1815);

        // --- ASIA-PACIFIC HUBS ---
        addAirport("Singapore Changi Airport", "SIN", "Singapore", "Singapore", 1.3644, 103.9915);
        addAirport("Suvarnabhumi Airport", "BKK", "Bangkok", "Thailand", 13.6900, 100.7501);
        addAirport("Don Mueang International Airport", "DMK", "Bangkok", "Thailand", 13.9126, 100.6067);
        addAirport("Kuala Lumpur International Airport", "KUL", "Kuala Lumpur", "Malaysia", 2.7456, 101.7072);
        addAirport("Hong Kong International Airport", "HKG", "Hong Kong", "Hong Kong", 22.3080, 113.9185);
        addAirport("Tokyo Haneda Airport", "HND", "Tokyo", "Japan", 35.5494, 139.7798);
        addAirport("Narita International Airport", "NRT", "Tokyo", "Japan", 35.7720, 140.3929);
        addAirport("Incheon International Airport", "ICN", "Seoul", "South Korea", 37.4602, 126.4407);
        addAirport("Sydney Kingsford Smith Airport", "SYD", "Sydney", "Australia", -33.9399, 151.1753);
        addAirport("Melbourne Airport", "MEL", "Melbourne", "Australia", -37.6690, 144.8410);
        addAirport("Ngurah Rai International Airport", "DPS", "Bali", "Indonesia", -8.7482, 115.1672);
    }

    private static void addAirport(String name, String iataCode, String cityName, String countryName, Double lat, Double lon) {
        AirportResultDto dto = AirportResultDto.builder()
                .name(name)
                .iataCode(iataCode)
                .cityName(cityName)
                .countryName(countryName)
                .airportType("AIRPORT")
                .latitude(lat)
                .longitude(lon)
                .build();
        AIRPORT_CATALOG.add(dto);
        IATA_LOOKUP.putIfAbsent(iataCode.toUpperCase(), dto);
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
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. If live Amadeus integration is authorized, attempt provider location lookup
        if (amadeusEnabled && amadeusClientId != null && !amadeusClientId.isBlank() && amadeusClientSecret != null && !amadeusClientSecret.isBlank()) {
            try {
                List<AirportResultDto> amadeusResults = fetchFromAmadeus(trimmed);
                if (amadeusResults != null && !amadeusResults.isEmpty()) {
                    log.info("Resolved {} airport results from live Amadeus location API for query '{}'", amadeusResults.size(), trimmed);
                    return amadeusResults;
                }
            } catch (Exception e) {
                log.warn("Amadeus location search failed for query '{}': {}. Falling back to internal catalog.", trimmed, e.getMessage());
            }
        }

        // 2. Search internal comprehensive catalog with ranking priority
        String lower = trimmed.toLowerCase();
        Map<AirportResultDto, Integer> scored = new LinkedHashMap<>();

        for (AirportResultDto apt : AIRPORT_CATALOG) {
            int score = calculateMatchScore(apt, lower, trimmed);
            if (score > 0) {
                // Keep highest score if duplicate IATA
                Integer current = scored.get(apt);
                if (current == null || score > current) {
                    scored.put(apt, score);
                }
            }
        }

        // If no matches found and trimmed is a 3-letter uppercase code, provide fallback representation
        if (scored.isEmpty() && trimmed.length() == 3 && trimmed.matches("[a-zA-Z]{3}")) {
            String upper = trimmed.toUpperCase();
            return List.of(AirportResultDto.builder()
                    .name(upper + " International Airport")
                    .iataCode(upper)
                    .cityName(upper)
                    .countryName("Commercial Hub")
                    .airportType("AIRPORT")
                    .latitude(20.0)
                    .longitude(78.0)
                    .build());
        }

        return scored.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public AirportResultDto getAirportByIataCode(String iataCode) {
        if (iataCode == null) return null;
        String upper = iataCode.trim().toUpperCase();
        AirportResultDto found = IATA_LOOKUP.get(upper);
        if (found != null) {
            return found;
        }
        // Check catalog search if not in primary map
        return AIRPORT_CATALOG.stream()
                .filter(a -> a.getIataCode().equalsIgnoreCase(upper))
                .findFirst()
                .orElse(null);
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

    private List<AirportResultDto> fetchFromAmadeus(String query) {
        try {
            // Fetch OAuth2 Token
            String token = getAmadeusToken();
            if (token == null) return Collections.emptyList();

            String url = amadeusLocationUrl + "?subType=AIRPORT,CITY&keyword=" + query + "&page[limit]=10";
            String json = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (json == null) return Collections.emptyList();
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");

            List<AirportResultDto> results = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    String iata = item.path("iataCode").asText(null);
                    if (iata == null || iata.isBlank()) continue;

                    String name = item.path("name").asText(iata);
                    String subType = item.path("subType").asText("AIRPORT");
                    String city = item.path("address").path("cityName").asText(name);
                    String country = item.path("address").path("countryName").asText("International");
                    Double lat = item.path("geoCode").path("latitude").asDouble(0.0);
                    Double lon = item.path("geoCode").path("longitude").asDouble(0.0);

                    results.add(AirportResultDto.builder()
                            .name(name)
                            .iataCode(iata.toUpperCase())
                            .cityName(city)
                            .countryName(country)
                            .airportType(subType)
                            .latitude(lat != 0.0 ? lat : null)
                            .longitude(lon != 0.0 ? lon : null)
                            .build());
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Amadeus API lookup error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getAmadeusToken() {
        try {
            String form = "grant_type=client_credentials&client_id=" + amadeusClientId + "&client_secret=" + amadeusClientSecret;
            String resp = restClient.post()
                    .uri(amadeusAuthUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            if (resp != null) {
                JsonNode node = objectMapper.readTree(resp);
                return node.path("access_token").asText(null);
            }
        } catch (Exception e) {
            log.warn("Amadeus auth error: {}", e.getMessage());
        }
        return null;
    }
}
