
package com.comparehub.provider;

import com.comparehub.provider.impl.OpenProductDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class OpenProductDataProviderTest {

    private OpenProductDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OpenProductDataProvider();

        // Enable the provider for this test.
        // It should still return no fabricated offers.
        ReflectionTestUtils.setField(provider, "enabled", true);
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("OpenCommerce", provider.getProviderName());
    }

    @Test
    void shouldReturnEmptyResultsWhenNoAuthorizedApiExists() {
        assertNotNull(provider.searchProducts("iphone"));

        assertTrue(provider.searchProducts("iphone").isEmpty());
    }

    @Test
    void shouldReturnEmptyResultsFromFallback() {
        assertNotNull(
                provider.fallbackSearch(
                        "iphone",
                        new RuntimeException("API unavailable")
                )
        );

        assertTrue(
                provider.fallbackSearch(
                        "iphone",
                        new RuntimeException("API unavailable")
                ).isEmpty()
        );
    }
}
