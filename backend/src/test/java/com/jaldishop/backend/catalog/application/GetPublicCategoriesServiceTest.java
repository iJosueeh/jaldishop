package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetPublicCategoriesServiceTest {
    private final GetCategoriesService categories = mock(GetCategoriesService.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final GetPublicCategoriesService service = new GetPublicCategoriesService(categories, stores);

    @Test
    void returnsOnlyActiveCategoriesBelongingToTheRequestedStore() {
        UUID storeId = UUID.randomUUID();
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(store.getStatus()).thenReturn(StoreStatus.ACTIVE);
        when(stores.findById(storeId)).thenReturn(Optional.of(store));
        Category active = Category.create(storeId, "Comida mexicana", null);
        Category inactive = Category.create(storeId, "Archivada", null);
        inactive.deactivate();
        Category otherStore = Category.create(UUID.randomUUID(), "Otra tienda", null);
        when(categories.execute(storeId)).thenReturn(List.of(active, inactive, otherStore));
        assertEquals(List.of(active), service.execute(storeId));
    }

    @Test
    void rejectsInactiveOrMissingStoresBeforeReadingTheirCatalog() {
        UUID storeId = UUID.randomUUID();
        when(stores.findById(storeId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.execute(storeId));
        Store store = mock(Store.class);
        when(store.getStatus()).thenReturn(StoreStatus.INACTIVE);
        when(stores.findById(storeId)).thenReturn(Optional.of(store));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(storeId));
        verifyNoInteractions(categories);
    }
}
