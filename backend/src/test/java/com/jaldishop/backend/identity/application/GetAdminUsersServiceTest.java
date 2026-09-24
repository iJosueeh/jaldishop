package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserSummaryResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
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
class GetAdminUsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private GetAdminUsersService getAdminUsersService;

    private User merchantUser;
    private Store store;
    private UUID merchantId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        merchantUser = User.reconstitute(
                merchantId,
                "merchant@jaldishop.com",
                "hashed",
                "Maria",
                "Gomez",
                "912345678",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 2, RoleName.MERCHANT)),
                Instant.now(),
                Instant.now()
        );

        store = Store.create(
                merchantId,
                "Pasteleria Dulce",
                "pasteleria-dulce",
                "Pasteles finos",
                "912345678",
                "Av. Larco 123",
                "Miraflores",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(5),
                "PEN",
                false,
                null
        );
    }

    @Test
    @DisplayName("Debe listar usuarios y relacionar tienda asociada para comerciantes")
    void shouldListUsersWithStoreInfo() {
        when(userRepository.findAll(null, null, null)).thenReturn(List.of(merchantUser));
        when(storeRepository.findByMerchantUserId(merchantId)).thenReturn(Optional.of(store));

        List<AdminUserSummaryResponse> results = getAdminUsersService.listUsers(null, null, null);

        assertThat(results).hasSize(1);
        AdminUserSummaryResponse res = results.get(0);
        assertThat(res.id()).isEqualTo(merchantId);
        assertThat(res.email()).isEqualTo("merchant@jaldishop.com");
        assertThat(res.roles()).contains("MERCHANT");
        assertThat(res.storeName()).isEqualTo("Pasteleria Dulce");
    }

    @Test
    @DisplayName("Debe obtener detalle de usuario por ID")
    void shouldGetUserById() {
        when(userRepository.findById(merchantId)).thenReturn(Optional.of(merchantUser));
        when(storeRepository.findByMerchantUserId(merchantId)).thenReturn(Optional.of(store));

        AdminUserDetailResponse result = getAdminUsersService.getUserById(merchantId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(merchantId);
        assertThat(result.fullName()).isEqualTo("Maria Gomez");
        assertThat(result.store()).isNotNull();
        assertThat(result.store().name()).isEqualTo("Pasteleria Dulce");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe al buscar por ID")
    void shouldThrowWhenUserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getAdminUsersService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
