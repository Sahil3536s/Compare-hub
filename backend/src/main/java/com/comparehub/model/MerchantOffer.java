package com.comparehub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "merchant_offers", indexes = {
    @Index(name = "idx_merchant_offers_product_id", columnList = "product_id"),
    @Index(name = "idx_merchant_offers_price", columnList = "price")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank(message = "Merchant name is required")
    @Size(max = 100, message = "Merchant name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String merchant;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @NotBlank(message = "Product URL is required")
    @Size(max = 2000, message = "Product URL must not exceed 2000 characters")
    @Column(name = "product_url", nullable = false, length = 2000)
    private String productUrl;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private Boolean inStock = true;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Size(max = 255, message = "Delivery text must not exceed 255 characters")
    @Column(name = "delivery_text", length = 255)
    private String deliveryText;

    @Column(name = "last_updated", nullable = false)
    @Builder.Default
    private Instant lastUpdated = Instant.now();

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        lastUpdated = Instant.now();
    }
}
