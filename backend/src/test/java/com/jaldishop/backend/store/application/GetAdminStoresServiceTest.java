package com.jaldishop.backend.store.application;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminStoresServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetAdminStoresService getAdminStoresService;

    private Store store;
    private User merchant;
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

        merchant = User.reconstitute(
                merchantId,
                "owner@jaldishop.com",
                "hashed",
                "Elena",
                "Rios",
                "955443322",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 2, RoleName.MERCHANT)),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Debe listar tiendas con datos del comerciante propietario")
    void shouldListStoresWithMerchantInfo() {
        when(storeRepository.findAllStores(null, null)).thenReturn(List.of(store));
        when(userRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        List<AdminStoreSummaryResponse> list = getAdminStoresService.listStores(null, null);

        assertThat(list).hasSize(1);
        AdminStoreSummaryResponse item = list.get(0);
        assertThat(item.name()).isEqualTo("Cafe Central");
        assertThat(item.merchant()).isNotNull();
        assertThat(item.merchant().fullName()).isEqualTo("Elena Rios");
    }

    @Test
    @DisplayName("Debe obtener detalle de tienda por ID")
    void shouldGetStoreById() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(userRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        AdminStoreDetailResponse detail = getAdminStoresService.getStoreById(storeId);

        assertThat(detail).isNotNull();
        assertThat(detail.name()).isEqualTo("Cafe Central");
        assertThat(detail.slug()).isEqualTo("cafe-central");
        assertThat(detail.merchant()).isNotNull();
        assertThat(detail.merchant().email()).isEqualTo("owner@jaldishop.com");
    }

    @Test
    @DisplayName("Debe lanzar excepción si la tienda no existe")
    void shouldThrowWhenStoreNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(storeRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getAdminStoresService.getStoreById(nonExistent))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
