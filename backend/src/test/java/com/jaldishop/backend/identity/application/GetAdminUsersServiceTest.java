package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private GetAdminUsersService getAdminUsersService;

    private User merchantUser;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();

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
    }

    @Test
    @DisplayName("Debe listar usuarios del repositorio")
    void shouldListUsers() {
        when(userRepository.findAll(null, null, null)).thenReturn(List.of(merchantUser));

        List<User> results = getAdminUsersService.execute(new GetAdminUsersQuery(null, null, null));

        assertThat(results).hasSize(1);
        User res = results.get(0);
        assertThat(res.getId()).isEqualTo(merchantId);
        assertThat(res.getEmail()).isEqualTo("merchant@jaldishop.com");
        assertThat(res.hasRole(RoleName.MERCHANT)).isTrue();
    }

    @Test
    @DisplayName("Debe obtener usuario por ID")
    void shouldGetUserById() {
        when(userRepository.findById(merchantId)).thenReturn(Optional.of(merchantUser));

        User result = getAdminUsersService.execute(merchantId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(merchantId);
        assertThat(result.getFullName()).isEqualTo("Maria Gomez");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe al buscar por ID")
    void shouldThrowWhenUserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getAdminUsersService.execute(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
