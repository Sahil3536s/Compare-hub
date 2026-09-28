package com.comparehub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "savings_events", indexes = {
        @Index(name = "idx_savings_events_user_id", columnList = "user_id"),
        @Index(name = "idx_savings_events_event_type", columnList = "event_type"),
        @Index(name = "idx_savings_events_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String category; // "Smartphones", "Laptops", "Flights", "Rides", "Electronics", "General"

    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SavingsEventType eventType; // POTENTIAL, CONFIRMED

    @Column(name = "selected_price", precision = 12, scale = 2)
    private BigDecimal selectedPrice;

    @Column(name = "baseline_price", precision = 12, scale = 2)
    private BigDecimal baselinePrice;

    @NotNull(message = "Saving amount is required")
    @DecimalMin(value = "0.00", message = "Saving amount must be non-negative")
    @Column(name = "saving_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal savingAmount;

    @Column(name = "merchant_or_provider", length = 100)
    private String merchantOrProvider;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (eventType == null) {
            eventType = SavingsEventType.POTENTIAL;
        }
    }
}
