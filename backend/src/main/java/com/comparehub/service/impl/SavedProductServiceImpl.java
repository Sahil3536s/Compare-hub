package com.comparehub.service.impl;

import com.comparehub.dto.SavedProductRequestDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.exception.DuplicateResourceException;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.Product;
import com.comparehub.model.SavedProduct;
import com.comparehub.model.User;
import com.comparehub.repository.ProductRepository;
import com.comparehub.repository.SavedProductRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.SavedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedProductServiceImpl implements SavedProductService {

    private final SavedProductRepository savedProductRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

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

        SavedProduct savedProduct = SavedProduct.builder()
                .user(user)
                .product(product)
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
        return savedProductRepository.findByUserId(userId).stream()
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

    private SavedProductResponseDto mapToDto(SavedProduct sp) {
        return SavedProductResponseDto.builder()
                .id(sp.getId())
                .userId(sp.getUser().getId())
                .productId(sp.getProduct().getId())
                .productName(sp.getProduct().getName())
                .productCategory(sp.getProduct().getCategory())
                .productImageUrl(sp.getProduct().getImageUrl())
                .createdAt(sp.getCreatedAt())
                .build();
    }
}
