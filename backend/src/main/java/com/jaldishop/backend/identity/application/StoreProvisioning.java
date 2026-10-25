package com.jaldishop.backend.identity.application;

import java.util.UUID;

/**
 * Puerto inverso de aprovisionamiento de tienda: {@code identity} no depende de
 * {@code store} para registrar un comerciante (rompe el ciclo identity ↔ store).
 * La implementación vive en {@code store/application} (StoreProvisioningAdapter)
 * y delega en CreateStoreService, conservando validaciones, defaults y
 * transacción únicas del flujo de registro.
 */
public interface StoreProvisioning {

    /**
     * Crea la tienda del comerciante durante el registro.
     *
     * @param data datos de la tienda calculados por identity (incluye los
     *             defaults del flujo de registro: slug null, PEN, 18.00, etc.)
     * @return el id de la tienda creada
     */
    UUID provisionStore(StoreProvisioningData data);
}
