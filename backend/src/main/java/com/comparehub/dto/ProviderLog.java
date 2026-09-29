package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderLog {
    private String providerName;
    private String providerMode; // enum name as string
    private String status; // enum name as string
    private int rawResultCount;
    private int normalizedResultCount;
    private int acceptedResultCount;
    private String failureReason;
}
