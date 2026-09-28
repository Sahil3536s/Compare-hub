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
    private String processor; // e.g. "M3", "Snapdragon 8 Gen 3"
    private String modelNumber; // e.g. "SM-S928B"
    private String network; // e.g. "5G", "4G", "LTE", "Wi-Fi"
    private String screenSize; // e.g. "6.7 inch", "6.1 inch"
    private String canonicalKey; // Deterministic canonical hash key e.g. "samsung_galaxy-s26_5g_12gb_256gb"

    @Builder.Default
    private java.util.Map<String, String> genericAttributes = new java.util.LinkedHashMap<>();

    public java.util.Map<String, String> toMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        if (brand != null && !brand.isBlank()) map.put("Brand", brand);
        if (model != null && !model.isBlank()) map.put("Model", model);
        if (variant != null && !variant.isBlank()) map.put("Variant", variant);
        if (ram != null && !ram.isBlank()) { map.put("RAM", ram); map.put("ram", ram); }
        if (storage != null && !storage.isBlank()) { map.put("Storage", storage); map.put("storage", storage); }
        if (color != null && !color.isBlank()) { map.put("Color", color); map.put("color", color); }
        if (processor != null && !processor.isBlank()) map.put("Processor", processor);
        if (modelNumber != null && !modelNumber.isBlank()) map.put("Model Number", modelNumber);
        if (network != null && !network.isBlank()) map.put("Network", network);
        if (screenSize != null && !screenSize.isBlank()) map.put("Screen Size", screenSize);
        if (genericAttributes != null) {
            map.putAll(genericAttributes);
        }
        return map;
    }
}
