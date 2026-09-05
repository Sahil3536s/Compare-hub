package com.comparehub.provider;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.provider.impl.OpenProductDataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OpenProductDataProviderTest {

    @Mock
    private RestClient restClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OpenProductDataProvider openProductDataProvider;

    @Test
    void shouldReturnFallbackWhenExceptionOccurs() {
        List<NormalizedProductOfferDto> fallbackResults = openProductDataProvider.fallbackSearch(
                "iphone", new RuntimeException("API connection timeout"));

        assertNotNull(fallbackResults);
        assertFalse(fallbackResults.isEmpty());
        assertEquals("OpenCommerce (Fallback)", fallbackResults.get(0).getMerchant());
        assertEquals("Apple", fallbackResults.get(0).getBrand());
    }

    @Test
    void shouldIdentifyProviderName() {
        assertEquals("OpenCommerce", openProductDataProvider.getProviderName());
    }
}
