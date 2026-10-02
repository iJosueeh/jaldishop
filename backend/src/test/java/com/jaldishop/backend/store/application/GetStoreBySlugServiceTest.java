package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStoreBySlugServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private GetStoreBySlugService getStoreBySlugService;

    @Test
    @DisplayName("Debe retornar la tienda activa cuando el slug existe")
    void shouldReturnActiveStoreWhenSlugExists() {
        UUID storeId = UUID.randomUUID();
        Store store = Store.reconstitute(
                storeId,
                UUID.randomUUID(),
                "Pastelería Dulce Sabor",
                "dulce-sabor",
                "Los mejores postres",
                "987654321",
                "Av Larco 500",
                "Miraflores",
                new BigDecimal("-12.12"),
                new BigDecimal("-77.03"),
                true,
                true,
                new BigDecimal("6.00"),
                "PEN",
                new BigDecimal("18.00"),
                StoreStatus.ACTIVE,
                null,
                null
        );

        when(storeRepository.findBySlug("dulce-sabor")).thenReturn(Optional.of(store));

        Store result = getStoreBySlugService.execute("Dulce-Sabor");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(storeId);
        assertThat(result.getName()).isEqualTo("Pastelería Dulce Sabor");
        assertThat(result.getSlug()).isEqualTo("dulce-sabor");
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el slug no existe")
    void shouldThrowNotFoundWhenSlugDoesNotExist() {
        when(storeRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreBySlugService.execute("no-existe"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la tienda está INACTIVA")
    void shouldThrowNotFoundWhenStoreIsInactive() {
        Store inactiveStore = Store.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Tienda Inactiva",
                "tienda-inactiva",
                null, null, null, null, null, null,
                true, true, null, "PEN", null,
                StoreStatus.INACTIVE, null, null
        );

        when(storeRepository.findBySlug("tienda-inactiva")).thenReturn(Optional.of(inactiveStore));

        assertThatThrownBy(() -> getStoreBySlugService.execute("tienda-inactiva"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no se encuentra activa");
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si el slug es nulo o vacío")
    void shouldThrowIllegalArgumentWhenSlugInvalid() {
        assertThatThrownBy(() -> getStoreBySlugService.execute(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
