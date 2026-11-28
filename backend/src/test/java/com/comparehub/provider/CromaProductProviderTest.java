
package com.comparehub.provider;

import com.comparehub.provider.impl.CromaProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CromaProductProviderTest {

    private CromaProductProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CromaProductProvider();

        // Enable the provider for this test.
        // No mock catalogue should be returned.
        ReflectionTestUtils.setField(provider, "enabled", true);
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("Croma", provider.getProviderName());
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
