# Plan de Refuerzo: Validaciones Robustas, Manejo de Errores Backend y Autocompletado Inteligente

> **Para trabajadores agenticos:** SUB-SKILL REQUERIDO: Usar `superpowers:subagent-driven-development` o `superpowers:executing-plans` para implementar este plan tarea por tarea. Cada paso usa la sintaxis de checklist (`- [ ]`) para seguimiento.

**Meta:** 
1. **Manejo de Errores y Validaciones del Backend**: Extraer y mapear la respuesta de error estándar de Spring Boot (`ApiError` / `errors` por campo) tanto en `PayoutDestinationModal` como en `SettingsPaymentsDeliveryTab`, mostrando mensajes amigables y accesibles debajo de cada input correspondiente en lugar de un mensaje genérico.
2. **Autocompletado Inteligente de Datos del Titular**: Aprovechar los datos de identidad fiscal de la tienda (`legalBusinessName`, `taxIdType`, `taxIdNumber` registrados en `StoreService.currentStore`) para pre-llenar automáticamente los campos de titular y documento en la configuración de la cuenta de abono con 1 clic ("Usar datos fiscales de mi tienda").
3. **Validaciones en Tiempo Real (Frontend + Backend Guardrails)**:
   - CCI: 20 dígitos numéricos con sanitización y feedback instantáneo.
   - Teléfono Yape/Plin: 9 dígitos comenzando obligatoriamente con `9`.
   - Reglas de Despacho: Monto mínimo no negativo, tiempo de preparación acotado estrictamente entre 5 y 480 minutos, y verificación de coherencia.

---

## 1. Arquitectura de Manejo de Errores

El backend (`GlobalExceptionHandler.java`) retorna el formato canónico:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Los datos enviados no son válidos",
  "errors": {
    "accountCci": "El CCI debe tener exactamente 20 dígitos numéricos",
    "accountHolderName": "El nombre del titular es obligatorio",
    "minDeliveryAmount": "El monto mínimo no puede ser negativo"
  }
}
```

---

## 2. Plan de Tareas Detallado

### Tarea 1: Utilidad de Errores Backend y Validaciones en Frontend
- [ ] Crear utilidad `extractApiErrorMessage` en `frontend-merchant/src/app/core/utils/error.utils.ts`.
- [ ] Implementar autocompletado fiscal ("Usar datos de Mi Tienda") en `PayoutDestinationModal`.
- [ ] Mapear errores campo por campo (CCI, Celular, Titular, Documento) en `PayoutDestinationModal`.
- [ ] Validar en tiempo real montos negativos y tiempos fuera de rango en `SettingsPaymentsDeliveryTab`.

### Tarea 2: Refuerzo Backend en Validaciones
- [ ] Revisar y asegurar anotaciones Jakarta en `StoreSettingsRequest.java` y `UpdatePayoutDestinationRequest.java`.
- [ ] Verificar pruebas unitarias de validación en `StoreSettingsControllerTest.java`.

### Tarea 3: Verificación, Tests y Detector Impeccable
- [ ] Tests de componente en `payout-destination-modal.spec.ts` para autocompletado y errores de campo.
- [ ] Tests en `settings-payments-delivery-tab.spec.ts` para validación de rangos.
- [ ] Ejecución de suite de tests en frontend y backend.
- [ ] Verificación con `impeccable detect` y documentación.
