'use client';

import { useCallback, useEffect, useState } from 'react';
import { customerAuthService } from '../services/customerAuthService';
import { mapAuthErrorMessage } from '../utils/authErrors';
import { validateEmailField } from '../utils/validation';

const DEFAULT_COOLDOWN_SECONDS = 60;

export function useResendCooldown(initialCooldown = DEFAULT_COOLDOWN_SECONDS) {
  const [cooldownSeconds, setCooldownSeconds] = useState(initialCooldown);
  const [isResending, setIsResending] = useState(false);
  const [resendMessage, setResendMessage] = useState<string | null>(null);
  const [resendError, setResendError] = useState<string | null>(null);

  useEffect(() => {
    if (cooldownSeconds <= 0) return;
    const timer = setTimeout(() => {
      setCooldownSeconds((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => clearTimeout(timer);
  }, [cooldownSeconds]);

  const resend = useCallback(
    async (email: string) => {
      if (isResending || cooldownSeconds > 0) return false;

      const validationError = validateEmailField(email);
      if (validationError) {
        setResendError(validationError);
        return false;
      }

      setIsResending(true);
      setResendError(null);
      setResendMessage(null);

      try {
        const res = await customerAuthService.resendVerification({
          email: email.trim().toLowerCase(),
        });
        setResendMessage(
          res.message ||
            'Si tu correo corresponde a una cuenta pendiente de verificación, recibirás un nuevo enlace en unos momentos.'
        );
        setCooldownSeconds(DEFAULT_COOLDOWN_SECONDS);
        return true;
      } catch (err: unknown) {
        setResendError(
          mapAuthErrorMessage(err, 'No se pudo reenviar el correo de verificación.')
        );
        return false;
      } finally {
        setIsResending(false);
      }
    },
    [cooldownSeconds, isResending]
  );

  return {
    cooldownSeconds,
    isResending,
    resendMessage,
    resendError,
    setResendError,
    resend,
  };
}
