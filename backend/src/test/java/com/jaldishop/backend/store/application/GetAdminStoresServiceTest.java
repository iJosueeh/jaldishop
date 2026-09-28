package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminStoresServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private GetAdminStoresService getAdminStoresService;

    private Store store;
    private UUID storeId;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        store = Store.create(
                merchantId,
                "Cafe Central",
                "cafe-central",
                "Cafe y reposteria",
                "955443322",
                "Calle Lima 456",
                "Esquina",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(4),
                "PEN",
                false,
                null
        );
    }

    @Test
    @DisplayName("Debe listar tiendas del repositorio")
    void shouldListStores() {
        when(storeRepository.findAllStores(null, null)).thenReturn(List.of(store));

        List<Store> list = getAdminStoresService.execute(new GetAdminStoresQuery(null, null));

        assertThat(list).hasSize(1);
        Store item = list.get(0);
        assertThat(item.getName()).isEqualTo("Cafe Central");
    }

    @Test
    @DisplayName("Debe obtener tienda por ID")
    void shouldGetStoreById() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        Store result = getAdminStoresService.execute(storeId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Cafe Central");
        assertThat(result.getSlug()).isEqualTo("cafe-central");
    }

    @Test
    @DisplayName("Debe lanzar excepción si la tienda no existe")
    void shouldThrowWhenStoreNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(storeRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getAdminStoresService.execute(nonExistent))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
