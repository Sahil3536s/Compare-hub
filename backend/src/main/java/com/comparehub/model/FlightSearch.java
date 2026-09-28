package com.comparehub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "flight_searches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank(message = "From airport is required")
    @Size(max = 10, message = "From airport code must not exceed 10 characters")
    @Column(name = "from_airport", nullable = false, length = 10)
    private String fromAirport;

    @NotBlank(message = "To airport is required")
    @Size(max = 10, message = "To airport code must not exceed 10 characters")
    @Column(name = "to_airport", nullable = false, length = 10)
    private String toAirport;

    @NotNull(message = "Departure date is required")
    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Min(value = 1, message = "Passengers must be at least 1")
    @Column(nullable = false)
    @Builder.Default
    private Integer passengers = 1;

    @NotBlank(message = "Cabin class is required")
    @Size(max = 50, message = "Cabin class must not exceed 50 characters")
    @Column(name = "cabin_class", nullable = false, length = 50)
    @Builder.Default
    private String cabinClass = "Economy";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (passengers == null) {
            passengers = 1;
        }
        if (cabinClass == null) {
            cabinClass = "Economy";
        }
    }
}
