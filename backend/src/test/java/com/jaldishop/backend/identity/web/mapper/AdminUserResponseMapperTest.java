package com.jaldishop.backend.identity.web.mapper;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserSummaryResponse;
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
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserResponseMapperTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private AdminUserResponseMapper mapper;

    private User merchantUser;
    private Store store;
    private UUID userId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        merchantUser = User.reconstitute(
                userId,
                "merchant@jaldishop.com",
                "pass",
                "Ana",
                "Silva",
                "999888777",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 2, RoleName.MERCHANT)),
                Instant.now(),
                Instant.now()
        );

        store = Store.create(
                userId,
                "Floreria Lima",
                "floreria-lima",
                "Flores y detalles",
                "999888777",
                "Av Arequipa 123",
                "Lince",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(8),
                "PEN",
                null
        );
    }

    @Test
    @DisplayName("toSummary() debe mapear User a AdminUserSummaryResponse con datos de tienda")
    void toSummaryShouldMapCorrectly() {
        when(storeRepository.findByMerchantUserId(userId)).thenReturn(Optional.of(store));

        AdminUserSummaryResponse summary = mapper.toSummary(merchantUser);

        assertThat(summary.id()).isEqualTo(userId);
        assertThat(summary.email()).isEqualTo("merchant@jaldishop.com");
        assertThat(summary.roles()).contains("MERCHANT");
        assertThat(summary.storeName()).isEqualTo("Floreria Lima");
    }

    @Test
    @DisplayName("toDetail() debe mapear User a AdminUserDetailResponse con datos de tienda")
    void toDetailShouldMapCorrectly() {
        when(storeRepository.findByMerchantUserId(userId)).thenReturn(Optional.of(store));

        AdminUserDetailResponse detail = mapper.toDetail(merchantUser);

        assertThat(detail.id()).isEqualTo(userId);
        assertThat(detail.fullName()).isEqualTo("Ana Silva");
        assertThat(detail.store()).isNotNull();
        assertThat(detail.store().name()).isEqualTo("Floreria Lima");
        assertThat(detail.store().slug()).isEqualTo("floreria-lima");
    }
}
