import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function isValidEmail(email: string): boolean {
  return typeof email === 'string' && EMAIL_REGEX.test(email.trim());
}

/**
 * BFF Endpoint for Mercado Pago payment preference creation:
 * POST /api/customer/checkout/mercado-pago/preference -> Spring /api/v1/checkout/mercado-pago/preference
 *
 * Forwards authenticated customer session, validates payload contracts,
 * injects authenticated user profile details if missing, and prevents sensitive leakage.
 */
export async function POST(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json(
      {
        code: 'UNAUTHENTICATED',
        message: 'Inicia sesión como cliente para continuar con el pago.',
      },
      { status: 401 }
    );
  }

  const body = await request.json().catch(() => null);
  if (!body || typeof body !== 'object') {
    return NextResponse.json(
      {
        code: 'INVALID_REQUEST',
        message: 'El cuerpo de la solicitud no es un JSON válido.',
      },
      { status: 400 }
    );
  }

  const { capacityReservationId, fulfillmentType } = body;

  if (
    !capacityReservationId ||
    typeof capacityReservationId !== 'string' ||
    !UUID_REGEX.test(capacityReservationId.trim())
  ) {
    return NextResponse.json(
      {
        code: 'INVALID_RESERVATION_ID',
        message: 'El ID de la reserva de capacidad es obligatorio y debe ser un UUID válido.',
      },
      { status: 400 }
    );
  }

  if (fulfillmentType !== 'DELIVERY' && fulfillmentType !== 'PICKUP') {
    return NextResponse.json(
      {
        code: 'INVALID_FULFILLMENT_TYPE',
        message: 'El tipo de entrega debe ser DELIVERY o PICKUP.',
      },
      { status: 400 }
    );
  }

  if (fulfillmentType === 'DELIVERY') {
    if (typeof body.deliveryAddress !== 'string' || !body.deliveryAddress.trim()) {
      return NextResponse.json(
        {
          code: 'DELIVERY_ADDRESS_REQUIRED',
          message: 'La dirección de entrega es obligatoria para envíos a domicilio.',
        },
        { status: 400 }
      );
    }
  }

  let customerName = typeof body.customerName === 'string' ? body.customerName.trim() : '';
  let customerPhone = typeof body.customerPhone === 'string' ? body.customerPhone.trim() : '';
  let customerEmail = typeof body.customerEmail === 'string' ? body.customerEmail.trim() : '';

  // Fallback to authenticated profile if contact details were not provided in checkout form
  if (!customerName || !customerPhone || !isValidEmail(customerEmail)) {
    try {
      const userRes = await fetch(`${env.apiUrl}/users/me`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        cache: 'no-store',
        signal: AbortSignal.timeout(5000),
      });

      if (userRes.ok) {
        const user = await userRes.json();
        if (!customerName && user) {
          customerName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
        }
        if (!customerPhone && user?.phone) {
          customerPhone = user.phone.trim();
        }
        if (!isValidEmail(customerEmail) && user?.email) {
          customerEmail = user.email.trim();
        }
      }
    } catch {
      // Best-effort profile fallback; explicit validation follows
    }
  }

  if (!customerName) {
    return NextResponse.json(
      {
        code: 'MISSING_CUSTOMER_NAME',
        message: 'El nombre del cliente es obligatorio para generar el pago.',
      },
      { status: 400 }
    );
  }

  if (!customerPhone) {
    return NextResponse.json(
      {
        code: 'MISSING_CUSTOMER_PHONE',
        message: 'El teléfono del cliente es obligatorio para generar el pago.',
      },
      { status: 400 }
    );
  }

  if (!isValidEmail(customerEmail)) {
    return NextResponse.json(
      {
        code: 'INVALID_CUSTOMER_EMAIL',
        message: 'El correo electrónico del cliente es obligatorio y debe ser válido.',
      },
      { status: 400 }
    );
  }

  const upstreamPayload = {
    capacityReservationId: capacityReservationId.trim(),
    fulfillmentType,
    customerName,
    customerPhone,
    customerEmail,
    deliveryAddress: typeof body.deliveryAddress === 'string' ? body.deliveryAddress.trim() : null,
    deliveryReference: typeof body.deliveryReference === 'string' ? body.deliveryReference.trim() : null,
    deliveryLatitude: typeof body.deliveryLatitude === 'number' ? body.deliveryLatitude : null,
    deliveryLongitude: typeof body.deliveryLongitude === 'number' ? body.deliveryLongitude : null,
  };

  try {
    const upstreamUrl = `${env.apiUrl}/checkout/mercado-pago/preference`;
    const upstream = await fetch(upstreamUrl, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(upstreamPayload),
      cache: 'no-store',
      signal: AbortSignal.timeout(15000),
    });

    const data = await upstream.json().catch(() => ({}));

    if (upstream.status === 401) {
      const expiredResponse = NextResponse.json(
        {
          code: 'SESSION_EXPIRED',
          message: 'Tu sesión ha expirado. Por favor inicia sesión nuevamente.',
        },
        { status: 401 }
      );
      expiredResponse.cookies.delete(CUSTOMER_COOKIE);
      return expiredResponse;
    }

    if (upstream.status === 403) {
      return NextResponse.json(
        {
          code: 'FORBIDDEN',
          message: 'No tienes autorización para generar la preferencia de pago.',
        },
        { status: 403 }
      );
    }

    if (upstream.status === 429) {
      const rateLimitResponse = NextResponse.json(
        data && Object.keys(data).length > 0
          ? data
          : {
              code: 'RATE_LIMIT_EXCEEDED',
              message: 'Demasiadas solicitudes de pago. Intenta más tarde.',
            },
        { status: 429 }
      );
      const retryAfter = upstream.headers.get('Retry-After');
      if (retryAfter) {
        rateLimitResponse.headers.set('Retry-After', retryAfter);
      }
      return rateLimitResponse;
    }

    if (upstream.status === 400 || upstream.status === 409 || upstream.status === 422) {
      return NextResponse.json(data, { status: upstream.status });
    }

    if (upstream.status >= 500) {
      return NextResponse.json(
        {
          code: 'PAYMENT_GATEWAY_UNAVAILABLE',
          message: 'El servicio de pagos no está disponible en este momento. Intenta nuevamente.',
        },
        { status: 503 }
      );
    }

    return NextResponse.json(data, { status: upstream.status });
  } catch {
    return NextResponse.json(
      {
        code: 'PAYMENT_SERVICE_UNAVAILABLE',
        message: 'No se pudo conectar con el servicio de pagos. Intenta nuevamente.',
      },
      { status: 503 }
    );
  }
}
