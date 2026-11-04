package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.store.application.command.CreateStoreCommand;
import com.jaldishop.backend.store.domain.enums.StoreCategoryStatus;
import com.jaldishop.backend.store.domain.model.StoreCategory;
import com.jaldishop.backend.store.domain.repository.StoreCategoryRepository;

import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    private CreateStoreService createStoreService;

    private UUID ownerUserId;

    @BeforeEach
    void setUp() {
        createStoreService = new CreateStoreService(storeRepository, userRepository);
        ownerUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Crear tienda exitosamente y asignar rol MERCHANT al owner")
    void createStoreSuccessfully() {
        CreateStoreCommand command = new CreateStoreCommand(
                ownerUserId,
                "Tienda Jaldi",
                "tienda-jaldi",
                "Descripción de prueba",
                "+50212345678",
                "Calle 1",
                "Ref 1",
                new BigDecimal("14.5"),
                new BigDecimal("-90.5"),
                true,
                true,
                new BigDecimal("10.00"),
                "GTQ",
                null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);
        when(storeRepository.existsBySlug("tienda-jaldi")).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store store = createStoreService.execute(command);

        assertNotNull(store);
        assertEquals(ownerUserId, store.getOwnerUserId());
        assertEquals("Tienda Jaldi", store.getName());
        assertEquals("tienda-jaldi", store.getSlug());
        assertEquals(StoreStatus.ACTIVE, store.getStatus());
        verify(storeRepository).save(any(Store.class));
        verify(userRepository, times(1)).assignStoreToMerchantRole(ownerUserId, store.getId());
    }

    @Test
    @DisplayName("Si falla la asignación del rol MERCHANT, se lanza excepción para rollback")
    void shouldThrowExceptionAndRollbackWhenMerchantRoleAssignmentFails() {
        CreateStoreCommand command = new CreateStoreCommand(
                ownerUserId,
                "Tienda Fallida",
                "tienda-fallida",
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);
        when(storeRepository.existsBySlug("tienda-fallida")).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Database error assigning role"))
                .when(userRepository).assignStoreToMerchantRole(eq(ownerUserId), any());

        assertThrows(RuntimeException.class, () -> createStoreService.execute(command));
    }

    @Test
    @DisplayName("Lanzar excepción cuando el comerciante ya tiene una tienda registrada")
    void throwExceptionWhenMerchantAlreadyHasStore() {
        CreateStoreCommand command = new CreateStoreCommand(
                ownerUserId,
                "Segunda Tienda",
                "segunda-tienda",
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> createStoreService.execute(command)
        );

        assertEquals("MERCHANT_ALREADY_HAS_STORE", exception.getCode());
        verify(storeRepository, never()).save(any(Store.class));
        verify(userRepository, never()).assignStoreToMerchantRole(any(), any());
    }

    @Test
    @DisplayName("Lanzar excepción cuando el slug ya está en uso")
    void throwExceptionWhenSlugAlreadyExists() {
        CreateStoreCommand command = new CreateStoreCommand(
                ownerUserId,
                "Tienda Duplicada",
                "tienda-duplicada",
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);
        when(storeRepository.existsBySlug("tienda-duplicada")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> createStoreService.execute(command)
        );

        assertEquals("STORE_SLUG_ALREADY_EXISTS", exception.getCode());
        verify(storeRepository, never()).save(any(Store.class));
        verify(userRepository, never()).assignStoreToMerchantRole(any(), any());
    }

    @Test
    @DisplayName("Generar slug automáticamente a partir del nombre cuando slug es nulo")
    void generateSlugFromNameWhenSlugIsNull() {
        CreateStoreCommand command = new CreateStoreCommand(
                ownerUserId,
                "Super Tienda Express!",
                null,
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);
        when(storeRepository.existsBySlug("super-tienda-express")).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store store = createStoreService.execute(command);

        assertNotNull(store);
        assertEquals("super-tienda-express", store.getSlug());
        verify(storeRepository).save(any(Store.class));
        verify(userRepository).assignStoreToMerchantRole(ownerUserId, store.getId());
    }

    @ParameterizedTest
    @DisplayName("Normalización de caracteres Unicode, acentos, ñ y símbolos en generación de slug")
    @CsvSource({
            "Panadería Don Marco, panaderia-don-marco",
            "Café & Grano, cafe-grano",
            "Niño Feliz, nino-feliz",
            "Pastelería Dulce Deleite, pasteleria-dulce-deleite",
            "'  Super  &&  Tienda   $$  Plus   ', super-tienda-plus"
    })
    void shouldNormalizeUnicodeAccentsAndSymbolsWhenGeneratingSlug(String storeName, String expectedSlug) {
        CreateStoreCommand command = new CreateStoreCommand(
                ownerUserId,
                storeName,
                null,
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);
        when(storeRepository.existsBySlug(expectedSlug)).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store store = createStoreService.execute(command);

        assertNotNull(store);
        assertEquals(expectedSlug, store.getSlug());
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    @DisplayName("Conservar slug explícito proporcionado por el usuario respetando normalización básica")
    void shouldUseExplicitSlugWhenProvided() {
        CreateStoreCommand command = new CreateStoreCommand(
                ownerUserId,
                "Panadería Don Marco",
                "mi-slug-personalizado",
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);
        when(storeRepository.existsBySlug("mi-slug-personalizado")).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store store = createStoreService.execute(command);

        assertNotNull(store);
        assertEquals("mi-slug-personalizado", store.getSlug());
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    @DisplayName("Lanzar IllegalArgumentException si la entrada queda vacía después de normalizarse")
    void shouldThrowIllegalArgumentExceptionWhenSlugBecomesEmptyAfterNormalization() {
        CreateStoreCommand commandOnlySymbols = new CreateStoreCommand(
                ownerUserId,
                "??? !!! @@@ ###",
                null,
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        when(storeRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);

        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> createStoreService.execute(commandOnlySymbols)
        );
        assertEquals("El slug no puede ser nulo ni vacio.", ex1.getMessage());

        CreateStoreCommand commandExplicitInvalidSlug = new CreateStoreCommand(
                ownerUserId,
                "Tienda Válida",
                "   ---   $$$   ",
                null, null, null, null, null, null,
                true, false, null, null, null
        );

        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> createStoreService.execute(commandExplicitInvalidSlug)
        );
        assertEquals("El slug no puede ser nulo ni vacio.", ex2.getMessage());
    }
}
