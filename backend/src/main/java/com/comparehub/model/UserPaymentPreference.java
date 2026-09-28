package com.comparehub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_payment_preferences", indexes = {
    @Index(name = "idx_user_payment_prefs_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPaymentPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_bank", nullable = false, length = 100)
    @Builder.Default
    private String preferredBank = "ALL";

    @Column(name = "preferred_card_type", nullable = false, length = 50)
    @Builder.Default
    private String preferredCardType = "ALL";

    @Column(name = "has_upi", nullable = false)
    @Builder.Default
    private Boolean hasUpi = true;

    @Column(name = "has_wallet", nullable = false)
    @Builder.Default
    private Boolean hasWallet = false;

    @Column(name = "preferred_wallet", nullable = false, length = 100)
    @Builder.Default
    private String preferredWallet = "NONE";

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
