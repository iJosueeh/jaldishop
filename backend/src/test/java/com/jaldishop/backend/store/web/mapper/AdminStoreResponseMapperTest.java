package com.jaldishop.backend.store.web.mapper;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.store.domain.Store;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStoreResponseMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminStoreResponseMapper mapper;

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
                null
        );

        merchant = User.reconstitute(
                merchantId,
                "owner@jaldishop.com",
                "pass",
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
    @DisplayName("toSummary() debe mapear Store con datos del comerciante")
    void toSummaryShouldMapCorrectly() {
        when(userRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        AdminStoreSummaryResponse summary = mapper.toSummary(store);

        assertThat(summary.name()).isEqualTo("Cafe Central");
        assertThat(summary.slug()).isEqualTo("cafe-central");
        assertThat(summary.merchant()).isNotNull();
        assertThat(summary.merchant().fullName()).isEqualTo("Elena Rios");
    }

    @Test
    @DisplayName("toDetail() debe mapear Store a AdminStoreDetailResponse con merchant")
    void toDetailShouldMapCorrectly() {
        when(userRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        AdminStoreDetailResponse detail = mapper.toDetail(store);

        assertThat(detail.name()).isEqualTo("Cafe Central");
        assertThat(detail.slug()).isEqualTo("cafe-central");
        assertThat(detail.merchant()).isNotNull();
        assertThat(detail.merchant().email()).isEqualTo("owner@jaldishop.com");
    }
}
