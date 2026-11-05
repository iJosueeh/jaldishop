package com.jaldishop.backend.payment.application.gateway;

import com.jaldishop.backend.payment.application.gateway.dto.CreateOrderCommand;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.ExecuteRefundCommand;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.OrderResult;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundResult;

import java.util.List;

/**
 * Puerto de integración con la pasarela de Mercado Pago.
 * Permite a la capa de aplicación operar con Checkout Pro sin acoplarse al cliente HTTP ni al formato JSON del proveedor.
 */
public interface MercadoPagoGateway {

    /**
     * Crea una preferencia de pago en Checkout Pro para redireccionar al comprador.
     *
     * @param command datos estructurados de la orden y el comprador
     * @return resultado con preferenceId e initPoint
     */
    PreferenceResult createPreference(CreatePreferenceCommand command);

    /**
     * Consulta el estado financiero de un pago en Mercado Pago a partir de su ID de transacción.
     *
     * @param providerPaymentId identificador devuelto por Mercado Pago (número o cadena)
     * @return resultado estructurado del pago
     */
    PaymentQueryResult queryPayment(String providerPaymentId);

    /**
     * Ejecuta una solicitud de reembolso (total o parcial) contra Mercado Pago.
     *
     * @param command comando con el providerPaymentId, monto opcional y clave de idempotencia
     * @return resultado del reembolso emitido por el proveedor
     */
    RefundResult executeRefund(ExecuteRefundCommand command);

    /**
     * Consulta el listado de reembolsos procesados para un cobro específico en Mercado Pago.
     *
     * @param providerPaymentId identificador de cobro de Mercado Pago
     * @return lista de reembolsos existentes
     */
    List<RefundQueryResult> queryPaymentRefunds(String providerPaymentId);

    /**
     * Crea una orden en Checkout Pro mediante Orders API (/v1/orders) para redireccionar al comprador.
     *
     * @param command datos estructurados de la orden y el comprador
     * @return resultado con orderId y checkoutUrl
     */
    OrderResult createOrder(CreateOrderCommand command);

    /**
     * Consulta el estado financiero de una orden en Orders API (/v1/orders/{orderId}).
     *
     * @param orderId identificador devuelto por Mercado Pago al crear la orden
     * @return resultado estructurado de la orden y sus pagos
     */
    OrderQueryResult queryOrder(String orderId);

}
