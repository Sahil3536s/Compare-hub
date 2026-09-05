package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributesDto {

    private String brand;
    private String model;
    private String variant; // e.g. "5G", "Pro", "Pro Max", "Ultra", "Plus"
    private String ram; // e.g. "12GB", "8GB", "16GB"
    private String storage; // e.g. "256GB", "128GB", "512GB", "1TB"
    private String color; // e.g. "Black", "Titanium", "Midnight"
    private String screenSize; // e.g. "6.7 inch"
    private String processor; // e.g. "M3", "Snapdragon 8 Gen 3"
    private String modelNumber; // e.g. "SM-S928B"
    private String canonicalKey; // Deterministic canonical hash key e.g. "samsung_galaxy-s26_5g_12gb_256gb"
}
