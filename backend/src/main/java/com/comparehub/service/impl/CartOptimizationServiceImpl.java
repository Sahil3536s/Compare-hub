package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.service.CartOptimizationService;
import com.comparehub.service.OfferEligibilityService;
import com.comparehub.service.ProductComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartOptimizationServiceImpl implements CartOptimizationService {

    private final ProductComparisonService productComparisonService;
    private final OfferEligibilityService offerEligibilityService;

    @Override
    public CartOptimizationResponseDto optimizeCart(CartOptimizationRequestDto request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return CartOptimizationResponseDto.builder()
                    .strategy("MINIMIZE_PRICE")
                    .totalItemsRequested(0)
                    .totalItemsMatched(0)
                    .explanation("Empty cart provided.")
                    .build();
        }

        String strategy = request.getStrategy() != null ? request.getStrategy().trim().toUpperCase() : "MINIMIZE_PRICE";
        List<CartItemDto> items = request.getItems();

        // 1. Fetch & match offers for each item in the cart
        Map<String, List<CartItemOfferDto>> itemCandidateOffers = new LinkedHashMap<>();
        int matchedCount = 0;

        for (CartItemDto item : items) {
            String query = item.getName() != null ? item.getName().trim() : "";
            if (query.isBlank()) continue;

            ProductComparisonResponseDto comparison = productComparisonService.compareProducts(
                    query, null, null, null, null, item.getMaxPrice(), true, "best");

            List<CartItemOfferDto> candidateOffers = new ArrayList<>();
            if (comparison != null && comparison.getOffers() != null) {
                // Group by merchant and pick the best matching offer per merchant
                Map<String, NormalizedProductOfferDto> bestByMerchant = new HashMap<>();
                for (NormalizedProductOfferDto off : comparison.getOffers()) {
                    String m = off.getMerchant();
                    if (m != null && !bestByMerchant.containsKey(m)) {
                        bestByMerchant.put(m, off);
                    }
                }

                int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
                for (Map.Entry<String, NormalizedProductOfferDto> entry : bestByMerchant.entrySet()) {
                    NormalizedProductOfferDto o = entry.getValue();
                    BigDecimal unitPrice = o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice();
                    BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

                    candidateOffers.add(CartItemOfferDto.builder()
                            .itemName(item.getName())
                            .matchedProductName(o.getProductName())
                            .merchant(o.getMerchant())
                            .quantity(qty)
                            .unitPrice(unitPrice)
                            .totalPrice(totalPrice)
                            .productUrl(o.getProductUrl())
                            .imageUrl(o.getImageUrl())
                            .deliveryText(o.getDelivery())
                            .rating(o.getRating())
                            .build());
                }
            }

            if (!candidateOffers.isEmpty()) {
                matchedCount++;
            }
            itemCandidateOffers.put(item.getName(), candidateOffers);
        }

        // 2. Discover Single-Store Options
        Set<String> allMerchants = itemCandidateOffers.values().stream()
                .flatMap(Collection::stream)
                .map(CartItemOfferDto::getMerchant)
                .collect(Collectors.toSet());

        List<CartPlanDto> singleStorePlans = new ArrayList<>();

        for (String merchant : allMerchants) {
            boolean hasAllItems = true;
            List<CartItemOfferDto> storeItems = new ArrayList<>();

            for (CartItemDto item : items) {
                List<CartItemOfferDto> candidates = itemCandidateOffers.getOrDefault(item.getName(), Collections.emptyList());
                Optional<CartItemOfferDto> match = candidates.stream()
                        .filter(c -> merchant.equalsIgnoreCase(c.getMerchant()))
                        .findFirst();

                if (match.isPresent()) {
                    storeItems.add(match.get());
                } else {
                    hasAllItems = false;
                    break;
                }
            }

            if (hasAllItems && !storeItems.isEmpty()) {
                BigDecimal subtotal = storeItems.stream()
                        .map(CartItemOfferDto::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal deliveryFee = offerEligibilityService.determineDeliveryFee(merchant, "Standard", subtotal);
                BigDecimal platformFee = offerEligibilityService.determinePlatformFee(merchant);
                BigDecimal grandTotal = subtotal.add(deliveryFee).add(platformFee);

                CartMerchantOrderDto order = CartMerchantOrderDto.builder()
                        .merchant(merchant)
                        .items(storeItems)
                        .itemsSubtotal(subtotal)
                        .deliveryFee(deliveryFee)
                        .platformFee(platformFee)
                        .merchantTotal(grandTotal)
                        .build();

                singleStorePlans.add(CartPlanDto.builder()
                        .planType("SINGLE_STORE")
                        .title("All-in-One: " + merchant)
                        .description("Order all items together from " + merchant + " in 1 single package.")
                        .merchantOrders(List.of(order))
                        .totalProductsCost(subtotal)
                        .totalDeliveryFees(deliveryFee)
                        .totalPlatformFees(platformFee)
                        .grandTotal(grandTotal)
                        .totalOrders(1)
                        .build());
            }
        }

        singleStorePlans.sort(Comparator.comparing(CartPlanDto::getGrandTotal));
        CartPlanDto cheapestSingleStore = singleStorePlans.isEmpty() ? null : singleStorePlans.get(0);

        // 3. Compute Optimized Mixed-Store Combination
        CartPlanDto optimizedMixedPlan = findOptimalMixedPlan(items, itemCandidateOffers);

        // 4. Calculate Savings & Select Recommendation
        BigDecimal savings = BigDecimal.ZERO;
        if (cheapestSingleStore != null && optimizedMixedPlan != null) {
            if (cheapestSingleStore.getGrandTotal().compareTo(optimizedMixedPlan.getGrandTotal()) > 0) {
                savings = cheapestSingleStore.getGrandTotal().subtract(optimizedMixedPlan.getGrandTotal());
                optimizedMixedPlan.setSavingsVsBaseline(savings);
            }
        }

        CartPlanDto recommendedPlan;
        if ("MINIMIZE_DELIVERIES".equals(strategy)) {
            recommendedPlan = cheapestSingleStore != null ? cheapestSingleStore : optimizedMixedPlan;
        } else {
            recommendedPlan = (optimizedMixedPlan != null && (cheapestSingleStore == null || optimizedMixedPlan.getGrandTotal().compareTo(cheapestSingleStore.getGrandTotal()) <= 0))
                    ? optimizedMixedPlan
                    : cheapestSingleStore;
        }

        if (recommendedPlan != null) {
            recommendedPlan.setIsRecommended(true);
        }

        String explanation = String.format(
                "Evaluated %d shopping items across %d providers. %s",
                items.size(),
                allMerchants.size(),
                savings.compareTo(BigDecimal.ZERO) > 0
                        ? "Splitting into " + (optimizedMixedPlan != null ? optimizedMixedPlan.getTotalOrders() : 1) + " orders saves ₹" + savings + " vs single-store purchase."
                        : "Single-store purchasing provides the lowest overall cost with 1 single delivery."
        );

        return CartOptimizationResponseDto.builder()
                .strategy(strategy)
                .totalItemsRequested(items.size())
                .totalItemsMatched(matchedCount)
                .singleStorePlans(singleStorePlans)
                .cheapestSingleStore(cheapestSingleStore)
                .optimizedMixedPlan(optimizedMixedPlan)
                .recommendedPlan(recommendedPlan)
                .estimatedSavings(savings)
                .explanation(explanation)
                .build();
    }

    private CartPlanDto findOptimalMixedPlan(
            List<CartItemDto> items,
            Map<String, List<CartItemOfferDto>> itemCandidateOffers) {

        List<List<CartItemOfferDto>> candidateLists = new ArrayList<>();
        for (CartItemDto item : items) {
            List<CartItemOfferDto> list = itemCandidateOffers.getOrDefault(item.getName(), Collections.emptyList());
            if (list.isEmpty()) {
                return null; // Cannot fulfill cart if an item has zero offers
            }
            candidateLists.add(list);
        }

        // Branch-and-bound search for minimum cost combination
        List<CartItemOfferDto> bestCombination = new ArrayList<>();
        BigDecimal[] minCost = new BigDecimal[]{BigDecimal.valueOf(Double.MAX_VALUE)};

        searchCombinations(candidateLists, 0, new ArrayList<>(), bestCombination, minCost);

        if (bestCombination.isEmpty()) {
            return null;
        }

        // Group the best combination by merchant
        Map<String, List<CartItemOfferDto>> grouped = bestCombination.stream()
                .collect(Collectors.groupingBy(CartItemOfferDto::getMerchant));

        List<CartMerchantOrderDto> merchantOrders = new ArrayList<>();
        BigDecimal totalProducts = BigDecimal.ZERO;
        BigDecimal totalDelivery = BigDecimal.ZERO;
        BigDecimal totalPlatform = BigDecimal.ZERO;

        for (Map.Entry<String, List<CartItemOfferDto>> entry : grouped.entrySet()) {
            String m = entry.getKey();
            List<CartItemOfferDto> mItems = entry.getValue();

            BigDecimal subtotal = mItems.stream()
                    .map(CartItemOfferDto::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal delivery = offerEligibilityService.determineDeliveryFee(m, "Standard", subtotal);
            BigDecimal platform = offerEligibilityService.determinePlatformFee(m);
            BigDecimal mTotal = subtotal.add(delivery).add(platform);

            totalProducts = totalProducts.add(subtotal);
            totalDelivery = totalDelivery.add(delivery);
            totalPlatform = totalPlatform.add(platform);

            merchantOrders.add(CartMerchantOrderDto.builder()
                    .merchant(m)
                    .items(mItems)
                    .itemsSubtotal(subtotal)
                    .deliveryFee(delivery)
                    .platformFee(platform)
                    .merchantTotal(mTotal)
                    .build());
        }

        BigDecimal grandTotal = totalProducts.add(totalDelivery).add(totalPlatform);
        String merchantNames = grouped.keySet().stream().sorted().collect(Collectors.joining(" + "));

        return CartPlanDto.builder()
                .planType("OPTIMIZED_MIXED")
                .title("Smart Split: " + merchantNames)
                .description("Multi-store optimized combination split across " + merchantOrders.size() + " separate orders.")
                .merchantOrders(merchantOrders)
                .totalProductsCost(totalProducts)
                .totalDeliveryFees(totalDelivery)
                .totalPlatformFees(totalPlatform)
                .grandTotal(grandTotal)
                .totalOrders(merchantOrders.size())
                .build();
    }

    private void searchCombinations(
            List<List<CartItemOfferDto>> candidateLists,
            int itemIndex,
            List<CartItemOfferDto> current,
            List<CartItemOfferDto> bestCombination,
            BigDecimal[] minCost) {

        if (itemIndex == candidateLists.size()) {
            BigDecimal total = calculateTotalCostWithFees(current);
            if (total.compareTo(minCost[0]) < 0) {
                minCost[0] = total;
                bestCombination.clear();
                bestCombination.addAll(current);
            }
            return;
        }

        // Bounding: current products cost without fees
        BigDecimal currentProductsCost = current.stream()
                .map(CartItemOfferDto::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (currentProductsCost.compareTo(minCost[0]) >= 0) {
            return; // Prune branch
        }

        for (CartItemOfferDto offer : candidateLists.get(itemIndex)) {
            current.add(offer);
            searchCombinations(candidateLists, itemIndex + 1, current, bestCombination, minCost);
            current.remove(current.size() - 1);
        }
    }

    private BigDecimal calculateTotalCostWithFees(List<CartItemOfferDto> items) {
        Map<String, BigDecimal> merchantSubtotals = new HashMap<>();
        for (CartItemOfferDto item : items) {
            merchantSubtotals.merge(item.getMerchant(), item.getTotalPrice(), BigDecimal::add);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : merchantSubtotals.entrySet()) {
            String m = entry.getKey();
            BigDecimal subtotal = entry.getValue();
            BigDecimal delivery = offerEligibilityService.determineDeliveryFee(m, "Standard", subtotal);
            BigDecimal platform = offerEligibilityService.determinePlatformFee(m);
            total = total.add(subtotal).add(delivery).add(platform);
        }
        return total;
    }
}
