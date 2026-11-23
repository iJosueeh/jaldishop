/**
 * Estado operativo de la variante en el catálogo de productos.
 */
export type VariantStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

/**
 * Estado de configuración y rastreo del inventario para una variante:
 * - CONFIGURED: Variante con tracksInventory=true y cantidad física inicializada en la base de datos.
 * - NOT_CONFIGURED: Variante con tracksInventory=true pero sin registro inicial en la tabla inventories.
 * - NOT_TRACKED: Variante con tracksInventory=false (producto sin control de existencias físicas).
 */
export type InventoryStatus = 'CONFIGURED' | 'NOT_CONFIGURED' | 'NOT_TRACKED';

/**
 * Contrato de respuesta del inventario de una variante.
 * Representa com.jaldishop.backend.inventory.web.dto.InventoryResponse.
 */
export interface InventoryResponse {
  variantId: string;
  productId: string;
  productName: string;
  presentationName: string;
  sku: string | null;
  tracksInventory: boolean;
  status: VariantStatus | string;
  inventoryStatus: InventoryStatus;
  physicalQuantity: number | null;
  reservedQuantity: number | null;
  availableQuantity: number | null;
  lowStockThreshold: number | null;
  isLowStock: boolean;
  updatedAt: string | null;
}

/**
 * Contrato de solicitud para ajuste o configuración de stock físico absoluto.
 * Representa com.jaldishop.backend.inventory.web.dto.UpdateStockRequest.
 */
export interface UpdateStockRequest {
  quantity: number;
}

/**
 * Contrato de solicitud para actualización del umbral de alerta de stock bajo.
 * Representa com.jaldishop.backend.inventory.web.dto.UpdateThresholdRequest.
 * lowStockThreshold: número entero >= 0, o null para desactivar la alerta.
 */
export interface UpdateThresholdRequest {
  lowStockThreshold: number | null;
}
