package com.comparehub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "search_histories", indexes = {
    @Index(name = "idx_search_histories_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank(message = "Search query cannot be blank")
    @Size(max = 255, message = "Search query must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String query;

    @NotNull(message = "Search type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 50)
    private SearchType searchType;

    @Size(max = 500, message = "Details must not exceed 500 characters")
    @Column(length = 500)
    private String details;

    @Size(max = 500, message = "Target URL must not exceed 500 characters")
    @Column(name = "target_url", length = 500)
    private String targetUrl;

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
