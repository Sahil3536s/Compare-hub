package com.comparehub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_ranking_preferences", indexes = {
    @Index(name = "idx_user_ranking_prefs_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRankingPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String preset = "BALANCED";

    // Product Weights
    @Column(name = "product_price", nullable = false)
    @Builder.Default
    private Integer productPrice = 40;

    @Column(name = "product_rating", nullable = false)
    @Builder.Default
    private Integer productRating = 20;

    @Column(name = "product_discount", nullable = false)
    @Builder.Default
    private Integer productDiscount = 15;

    @Column(name = "product_delivery", nullable = false)
    @Builder.Default
    private Integer productDelivery = 15;

    @Column(name = "product_reliability", nullable = false)
    @Builder.Default
    private Integer productReliability = 10;

    // Flight Weights
    @Column(name = "flight_price", nullable = false)
    @Builder.Default
    private Integer flightPrice = 50;

    @Column(name = "flight_duration", nullable = false)
    @Builder.Default
    private Integer flightDuration = 30;

    @Column(name = "flight_stops", nullable = false)
    @Builder.Default
    private Integer flightStops = 20;

    // Ride Weights
    @Column(name = "ride_fare", nullable = false)
    @Builder.Default
    private Integer rideFare = 60;

    @Column(name = "ride_eta", nullable = false)
    @Builder.Default
    private Integer rideEta = 40;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
