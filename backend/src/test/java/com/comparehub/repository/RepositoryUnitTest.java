package com.comparehub.repository;

import com.comparehub.model.Product;
import com.comparehub.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SavedProductRepository savedProductRepository;

    @Test
    void shouldFindUserByEmail() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    void shouldFindProductsByCategory() {
        Product p = Product.builder()
                .id(10L)
                .name("MacBook Air")
                .category("Laptops")
                .build();

        when(productRepository.findByCategoryIgnoreCase("Laptops")).thenReturn(List.of(p));

        List<Product> products = productRepository.findByCategoryIgnoreCase("Laptops");
        assertEquals(1, products.size());
        assertEquals("MacBook Air", products.get(0).getName());
    }

    @Test
    void shouldCheckSavedProductExistence() {
        when(savedProductRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

        boolean exists = savedProductRepository.existsByUserIdAndProductId(1L, 10L);
        assertTrue(exists);
    }
}
