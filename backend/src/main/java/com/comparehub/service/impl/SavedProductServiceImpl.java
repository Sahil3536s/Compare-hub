package com.comparehub.service.impl;

import com.comparehub.dto.SavedProductRequestDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.exception.DuplicateResourceException;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.*;
import com.comparehub.repository.*;
import com.comparehub.service.SavedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedProductServiceImpl implements SavedProductService {

    private final SavedProductRepository savedProductRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final MerchantOfferRepository merchantOfferRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;
    private final PriceAlertRepository priceAlertRepository;

    @Override
    @Transactional
    public SavedProductResponseDto saveProduct(SavedProductRequestDto request) {
        if (savedProductRepository.existsByUserIdAndProductId(request.getUserId(), request.getProductId())) {
            throw new DuplicateResourceException("Product is already saved by this user");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        BigDecimal savedPrice = request.getSavedPrice();
        String savedMerchant = request.getSavedMerchant();

        // If price wasn't specified explicitly in request, look up current lowest market price
        if (savedPrice == null) {
            List<MerchantOffer> offers = merchantOfferRepository.findByProductIdAndInStockTrueOrderByPriceAsc(product.getId());
            if (!offers.isEmpty()) {
                savedPrice = offers.get(0).getPrice();
                savedMerchant = offers.get(0).getMerchant();
            } else {
                Optional<ProductPriceHistory> history = productPriceHistoryRepository.findTopByProductIdOrderByRecordedAtDesc(product.getId());
                if (history.isPresent()) {
                    savedPrice = history.get().getPrice();
                    savedMerchant = history.get().getMerchant();
                }
            }
        }

        SavedProduct savedProduct = SavedProduct.builder()
                .user(user)
                .product(product)
                .savedPrice(savedPrice)
                .savedMerchant(savedMerchant)
                .build();

        SavedProduct result = savedProductRepository.save(savedProduct);
        return mapToDto(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedProductResponseDto> getSavedProductsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return savedProductRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeSavedProduct(Long userId, Long productId) {
        if (!savedProductRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Saved product entry not found");
        }
        savedProductRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional
    public void removeSavedProductById(Long userId, Long savedProductId) {
        if (!savedProductRepository.findByIdAndUserId(savedProductId, userId).isPresent()) {
            throw new ResourceNotFoundException("Saved product with id " + savedProductId + " not found for current user");
        }
        savedProductRepository.deleteByIdAndUserId(savedProductId, userId);
    }

    public SavedProductResponseDto mapToDto(SavedProduct sp) {
        Product product = sp.getProduct();
        BigDecimal savedPrice = sp.getSavedPrice();
        String savedMerchant = sp.getSavedMerchant();

        // Retrieve current lowest price from live merchant offers
        BigDecimal currentPrice = null;
        String currentMerchant = savedMerchant;
        String productUrl = null;

        List<MerchantOffer> liveOffers = merchantOfferRepository.findByProductIdAndInStockTrueOrderByPriceAsc(product.getId());
        if (!liveOffers.isEmpty()) {
            MerchantOffer bestOffer = liveOffers.get(0);
            currentPrice = bestOffer.getPrice();
            currentMerchant = bestOffer.getMerchant();
            productUrl = bestOffer.getProductUrl();
        } else {
            // Check historical fallback
            Optional<ProductPriceHistory> latestHistory = productPriceHistoryRepository.findTopByProductIdOrderByRecordedAtDesc(product.getId());
            if (latestHistory.isPresent()) {
                currentPrice = latestHistory.get().getPrice();
                currentMerchant = latestHistory.get().getMerchant();
            } else {
                currentPrice = savedPrice;
            }
        }

        // Calculate verified price drop
        boolean isPriceDropped = false;
        BigDecimal priceDropAmount = BigDecimal.ZERO;
        Double priceDropPercentage = 0.0;
        BigDecimal priceChange = BigDecimal.ZERO;

        if (savedPrice != null && currentPrice != null) {
            priceChange = currentPrice.subtract(savedPrice);
            if (currentPrice.compareTo(savedPrice) < 0) {
                isPriceDropped = true;
                priceDropAmount = savedPrice.subtract(currentPrice);
                if (savedPrice.compareTo(BigDecimal.ZERO) > 0) {
                    priceDropPercentage = priceDropAmount.divide(savedPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();
                }
            }
        }

        // Check active alert for this user and product
        boolean hasActiveAlert = false;
        BigDecimal alertTargetPrice = null;
        Long alertId = null;

        if (sp.getUser() != null) {
            List<PriceAlert> activeAlerts = priceAlertRepository.findByUserIdAndActiveTrue(sp.getUser().getId());
            Optional<PriceAlert> alertMatch = activeAlerts.stream()
                    .filter(a -> a.getProduct().getId().equals(product.getId()))
                    .findFirst();
            if (alertMatch.isPresent()) {
                hasActiveAlert = true;
                alertTargetPrice = alertMatch.get().getTargetPrice();
                alertId = alertMatch.get().getId();
            }
        }

        return SavedProductResponseDto.builder()
                .id(sp.getId())
                .userId(sp.getUser() != null ? sp.getUser().getId() : null)
                .productId(product.getId())
                .productName(product.getName())
                .productBrand(product.getBrand())
                .productCategory(product.getCategory())
                .productImageUrl(product.getImageUrl())
                .savedPrice(savedPrice)
                .savedMerchant(savedMerchant)
                .currentPrice(currentPrice)
                .currentMerchant(currentMerchant)
                .priceChange(priceChange)
                .priceDropAmount(priceDropAmount)
                .priceDropPercentage(priceDropPercentage)
                .isPriceDropped(isPriceDropped)
                .hasActiveAlert(hasActiveAlert)
                .alertTargetPrice(alertTargetPrice)
                .alertId(alertId)
                .productUrl(productUrl)
                .createdAt(sp.getCreatedAt())
                .build();
    }
}
