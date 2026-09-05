package com.comparehub.service;

import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.service.impl.ProductAttributeExtractorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductAttributeExtractorTest {

    private ProductAttributeExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ProductAttributeExtractorImpl();
    }

    @Test
    @DisplayName("Should accurately extract brand, model, variant, RAM, storage, and color from full title")
    void testExtractAttributesFullTitle() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Samsung Galaxy S26 5G 12GB 256GB Black", null, null);

        assertThat(attr.getBrand()).isEqualTo("Samsung");
        assertThat(attr.getModel()).isEqualTo("Galaxy S26");
        assertThat(attr.getVariant()).isEqualTo("5G");
        assertThat(attr.getRam()).isEqualTo("12GB");
        assertThat(attr.getStorage()).isEqualTo("256GB");
        assertThat(attr.getColor()).isEqualTo("Black");
    }

    @Test
    @DisplayName("Should extract attributes from parenthesized format: SAMSUNG S26 (Black, 256 GB)")
    void testExtractAttributesParenthesized() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "SAMSUNG S26 (Black, 256 GB)", null, null);

        assertThat(attr.getBrand()).isEqualTo("Samsung");
        assertThat(attr.getModel()).isEqualTo("Galaxy S26");
        assertThat(attr.getStorage()).isEqualTo("256GB");
        assertThat(attr.getColor()).isEqualTo("Black");
    }

    @Test
    @DisplayName("Should extract RAM and Storage from slash notation: Galaxy S26 12/256GB 5G")
    void testExtractAttributesSlashNotation() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Galaxy S26 12/256GB 5G", null, null);

        assertThat(attr.getBrand()).isEqualTo("Samsung");
        assertThat(attr.getModel()).isEqualTo("Galaxy S26");
        assertThat(attr.getVariant()).isEqualTo("5G");
        assertThat(attr.getRam()).isEqualTo("12GB");
        assertThat(attr.getStorage()).isEqualTo("256GB");
    }

    @Test
    @DisplayName("Should extract iPhone 15 Pro Max and Natural Titanium color")
    void testExtractAttributesIPhone() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Apple iPhone 15 Pro Max (256 GB) - Natural Titanium", null, null);

        assertThat(attr.getBrand()).isEqualTo("Apple");
        assertThat(attr.getModel()).isEqualTo("Iphone 15 Pro Max");
        assertThat(attr.getStorage()).isEqualTo("256GB");
        assertThat(attr.getColor()).isEqualTo("Natural Titanium");
    }

    @Test
    @DisplayName("Should extract Sony headphone model and color")
    void testExtractAttributesSonyHeadphones() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Sony WH-1000XM5 Wireless Noise Cancelling Headphones - Silver", null, null);

        assertThat(attr.getBrand()).isEqualTo("Sony");
        assertThat(attr.getModel()).isEqualTo("WH-1000XM5");
        assertThat(attr.getColor()).isEqualTo("Silver");
    }
}
