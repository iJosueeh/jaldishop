package com.jaldishop.backend.store.domain;

import java.util.UUID;

/**
 * Puerto de dominio para la limpieza de los archivos multimedia de una tienda.
 * Permite que el cierre de tienda no dependa directamente del módulo {@code media}
 * (rompe el ciclo store ↔ media); la implementación vive en {@code media/application}.
 */
public interface StoreMediaCleaner {

    /**
     * Elimina los archivos multimedia asociados a la tienda.
     * Los fallos de limpieza no deben comprometer el cierre de la tienda:
     * el aislamiento de errores lo aplica el llamante.
     */
    void deleteStoreMedia(UUID storeId);
}
