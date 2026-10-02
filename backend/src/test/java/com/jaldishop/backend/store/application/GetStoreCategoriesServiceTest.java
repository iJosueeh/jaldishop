package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreCategory;
import com.jaldishop.backend.store.domain.StoreCategoryRepository;
import com.jaldishop.backend.store.domain.StoreCategoryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStoreCategoriesServiceTest {

    @Mock
    private StoreCategoryRepository storeCategoryRepository;

    @InjectMocks
    private GetStoreCategoriesService service;

    @Test
    @DisplayName("execute() - Debe retornar todas las categorias activas ordenadas")
    void executeShouldReturnAllActiveCategories() {
        StoreCategory cat1 = new StoreCategory(
                UUID.randomUUID(), "Restaurantes", "restaurantes", "Comida y bebidas",
                StoreCategoryStatus.ACTIVE, Instant.now(), Instant.now()
        );
        StoreCategory cat2 = new StoreCategory(
                UUID.randomUUID(), "Moda", "moda", "Ropa y calzado",
                StoreCategoryStatus.ACTIVE, Instant.now(), Instant.now()
        );

        when(storeCategoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        List<StoreCategory> result = service.execute();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Restaurantes");
        assertThat(result.get(1).getName()).isEqualTo("Moda");
        verify(storeCategoryRepository).findAll();
    }
}
