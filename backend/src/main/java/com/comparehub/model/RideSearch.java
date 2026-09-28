package com.comparehub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ride_searches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank(message = "Pickup location is required")
    @Size(max = 255, message = "Pickup location must not exceed 255 characters")
    @Column(name = "pickup_location", nullable = false, length = 255)
    private String pickupLocation;

    @NotBlank(message = "Drop location is required")
    @Size(max = 255, message = "Drop location must not exceed 255 characters")
    @Column(name = "drop_location", nullable = false, length = 255)
    private String dropLocation;

    @Size(max = 50, message = "Ride type must not exceed 50 characters")
    @Column(name = "ride_type", length = 50)
    private String rideType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
