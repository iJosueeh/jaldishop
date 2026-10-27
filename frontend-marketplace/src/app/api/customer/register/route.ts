import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';
import { isValidEmail, normalizePhone, PERU_PHONE_REGEX } from '@/features/auth/utils/validation';

export async function POST(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  try {
    const body = await request.json();
    const { email, password, firstName, lastName, phone } = body;

    // Validación de tipos
    if (
      typeof email !== 'string' ||
      typeof password !== 'string' ||
      typeof firstName !== 'string' ||
      typeof lastName !== 'string'
    ) {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'Todos los campos obligatorios deben ser completados.',
          errors: { form: 'Verifica los campos obligatorios.' },
        },
        { status: 400 }
      );
    }

    const cleanEmail = email.trim().toLowerCase();
    const cleanFirstName = firstName.trim();
    const cleanLastName = lastName.trim();

    if (!cleanFirstName || cleanFirstName.length < 2) {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'El nombre debe tener al menos 2 caracteres.',
          errors: { firstName: 'El nombre debe tener al menos 2 caracteres.' },
        },
        { status: 400 }
      );
    }

    if (!cleanLastName || cleanLastName.length < 2) {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'El apellido debe tener al menos 2 caracteres.',
          errors: { lastName: 'El apellido debe tener al menos 2 caracteres.' },
        },
        { status: 400 }
      );
    }

    if (!cleanEmail || !isValidEmail(cleanEmail)) {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'El correo electrónico no tiene un formato válido.',
          errors: { email: 'Ingresa un correo electrónico válido.' },
        },
        { status: 400 }
      );
    }

    if (password.length < 6) {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'La contraseña debe tener al menos 6 caracteres.',
          errors: { password: 'La contraseña debe tener al menos 6 caracteres.' },
        },
        { status: 400 }
      );
    }

    let cleanPhone: string | null = null;
    if (typeof phone === 'string' && phone.trim().length > 0) {
      const normalized = normalizePhone(phone.trim());
      if (!PERU_PHONE_REGEX.test(normalized)) {
        return NextResponse.json(
          {
            code: 'VALIDATION_ERROR',
            message: 'El número de celular debe ser peruano (9 dígitos empezando con 9).',
            errors: { phone: 'El número de celular debe ser peruano (ej. 912345678).' },
          },
          { status: 400 }
        );
      }
      cleanPhone = normalized;
    }

    // Upstream call to Spring Boot register endpoint
    const registerUpstream = await fetch(`${env.apiUrl}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: cleanEmail,
        password,
        firstName: cleanFirstName,
        lastName: cleanLastName,
        phone: cleanPhone,
      }),
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    const registerData = await registerUpstream.json().catch(() => ({}));

    if (!registerUpstream.ok) {
      if (registerUpstream.status === 409 || registerData.code === 'EMAIL_ALREADY_EXISTS') {
        return NextResponse.json(
          {
            code: 'EMAIL_ALREADY_EXISTS',
            message: 'El correo electrónico ya se encuentra registrado.',
            errors: { email: 'Este correo ya está registrado.' },
          },
          { status: 409 }
        );
      }

      if (registerUpstream.status === 400) {
        return NextResponse.json(
          {
            code: 'VALIDATION_ERROR',
            message: registerData.message || 'Los datos de registro no son válidos.',
            errors: registerData.errors || {},
          },
          { status: 400 }
        );
      }

      return NextResponse.json(
        { message: registerData.message || 'No pudimos registrar tu cuenta. Inténtalo nuevamente.' },
        { status: registerUpstream.status }
      );
    }

    // Step 2: Auto-login after successful 201 registration
    try {
      const loginUpstream = await fetch(`${env.apiUrl}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: cleanEmail, password }),
        cache: 'no-store',
        signal: AbortSignal.timeout(10000),
      });

      if (loginUpstream.ok) {
        const loginData = await loginUpstream.json();
        if (
          Array.isArray(loginData.roles) &&
          loginData.roles.includes('CUSTOMER') &&
          typeof loginData.token === 'string'
        ) {
          const response = NextResponse.json(
            {
              authenticated: true,
              autoLogin: true,
              fullName: loginData.fullName || cleanFirstName,
              message: '¡Cuenta creada con éxito!',
            },
            { status: 201 }
          );

          response.cookies.set(CUSTOMER_COOKIE, loginData.token, {
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'lax',
            path: '/',
          });

          return response;
        }
      }
    } catch {
      // Auto-login failed (e.g. timeout), but user registration succeeded in database.
      // Do not repeat registration or report error; instruct user to log in.
    }

    // Fallback: registration succeeded but auto-login could not establish session
    return NextResponse.json(
      {
        authenticated: false,
        autoLogin: false,
        fullName: cleanFirstName,
        message: 'Tu cuenta fue creada con éxito. Por favor, inicia sesión con tus credenciales.',
      },
      { status: 201 }
    );
  } catch {
    return NextResponse.json(
      { message: 'No pudimos conectar con el servicio de registro. Vuelve a intentarlo.' },
      { status: 503 }
    );
  }
}
