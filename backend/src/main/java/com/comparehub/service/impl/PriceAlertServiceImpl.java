package com.comparehub.service.impl;

import com.comparehub.dto.PriceAlertRequestDto;
import com.comparehub.dto.PriceAlertResponseDto;
import com.comparehub.exception.DuplicateResourceException;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.PriceAlert;
import com.comparehub.model.Product;
import com.comparehub.model.User;
import com.comparehub.repository.PriceAlertRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceAlertServiceImpl implements PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public PriceAlertResponseDto createPriceAlert(PriceAlertRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Product product = productRepository.findById(request.getProductId())
                .orElseGet(() -> {
                    if (request.getProductName() != null && !request.getProductName().isBlank()) {
                        List<Product> matches = productRepository.findByNameContainingIgnoreCase(request.getProductName());
                        if (!matches.isEmpty()) {
                            return matches.get(0);
                        }
                        return productRepository.save(Product.builder()
                                .name(request.getProductName())
                                .category("General")
                                .build());
                    }
                    List<Product> all = productRepository.findAll();
                    if (!all.isEmpty()) {
                        return all.get(0);
                    }
                    return productRepository.save(Product.builder()
                            .name("Sample Product")
                            .category("General")
                            .build());
                });

        // Prevent accidental duplicate alerts for the same product
        List<PriceAlert> existingActive = priceAlertRepository.findByUserIdAndActiveTrue(request.getUserId());
        boolean alreadyExists = existingActive.stream()
                .anyMatch(a -> a.getProduct().getId().equals(product.getId()));
        if (alreadyExists) {
            throw new DuplicateResourceException("An active price alert already exists for " + product.getName() + ".");
        }

        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .product(product)
                .targetPrice(request.getTargetPrice())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        PriceAlert savedAlert = priceAlertRepository.save(alert);
        return mapToDto(savedAlert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceAlertResponseDto> getAlertsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return priceAlertRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceAlertResponseDto> getAllActiveAlerts() {
        return priceAlertRepository.findAll().stream()
                .filter(PriceAlert::getActive)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleAlertStatus(Long alertId, boolean active) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Price alert not found with id: " + alertId));
        alert.setActive(active);
        priceAlertRepository.save(alert);
    }

    @Override
    @Transactional
    public void toggleAlertStatus(Long userId, Long alertId, boolean active) {
        PriceAlert alert = priceAlertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Price alert not found with id: " + alertId + " for user: " + userId));
        alert.setActive(active);
        priceAlertRepository.save(alert);
    }

    @Override
    @Transactional
    public void deleteAlert(Long alertId) {
        if (!priceAlertRepository.existsById(alertId)) {
            throw new ResourceNotFoundException("Price alert not found with id: " + alertId);
        }
        priceAlertRepository.deleteById(alertId);
    }

    @Override
    @Transactional
    public void deleteAlert(Long userId, Long alertId) {
        if (!priceAlertRepository.findByIdAndUserId(alertId, userId).isPresent()) {
            throw new ResourceNotFoundException("Price alert not found with id: " + alertId + " for user: " + userId);
        }
        priceAlertRepository.deleteByIdAndUserId(alertId, userId);
    }

    private PriceAlertResponseDto mapToDto(PriceAlert alert) {
        return PriceAlertResponseDto.builder()
                .id(alert.getId())
                .userId(alert.getUser().getId())
                .userName(alert.getUser().getName())
                .userEmail(alert.getUser().getEmail())
                .productId(alert.getProduct().getId())
                .productName(alert.getProduct().getName())
                .targetPrice(alert.getTargetPrice())
                .active(alert.getActive())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
