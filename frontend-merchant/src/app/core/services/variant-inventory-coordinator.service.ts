import { inject, Injectable } from '@angular/core';
import { Observable, catchError, map, of, switchMap, throwError } from 'rxjs';
import { ProductService } from './product.service';
import { InventoryService } from './inventory.service';
import { TokenService } from './token-service';
import {
  CreateProductVariantRequest,
  ProductVariant,
  ProductVariantAttribute,
} from '../models/product.models';
import { InventoryResponse } from '../models/inventory.models';

export interface CreateVariantWithInventoryRequest {
  presentationName: string;
  sku?: string;
  priceAmount: number;
  priceCurrency?: string;
  tracksInventory: boolean;
  initialStock?: number | null;
  lowStockThreshold?: number | null;
  attributes?: ProductVariantAttribute[];
}

export type VariantInventoryStepFailed = 'VARIANT' | 'STOCK' | 'THRESHOLD' | null;

export interface VariantInventoryResult {
  success: boolean;
  variant: ProductVariant | null;
  inventory: InventoryResponse | null;
  stepFailed: VariantInventoryStepFailed;
  error?: any;
  errorMessage: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class VariantInventoryCoordinatorService {
  private readonly productService = inject(ProductService);
  private readonly inventoryService = inject(InventoryService);
  private readonly tokenService = inject(TokenService);

  /**
   * Valida en memoria los parámetros de inventario según las reglas de negocio:
   * - Si tracksInventory=false, no se requiere stock inicial.
   * - Si tracksInventory=true, initialStock debe ser un número entero mayor o igual a cero.
   * - lowStockThreshold (si existe) debe ser un número entero mayor o igual a cero o null.
   */
  validateInventoryParameters(
    tracksInventory: boolean,
    initialStock?: number | null,
    lowStockThreshold?: number | null,
  ): { valid: boolean; error: string | null } {
    if (!tracksInventory) {
      return { valid: true, error: null };
    }

    if (
      initialStock === null ||
      initialStock === undefined ||
      !Number.isInteger(initialStock) ||
      initialStock < 0
    ) {
      return {
        valid: false,
        error:
          'Para variantes con seguimiento de inventario, el stock inicial debe ser un número entero mayor o igual a cero.',
      };
    }

    if (
      lowStockThreshold !== undefined &&
      lowStockThreshold !== null &&
      (!Number.isInteger(lowStockThreshold) || lowStockThreshold < 0)
    ) {
      return {
        valid: false,
        error:
          'El umbral de alerta de stock bajo debe ser un número entero mayor o igual a cero o nulo.',
      };
    }

    return { valid: true, error: null };
  }

  /**
   * Orquesta la creación de una variante y la configuración de su inventario:
   * 1. Valida reglas en memoria antes de cualquier petición HTTP.
   * 2. Si tracksInventory=false: crea únicamente la variante en catálogo.
   * 3. Si tracksInventory=true:
   *    - Paso 1: Crea variante en catálogo mediante POST existente.
   *    - Paso 2: Configura stock físico absoluto mediante PUT /inventory/variants/{id}/stock.
   *    - Paso 3: Si se indicó umbral, configura PATCH /inventory/variants/{id}/threshold.
   *    - Maneja fallos parciales: si el paso 2 o 3 falla, conserva la variante creada y no declara éxito total.
   */
  createVariantWithInventory(
    storeId: string,
    productId: string,
    request: CreateVariantWithInventoryRequest,
  ): Observable<VariantInventoryResult> {
    // Aislamiento de sesión: verificar validez de token
    if (!this.tokenService.hasValidToken()) {
      return throwError(() => new Error('Sesión no válida o expirada.'));
    }

    // 1. Validación preventiva en memoria
    const validation = this.validateInventoryParameters(
      request.tracksInventory,
      request.initialStock,
      request.lowStockThreshold,
    );
    if (!validation.valid) {
      return throwError(() => new Error(validation.error!));
    }

    const variantPayload: CreateProductVariantRequest = {
      presentationName: request.presentationName.trim(),
      sku: request.sku?.trim() || undefined,
      priceAmount: Number(request.priceAmount),
      priceCurrency: request.priceCurrency?.trim() || 'PEN',
      tracksInventory: request.tracksInventory,
      attributes: request.attributes || [],
    };

    // Caso 1: Sin seguimiento de inventario
    if (!request.tracksInventory) {
      return this.productService.createProductVariant(storeId, productId, variantPayload).pipe(
        map((createdVariant) => ({
          success: true,
          variant: createdVariant,
          inventory: null,
          stepFailed: null,
          errorMessage: null,
        })),
        catchError((err) =>
          of({
            success: false,
            variant: null,
            inventory: null,
            stepFailed: 'VARIANT' as const,
            error: err,
            errorMessage:
              err?.error?.message ||
              err?.message ||
              'Error al registrar la variante en el catálogo.',
          }),
        ),
      );
    }

    // Caso 2: Con seguimiento de inventario
    const initialStock = request.initialStock!;
    const lowStockThreshold =
      request.lowStockThreshold !== undefined ? request.lowStockThreshold : null;

    return this.productService.createProductVariant(storeId, productId, variantPayload).pipe(
      switchMap((createdVariant) => {
        // Variante creada exitosamente. Inicializar stock físico
        return this.inventoryService.updateStock(createdVariant.id, initialStock).pipe(
          switchMap((stockResponse) => {
            // Stock físico configurado exitosamente. Configurar umbral si fue especificado
            if (lowStockThreshold !== null) {
              return this.inventoryService
                .updateThreshold(createdVariant.id, lowStockThreshold)
                .pipe(
                  map((thresholdResponse) => ({
                    success: true,
                    variant: createdVariant,
                    inventory: thresholdResponse,
                    stepFailed: null,
                    errorMessage: null,
                  })),
                  catchError((thresholdErr) => {
                    const detail =
                      thresholdErr?.error?.message || thresholdErr?.message || 'Error desconocido';
                    // Falló el umbral, pero la variante y el stock físico ya están persistidos
                    return of({
                      success: false,
                      variant: createdVariant,
                      inventory: stockResponse,
                      stepFailed: 'THRESHOLD' as const,
                      error: thresholdErr,
                      errorMessage: `Stock inicial configurado, pero falló la configuración del umbral de alerta: ${detail}`,
                    });
                  }),
                );
            }

            // Sin umbral específico: finalizado exitoso
            return of({
              success: true,
              variant: createdVariant,
              inventory: stockResponse,
              stepFailed: null,
              errorMessage: null,
            });
          }),
          catchError((stockErr) => {
            const detail = stockErr?.error?.message || stockErr?.message || 'Error desconocido';
            // Falló la inicialización del stock físico. La variante ya existe en catálogo
            return of({
              success: false,
              variant: createdVariant,
              inventory: null,
              stepFailed: 'STOCK' as const,
              error: stockErr,
              errorMessage: `Variante creada en catálogo, pero falló la inicialización de stock físico: ${detail}`,
            });
          }),
        );
      }),
      catchError((variantErr) => {
        // Falló la creación de la variante en catálogo
        return of({
          success: false,
          variant: null,
          inventory: null,
          stepFailed: 'VARIANT' as const,
          error: variantErr,
          errorMessage:
            variantErr?.error?.message ||
            variantErr?.message ||
            'Error al registrar la variante en el catálogo.',
        });
      }),
    );
  }

  /**
   * Reintenta únicamente la inicialización de inventario para una variante que ya existe en el catálogo.
   * Evita duplicar la creación de variantes tras un fallo parcial en el paso de stock o umbral.
   */
  initializePendingInventory(
    variant: ProductVariant,
    initialStock: number,
    lowStockThreshold?: number | null,
  ): Observable<VariantInventoryResult> {
    if (!this.tokenService.hasValidToken()) {
      return throwError(() => new Error('Sesión no válida o expirada.'));
    }

    const validation = this.validateInventoryParameters(true, initialStock, lowStockThreshold);
    if (!validation.valid) {
      return throwError(() => new Error(validation.error!));
    }

    const threshold = lowStockThreshold !== undefined ? lowStockThreshold : null;

    return this.inventoryService.updateStock(variant.id, initialStock).pipe(
      switchMap((stockResponse) => {
        if (threshold !== null) {
          return this.inventoryService.updateThreshold(variant.id, threshold).pipe(
            map((thresholdResponse) => ({
              success: true,
              variant,
              inventory: thresholdResponse,
              stepFailed: null,
              errorMessage: null,
            })),
            catchError((thresholdErr) => {
              const detail =
                thresholdErr?.error?.message || thresholdErr?.message || 'Error desconocido';
              return of({
                success: false,
                variant,
                inventory: stockResponse,
                stepFailed: 'THRESHOLD' as const,
                error: thresholdErr,
                errorMessage: `Stock físico configurado, pero falló la configuración del umbral de alerta: ${detail}`,
              });
            }),
          );
        }

        return of({
          success: true,
          variant,
          inventory: stockResponse,
          stepFailed: null,
          errorMessage: null,
        });
      }),
      catchError((stockErr) => {
        const detail = stockErr?.error?.message || stockErr?.message || 'Error desconocido';
        return of({
          success: false,
          variant,
          inventory: null,
          stepFailed: 'STOCK' as const,
          error: stockErr,
          errorMessage: `Falló la inicialización del stock físico de la variante existente: ${detail}`,
        });
      }),
    );
  }
}
