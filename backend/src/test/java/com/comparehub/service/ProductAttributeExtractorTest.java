package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.service.impl.ProductAttributeExtractorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductAttributeExtractorTest {

    private ProductAttributeExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ProductAttributeExtractorImpl();
    }

    @Test
    @DisplayName("1. Currency normalization across symbols and codes")
    void testNormalizeCurrency() {
        assertThat(extractor.normalizeCurrency("₹")).isEqualTo("INR");
        assertThat(extractor.normalizeCurrency("Rs.")).isEqualTo("INR");
        assertThat(extractor.normalizeCurrency("rs")).isEqualTo("INR");
        assertThat(extractor.normalizeCurrency("INR")).isEqualTo("INR");
        assertThat(extractor.normalizeCurrency("inr")).isEqualTo("INR");
        assertThat(extractor.normalizeCurrency("$")).isEqualTo("USD");
        assertThat(extractor.normalizeCurrency("usd")).isEqualTo("USD");
        assertThat(extractor.normalizeCurrency("€")).isEqualTo("EUR");
        assertThat(extractor.normalizeCurrency("£")).isEqualTo("GBP");
        assertThat(extractor.normalizeCurrency(null)).isEqualTo("INR");
        assertThat(extractor.normalizeCurrency("")).isEqualTo("INR");
    }

    @Test
    @DisplayName("2. Price parsing from string, formatted values, and numbers")
    void testNormalizePrice() {
        assertThat(extractor.normalizePrice("₹59,999")).isEqualByComparingTo(new BigDecimal("59999"));
        assertThat(extractor.normalizePrice("Rs. 58,499.50")).isEqualByComparingTo(new BigDecimal("58499.50"));
        assertThat(extractor.normalizePrice("59999")).isEqualByComparingTo(new BigDecimal("59999"));
        assertThat(extractor.normalizePrice(58499)).isEqualByComparingTo(new BigDecimal("58499"));
        assertThat(extractor.normalizePrice(new BigDecimal("74999.00"))).isEqualByComparingTo(new BigDecimal("74999.00"));
        assertThat(extractor.normalizePrice(null)).isNull();
        assertThat(extractor.normalizePrice("")).isNull();
        assertThat(extractor.normalizePrice("invalid_price_text")).isNull();
    }

    @Test
    @DisplayName("3. Brand normalization to canonical capitalization")
    void testNormalizeBrand() {
        assertThat(extractor.normalizeBrand("SAMSUNG")).isEqualTo("Samsung");
        assertThat(extractor.normalizeBrand("samsung")).isEqualTo("Samsung");
        assertThat(extractor.normalizeBrand("apple")).isEqualTo("Apple");
        assertThat(extractor.normalizeBrand("APPLE")).isEqualTo("Apple");
        assertThat(extractor.normalizeBrand("boat")).isEqualTo("boAt");
        assertThat(extractor.normalizeBrand("oneplus")).isEqualTo("OnePlus");
        assertThat(extractor.normalizeBrand("SONY")).isEqualTo("Sony");
        assertThat(extractor.normalizeBrand(null)).isEqualTo("Generic");
        assertThat(extractor.normalizeBrand("all")).isEqualTo("Generic");
    }

    @Test
    @DisplayName("4. RAM extraction & normalization")
    void testNormalizeRam() {
        assertThat(extractor.normalizeRam("8 GB RAM")).isEqualTo("8GB");
        assertThat(extractor.normalizeRam("8GB")).isEqualTo("8GB");
        assertThat(extractor.normalizeRam("12 GB")).isEqualTo("12GB");
        assertThat(extractor.normalizeRam("16 gb")).isEqualTo("16GB");
        assertThat(extractor.normalizeRam(null)).isNull();
        assertThat(extractor.normalizeRam("")).isNull();
    }

    @Test
    @DisplayName("5. Storage extraction & normalization")
    void testNormalizeStorage() {
        assertThat(extractor.normalizeStorage("256 GB")).isEqualTo("256GB");
        assertThat(extractor.normalizeStorage("256GB")).isEqualTo("256GB");
        assertThat(extractor.normalizeStorage("256 gb")).isEqualTo("256GB");
        assertThat(extractor.normalizeStorage("1 TB")).isEqualTo("1TB");
        assertThat(extractor.normalizeStorage("1TB")).isEqualTo("1TB");
        assertThat(extractor.normalizeStorage("128 GB")).isEqualTo("128GB");
        assertThat(extractor.normalizeStorage("512 GB")).isEqualTo("512GB");
        assertThat(extractor.normalizeStorage(null)).isNull();
        assertThat(extractor.normalizeStorage("")).isNull();
    }

    @Test
    @DisplayName("6. Network extraction")
    void testExtractNetwork() {
        assertThat(extractor.extractNetwork("Samsung Galaxy S24 5G 256GB")).isEqualTo("5G");
        assertThat(extractor.extractNetwork("Redmi Note 13 4G LTE")).isEqualTo("4G");
        assertThat(extractor.extractNetwork("iPad Pro 11-inch Wi-Fi 128GB")).isEqualTo("Wi-Fi");
        assertThat(extractor.extractNetwork("Sony WH-1000XM5")).isNull();
    }

    @Test
    @DisplayName("7. Title cleaning without destroying model identifiers")
    void testCleanTitle() {
        String dirtyTitle = "Samsung Galaxy S24 5G (Onyx Black, 256 GB) (8 GB RAM) - Lowest Price Ever [Limited Time Deal]";
        String cleaned = extractor.cleanTitle(dirtyTitle);

        assertThat(cleaned).contains("Samsung Galaxy S24 5G");
        assertThat(cleaned).contains("256 GB");
        assertThat(cleaned).contains("8 GB RAM");
        assertThat(cleaned).doesNotContainIgnoringCase("Lowest Price Ever");
        assertThat(cleaned).doesNotContainIgnoringCase("Limited Time Deal");
    }

    @Test
    @DisplayName("8. Prefer structured provider attributes over title parsing")
    void testPreferStructuredAttributesOverTitleParsing() {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .title("Special Offer Device")
                .brand("SAMSUNG")
                .model("Galaxy S24")
                .storage("256 GB")
                .ram("8 GB RAM")
                .color("Onyx Black")
                .network("5G")
                .modelNumber("SM-S921B")
                .build();

        ProductAttributesDto attr = extractor.extractAttributes(offer);

        assertThat(attr.getBrand()).isEqualTo("Samsung");
        assertThat(attr.getModel()).isEqualTo("Galaxy S24");
        assertThat(attr.getStorage()).isEqualTo("256GB");
        assertThat(attr.getRam()).isEqualTo("8GB");
        assertThat(attr.getColor()).isEqualTo("Onyx Black");
        assertThat(attr.getNetwork()).isEqualTo("5G");
        assertThat(attr.getModelNumber()).isEqualTo("SM-S921B");
    }

    @Test
    @DisplayName("9. Accurately extract brand, model, variant, RAM, storage, and color from full title")
    void testExtractAttributesFullTitle() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Samsung Galaxy S26 5G 12GB 256GB Black", null, null);

        assertThat(attr.getBrand()).isEqualTo("Samsung");
        assertThat(attr.getModel()).isEqualTo("Galaxy S26");
        assertThat(attr.getRam()).isEqualTo("12GB");
        assertThat(attr.getStorage()).isEqualTo("256GB");
        assertThat(attr.getColor()).isEqualTo("Black");
    }

    @Test
    @DisplayName("10. Extract attributes from parenthesized format: SAMSUNG S26 (Black, 256 GB)")
    void testExtractAttributesParenthesized() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "SAMSUNG S26 (Black, 256 GB)", null, null);

        assertThat(attr.getBrand()).isEqualTo("Samsung");
        assertThat(attr.getModel()).isEqualTo("Galaxy S26");
        assertThat(attr.getStorage()).isEqualTo("256GB");
        assertThat(attr.getColor()).isEqualTo("Black");
    }

    @Test
    @DisplayName("11. Extract RAM and Storage from slash notation: Galaxy S26 12/256GB 5G")
    void testExtractAttributesSlashNotation() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Galaxy S26 12/256GB 5G", null, null);

        assertThat(attr.getBrand()).isEqualTo("Samsung");
        assertThat(attr.getModel()).isEqualTo("Galaxy S26");
        assertThat(attr.getRam()).isEqualTo("12GB");
        assertThat(attr.getStorage()).isEqualTo("256GB");
    }

    @Test
    @DisplayName("12. Extract iPhone 15 Pro Max and Natural Titanium color")
    void testExtractAttributesIPhone() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Apple iPhone 15 Pro Max (256 GB) - Natural Titanium", null, null);

        assertThat(attr.getBrand()).isEqualTo("Apple");
        assertThat(attr.getModel()).isEqualTo("Iphone 15 Pro Max");
        assertThat(attr.getStorage()).isEqualTo("256GB");
        assertThat(attr.getColor()).isEqualTo("Natural Titanium");
    }

    @Test
    @DisplayName("13. Extract Sony headphone model and color")
    void testExtractAttributesSonyHeadphones() {
        ProductAttributesDto attr = extractor.extractAttributes(
                "Sony WH-1000XM5 Wireless Noise Cancelling Headphones - Silver", null, null);

        assertThat(attr.getBrand()).isEqualTo("Sony");
        assertThat(attr.getModel()).isEqualTo("WH-1000XM5");
        assertThat(attr.getColor()).isEqualTo("Silver");
    }
}
