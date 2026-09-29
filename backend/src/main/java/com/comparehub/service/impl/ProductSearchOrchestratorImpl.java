package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.provider.ProductProvider;
import com.comparehub.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSearchOrchestratorImpl implements ProductSearchOrchestrator {

    private final List<ProductProvider> providers;
    private final ProductMatchingService productMatchingService;
    private final ProductNormalizationService normalizationService;
    private final ProductRankingService rankingService;
    private final ComparisonRecommendationService recommendationService;
    private final Executor providerExecutor;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "in", "on", "at", "for", "with", "to", "by", "of", "from", "is", "under"
    );

    private static final List<String> ACCESSORY_INDICATORS = List.of(
            "case", "cover", "protector", "screen protector", "sleeve", "skin", "strap",
            "band", "charger", "cable", "adapter", "holder", "stand", "mount", "stylus",
            "tempered glass", "pouch", "bag for", "casing", "lens protector", "cable cord", "ear tips"
    );

    @Autowired
    public ProductSearchOrchestratorImpl(
            List<ProductProvider> providers,
            ProductMatchingService productMatchingService,
            ProductNormalizationService normalizationService,
            ProductRankingService rankingService,
            ComparisonRecommendationService recommendationService,
            @Qualifier("providerExecutor") Executor providerExecutor) {
        this.providers = providers;
        this.productMatchingService = productMatchingService;
        this.normalizationService = normalizationService;
        this.rankingService = rankingService;
        this.recommendationService = recommendationService;
        this.providerExecutor = providerExecutor;
    }

    public ProductSearchOrchestratorImpl(
            List<ProductProvider> providers,
            ProductMatchingService productMatchingService,
            ProductNormalizationService normalizationService,
            ProductRankingService rankingService,
            ComparisonRecommendationService recommendationService) {
        this(providers, productMatchingService, normalizationService, rankingService, recommendationService, ForkJoinPool.commonPool());
    }

    @Override
    @Cacheable(
            value = "product-comparisons",
            key = "'search:' + T(java.lang.String).valueOf(#request.query).toLowerCase().trim() + ':' + #request.page + ':' + #request.pageSize + ':' + #request.merchant + ':' + #request.brand + ':' + #request.category + ':' + #request.minPrice + ':' + #request.maxPrice + ':' + #request.inStockOnly + ':' + #request.sortBy + ':' + #request.minRating + ':' + #request.delivery + ':' + #request.ram + ':' + #request.storage + ':' + #request.attributeFilters.toString()",
            unless = "#result == null || #result.offers.isEmpty()"
    )
    public ProductComparisonResponseDto search(ProductSearchRequestDto request) {
        String rawQuery = request.getQuery() != null ? request.getQuery().trim() : "";
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? Math.min(request.getPageSize(), 100) : 20;

        log.info("ProductSearchOrchestrator executing search for query: '{}', page: {}, pageSize: {}, providers: {}",
                rawQuery, page, pageSize, providers.size());

        List<String> failedProviders = new CopyOnWriteArrayList<>();
        List<String> successfulProviders = new CopyOnWriteArrayList<>();

        // 1. Concurrently call every enabled product provider with resilience
        List<CompletableFuture<List<NormalizedProductOfferDto>>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<NormalizedProductOfferDto> results = provider.searchProducts(rawQuery, page, pageSize);
                        if (results != null) {
                            successfulProviders.add(provider.getProviderName());
                            return results;
                        }
                        successfulProviders.add(provider.getProviderName());
                        return List.<NormalizedProductOfferDto>of();
                    } catch (Exception e) {
                        log.error("Provider '{}' failed during search: {}. Continuing with remaining providers.",
                                provider.getProviderName(), e.getMessage());
                        failedProviders.add(provider.getProviderName());
                        return List.<NormalizedProductOfferDto>of();
                    }
                }, providerExecutor)
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("Provider '{}' timed out or failed: {}", provider.getProviderName(), ex.getMessage());
                    failedProviders.add(provider.getProviderName());
                    return List.of();
                }))
                .toList();

        List<NormalizedProductOfferDto> rawOffers = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new));

        // 2. Data Normalization
        List<NormalizedProductOfferDto> normalized = normalizationService.normalizeOffers(rawOffers);

        // 3. Generic Relevance Engine & Generic Accessory Downranking
        for (NormalizedProductOfferDto offer : normalized) {
            double relevance = calculateGenericRelevance(offer, rawQuery);
            offer.setRelevanceScore(relevance);
        }

        // 4. Product Matching & Canonical Offer Grouping
        List<NormalizedProductOfferDto> matchedOffers = productMatchingService.enrichWithMatching(normalized);

        // 5. Dynamic Facet Extraction (Categories, Brands, Merchants, and dynamic attributes)
        List<String> availableCategories = matchedOffers.stream()
                .map(NormalizedProductOfferDto::getCategory)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank() && !"General".equalsIgnoreCase(s))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        List<String> availableBrands = matchedOffers.stream()
                .map(NormalizedProductOfferDto::getBrand)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank() && !"Generic".equalsIgnoreCase(s))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        List<String> availableMerchants = matchedOffers.stream()
                .map(NormalizedProductOfferDto::getMerchant)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        Map<String, List<String>> dynamicFilters = extractDynamicAttributeFacets(matchedOffers);

        // 6. Apply Generic User Filters
        List<NormalizedProductOfferDto> filtered = matchedOffers.stream()
                .filter(offer -> matchesFilters(offer, request))
                .collect(Collectors.toList());

        // 7. Relevance / Multi-factor Ranking & Sorting
        String sortBy = request.getSortBy() != null ? request.getSortBy().trim().toLowerCase(Locale.ROOT) : "best";
        List<NormalizedProductOfferDto> ranked = rankAndSort(filtered, sortBy, rawQuery);

        // 8. Find Overall Cheapest Price & Merchant
        BigDecimal cheapestPrice = null;
        String cheapestMerchant = null;
        if (!ranked.isEmpty()) {
            NormalizedProductOfferDto cheapest = ranked.stream()
                    .min(Comparator.comparing(o -> o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice()))
                    .orElse(ranked.get(0));

            cheapestPrice = cheapest.getEffectivePrice() != null ? cheapest.getEffectivePrice() : cheapest.getPrice();
            cheapestMerchant = cheapest.getMerchant();
        }

        // 9. AI Recommendation & Ranking Explanation
        AiRecommendationDto recommendation = recommendationService.recommendProducts(ranked);
        var rankingSummary = rankingService.getRankingSummary(ranked);



        // 11. Pagination Calculation
        int totalOffers = ranked.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalOffers / pageSize));
        int fromIndex = Math.min((page - 1) * pageSize, totalOffers);
        int toIndex = Math.min(fromIndex + pageSize, totalOffers);
        List<NormalizedProductOfferDto> pagedOffers = new ArrayList<>(ranked.subList(fromIndex, toIndex));
        boolean hasMore = toIndex < totalOffers;

        // 12. Status and Partial Success Handling
        String status = "SUCCESS";
        String statusMessage = null;
        if (totalOffers == 0) {
            if (successfulProviders.isEmpty() && !failedProviders.isEmpty()) {
                status = "FAILED";
                statusMessage = "All providers failed to return results.";
            } else {
                // No offers but at least one provider succeeded – treat as honest success.
                status = "SUCCESS";
                statusMessage = "No products matched the query, but providers responded successfully.";
            }
        } else if (!failedProviders.isEmpty() && !successfulProviders.isEmpty()) {
            status = "PARTIAL_SUCCESS";
            statusMessage = "Some stores couldn't be reached. Showing available results.";
        } else if (successfulProviders.isEmpty() && !failedProviders.isEmpty()) {
            status = "FAILED";
            statusMessage = "All store providers are currently unavailable. Please try again.";
        }


        return ProductComparisonResponseDto.builder()
                .query(rawQuery)
                .totalOffers(totalOffers)
                .cheapestPrice(cheapestPrice)
                .cheapestMerchant(cheapestMerchant)
                .offers(pagedOffers)
                .aiRecommendation(recommendation)
                .rankingSummary(rankingSummary)
                .status(status)
                .statusMessage(statusMessage)
                .successfulProviders(new ArrayList<>(successfulProviders))
                .failedProviders(new ArrayList<>(failedProviders))
                .providerDiagnostics(new ArrayList<>())
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .hasMore(hasMore)
                .dynamicFilters(dynamicFilters)
                .availableCategories(availableCategories)
                .availableBrands(availableBrands)
                .availableMerchants(availableMerchants)
                .build();
    }

    @Override
    public List<String> getSearchSuggestions(String prefix) {
        if (prefix == null || prefix.trim().length() < 2) {
            return List.of();
        }
        String clean = prefix.trim().toLowerCase(Locale.ROOT);
        Set<String> suggestions = new LinkedHashSet<>();

        for (ProductProvider provider : providers) {
            try {
                List<NormalizedProductOfferDto> offers = provider.searchProducts(clean, 1, 5);
                if (offers != null) {
                    for (NormalizedProductOfferDto offer : offers) {
                        if (offer.getProductName() != null && offer.getProductName().toLowerCase().contains(clean)) {
                            suggestions.add(cleanTitleForSuggestion(offer.getProductName()));
                        }
                        if (offer.getBrand() != null && offer.getBrand().toLowerCase().startsWith(clean)) {
                            suggestions.add(offer.getBrand());
                        }
                        if (offer.getCategory() != null && offer.getCategory().toLowerCase().contains(clean)) {
                            suggestions.add(capitalizeWords(offer.getCategory().replace("-", " ")));
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (suggestions.size() >= 8) break;
        }

        return suggestions.stream().limit(8).toList();
    }

    private double calculateGenericRelevance(NormalizedProductOfferDto offer, String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return 100.0;
        }

        String queryLower = rawQuery.toLowerCase(Locale.ROOT).trim();
        String titleLower = offer.getTitle() != null ? offer.getTitle().toLowerCase(Locale.ROOT) : "";
        String brandLower = offer.getBrand() != null ? offer.getBrand().toLowerCase(Locale.ROOT) : "";
        String categoryLower = offer.getCategory() != null ? offer.getCategory().toLowerCase(Locale.ROOT) : "";

        double score = 0.0;

        // 1. Exact phrase match
        if (titleLower.contains(queryLower)) {
            score += 45.0;
        }

        // 2. Token overlap ratio
        List<String> queryTokens = Arrays.stream(queryLower.split("\\s+"))
                .map(String::trim)
                .filter(t -> !t.isEmpty() && !STOP_WORDS.contains(t))
                .toList();

        if (!queryTokens.isEmpty()) {
            long matchedTokens = queryTokens.stream()
                    .filter(token -> titleLower.contains(token) || brandLower.contains(token) || categoryLower.contains(token))
                    .count();

            double overlapRatio = (double) matchedTokens / queryTokens.size();
            score += (overlapRatio * 35.0);

            // Bonus if all tokens match
            if (matchedTokens == queryTokens.size()) {
                score += 10.0;
            }
        }

        // 3. Brand match
        if (!brandLower.isEmpty() && !brandLower.equals("generic") && queryLower.contains(brandLower)) {
            score += 15.0;
        }

        // 4. Category match
        if (!categoryLower.isEmpty() && queryLower.contains(categoryLower.replace("-", " "))) {
            score += 10.0;
        }

        // 5. Model / SKU match
        if (offer.getModel() != null && !offer.getModel().isBlank() && queryLower.contains(offer.getModel().toLowerCase(Locale.ROOT))) {
            score += 10.0;
        }

        // 6. Generic Accessory Downranking (Part 8)
        boolean queryWantsAccessory = ACCESSORY_INDICATORS.stream().anyMatch(queryLower::contains);
        if (!queryWantsAccessory) {
            boolean isOfferAccessory = ACCESSORY_INDICATORS.stream().anyMatch(titleLower::contains)
                    || categoryLower.contains("accessories");
            if (isOfferAccessory) {
                score = Math.max(5.0, score - 35.0);
            }
        }

        return Math.min(100.0, Math.max(0.0, Math.round(score * 10.0) / 10.0));
    }

    private boolean matchesFilters(NormalizedProductOfferDto offer, ProductSearchRequestDto req) {
        // Merchant filter
        if (req.getMerchant() != null && !req.getMerchant().isBlank() && !"all".equalsIgnoreCase(req.getMerchant())) {
            if (!offer.getMerchant().equalsIgnoreCase(req.getMerchant().trim())) {
                return false;
            }
        }

        // Brand filter
        if (req.getBrand() != null && !req.getBrand().isBlank() && !"all".equalsIgnoreCase(req.getBrand())) {
            if (!offer.getBrand().equalsIgnoreCase(req.getBrand().trim())) {
                return false;
            }
        }

        // Category filter
        if (req.getCategory() != null && !req.getCategory().isBlank() && !"all".equalsIgnoreCase(req.getCategory()) && !"all categories".equalsIgnoreCase(req.getCategory())) {
            if (!offer.getCategory().equalsIgnoreCase(req.getCategory().trim())) {
                return false;
            }
        }

        // Min price
        if (req.getMinPrice() != null && offer.getPrice().compareTo(req.getMinPrice()) < 0) {
            return false;
        }

        // Max price
        if (req.getMaxPrice() != null && offer.getPrice().compareTo(req.getMaxPrice()) > 0) {
            return false;
        }

        // In-stock
        if (Boolean.TRUE.equals(req.getInStockOnly()) && Boolean.FALSE.equals(offer.getInStock())) {
            return false;
        }

        // Minimum rating
        if (req.getMinRating() != null && req.getMinRating() > 0) {
            if (offer.getRating() == null || offer.getRating() < req.getMinRating()) {
                return false;
            }
        }

        // Delivery speed
        if (req.getDelivery() != null && !req.getDelivery().isBlank() && !"all".equalsIgnoreCase(req.getDelivery())) {
            String deliv = offer.getDelivery() != null ? offer.getDelivery().toLowerCase(Locale.ROOT) : "";
            String filter = req.getDelivery().toLowerCase(Locale.ROOT).trim();
            if ("same_day".equals(filter) || "sameday".equals(filter)) {
                if (!deliv.contains("same day") && !deliv.contains("today")) return false;
            } else if ("next_day".equals(filter) || "tomorrow".equals(filter)) {
                if (!deliv.contains("tomorrow") && !deliv.contains("1 day") && !deliv.contains("same day") && !deliv.contains("today")) return false;
            } else if ("express".equals(filter)) {
                if (!deliv.contains("express") && !deliv.contains("same day") && !deliv.contains("tomorrow") && !deliv.contains("today")) return false;
            } else if ("free".equals(filter)) {
                if (!deliv.contains("free")) return false;
            } else {
                if (!deliv.contains(filter)) return false;
            }
        }

        // Backward compatibility: RAM
        if (req.getRam() != null && !req.getRam().isBlank() && !"all".equalsIgnoreCase(req.getRam())) {
            String cleanRam = req.getRam().toLowerCase().replaceAll("\\s+", "");
            String offerRam = offer.getAttributeValue("RAM") != null ? offer.getAttributeValue("RAM").toLowerCase().replaceAll("\\s+", "") : "";
            String title = offer.getTitle() != null ? offer.getTitle().toLowerCase() : "";
            if (!offerRam.contains(cleanRam) && !title.contains(cleanRam)) {
                return false;
            }
        }

        // Backward compatibility: Storage
        if (req.getStorage() != null && !req.getStorage().isBlank() && !"all".equalsIgnoreCase(req.getStorage())) {
            String cleanStorage = req.getStorage().toLowerCase().replaceAll("\\s+", "");
            String offerStorage = offer.getAttributeValue("Storage") != null ? offer.getAttributeValue("Storage").toLowerCase().replaceAll("\\s+", "") : "";
            String title = offer.getTitle() != null ? offer.getTitle().toLowerCase() : "";
            if (!offerStorage.contains(cleanStorage) && !title.contains(cleanStorage)) {
                return false;
            }
        }

        // Dynamic attribute filters (e.g. Size, Color, Capacity, Energy Rating)
        if (req.getAttributeFilters() != null && !req.getAttributeFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : req.getAttributeFilters().entrySet()) {
                String filterKey = entry.getKey();
                String filterVal = entry.getValue();
                if (filterVal != null && !filterVal.isBlank() && !"all".equalsIgnoreCase(filterVal)) {
                    String actualVal = offer.getAttributeValue(filterKey);
                    String title = offer.getTitle() != null ? offer.getTitle().toLowerCase(Locale.ROOT) : "";
                    String target = filterVal.trim().toLowerCase(Locale.ROOT);

                    boolean match = (actualVal != null && actualVal.toLowerCase(Locale.ROOT).contains(target))
                            || title.contains(target);
                    if (!match) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private List<NormalizedProductOfferDto> rankAndSort(List<NormalizedProductOfferDto> offers, String sortBy, String query) {
        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        switch (sortBy) {
            case "relevance":
                List<NormalizedProductOfferDto> sortedRel = new ArrayList<>(offers);
                sortedRel.sort(Comparator.comparingDouble((NormalizedProductOfferDto o) -> o.getRelevanceScore() != null ? o.getRelevanceScore() : 0.0).reversed()
                        .thenComparing(o -> o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice()));
                return rankingService.rankAndMarkCheapest(sortedRel, "best");

            case "price_asc":
            case "cheapest":
                return rankingService.rankAndMarkCheapest(offers, "price_asc");

            case "price_desc":
                List<NormalizedProductOfferDto> sortedDesc = new ArrayList<>(offers);
                sortedDesc.sort(Comparator.comparing((NormalizedProductOfferDto o) -> o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice(), Comparator.reverseOrder())
                        .thenComparing(o -> o.getRating() != null ? o.getRating() : 0.0, Comparator.reverseOrder()));
                return rankingService.rankAndMarkCheapest(sortedDesc, "price_desc");

            case "rating":
            case "highest_rated":
                return rankingService.rankAndMarkCheapest(offers, "rating");

            case "fastest_delivery":
            case "delivery":
                return rankingService.rankAndMarkCheapest(offers, "fastest_delivery");

            case "discount":
                return rankingService.rankAndMarkCheapest(offers, "discount");

            case "best":
            case "best_value":
            default:
                List<NormalizedProductOfferDto> ranked = new ArrayList<>(rankingService.rankAndMarkCheapest(offers, "best"));
                if (query != null && !query.isBlank()) {
                    // Stable sort prioritizing high-relevance matches
                    ranked.sort((a, b) -> {
                        double relA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 50.0;
                        double relB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 50.0;
                        // If relevance differs by more than 20 points, relevance takes priority
                        if (Math.abs(relA - relB) >= 20.0) {
                            return Double.compare(relB, relA);
                        }
                        double scoreA = a.getFinalScore() != null ? a.getFinalScore() : 0.0;
                        double scoreB = b.getFinalScore() != null ? b.getFinalScore() : 0.0;
                        return Double.compare(scoreB, scoreA);
                    });
                }
                return ranked;
        }
    }

    private Map<String, List<String>> extractDynamicAttributeFacets(List<NormalizedProductOfferDto> offers) {
        Map<String, List<String>> facets = new LinkedHashMap<>();
        if (offers == null || offers.isEmpty()) return facets;

        Map<String, Set<String>> valuesByKey = new LinkedHashMap<>();
        Map<String, Integer> countByKey = new HashMap<>();

        for (NormalizedProductOfferDto offer : offers) {
            if (offer.getAttributes() != null) {
                for (Map.Entry<String, String> entry : offer.getAttributes().entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();
                    if (k != null && v != null && !v.isBlank()) {
                        String cleanKey = k.trim();
                        if (cleanKey.equalsIgnoreCase("Brand") || cleanKey.equalsIgnoreCase("Category")
                                || cleanKey.equalsIgnoreCase("canonicalKey") || cleanKey.equalsIgnoreCase("ram")
                                || cleanKey.equalsIgnoreCase("storage") || cleanKey.equalsIgnoreCase("color")) {
                            continue;
                        }
                        valuesByKey.computeIfAbsent(cleanKey, key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(v.trim());
                        countByKey.put(cleanKey, countByKey.getOrDefault(cleanKey, 0) + 1);
                    }
                }
            }
        }

        // Only include attribute facets if at least 2 products have it
        for (Map.Entry<String, Set<String>> entry : valuesByKey.entrySet()) {
            String key = entry.getKey();
            int count = countByKey.getOrDefault(key, 0);
            if (count >= 2 && entry.getValue().size() >= 2 && entry.getValue().size() <= 15) {
                facets.put(key, new ArrayList<>(entry.getValue()));
            }
        }

        return facets;
    }

    private String cleanTitleForSuggestion(String title) {
        if (title == null) return "";
        return title.replaceAll("\\s*\\([^)]*\\)", "")
                .replaceAll("\\s*-[^-]*$", "")
                .trim();
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase(Locale.ROOT))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
