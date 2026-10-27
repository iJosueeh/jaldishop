/**
 * Validation utilities conforming strictly to Spring Boot backend constraints.
 */

export const PERU_PHONE_REGEX = /^(\+51)?9\d{8}$/;

/**
 * Normalizes phone input by removing spaces, dashes, and parentheses.
 */
export function normalizePhone(value: string): string {
  return value.replace(/[\s\-()]/g, '');
}

/**
 * Validates a Peruvian phone number.
 * Phone is optional: empty or whitespace string is considered valid.
 * If provided, must match ^(\+51)?9\d{8}$ after stripping whitespace and punctuation.
 */
export function isValidPeruPhone(value: string | null | undefined): boolean {
  if (!value || value.trim() === '') {
    return true; // Optional in backend
  }
  const clean = normalizePhone(value.trim());
  return PERU_PHONE_REGEX.test(clean);
}

/**
 * Basic email format validator.
 */
export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

/**
 * Validates first name according to backend constraints (min 2, max 50).
 */
export function validateFirstName(name: string): string | null {
  const trimmed = name.trim();
  if (!trimmed) {
    return 'Ingresa tus nombres.';
  }
  if (trimmed.length < 2) {
    return 'El nombre debe tener al menos 2 caracteres.';
  }
  if (trimmed.length > 50) {
    return 'El nombre no puede tener más de 50 caracteres.';
  }
  return null;
}

/**
 * Validates last name according to backend constraints (min 2, max 50).
 */
export function validateLastName(lastName: string): string | null {
  const trimmed = lastName.trim();
  if (!trimmed) {
    return 'Ingresa tus apellidos.';
  }
  if (trimmed.length < 2) {
    return 'El apellido debe tener al menos 2 caracteres.';
  }
  if (trimmed.length > 50) {
    return 'El apellido no puede tener más de 50 caracteres.';
  }
  return null;
}

/**
 * Validates email according to backend constraints (valid format, max 100).
 */
export function validateEmailField(email: string): string | null {
  const trimmed = email.trim();
  if (!trimmed) {
    return 'Ingresa tu correo electrónico.';
  }
  if (!isValidEmail(trimmed)) {
    return 'Ingresa un correo electrónico con formato válido (ej. nombre@correo.com).';
  }
  if (trimmed.length > 100) {
    return 'El correo no puede tener más de 100 caracteres.';
  }
  return null;
}

/**
 * Validates password for customer registration (min 6, max 72).
 */
export function validateRegisterPassword(password: string): string | null {
  if (!password) {
    return 'Crea una contraseña.';
  }
  if (password.length < 6) {
    return 'La contraseña debe tener al menos 6 caracteres.';
  }
  if (password.length > 72) {
    return 'La contraseña no puede superar los 72 caracteres.';
  }
  return null;
}

/**
 * Validates password confirmation for customer registration.
 */
export function validateConfirmPassword(password: string, confirmPassword: string): string | null {
  if (!confirmPassword) {
    return 'Confirma tu contraseña.';
  }
  if (password !== confirmPassword) {
    return 'Las contraseñas no coinciden.';
  }
  return null;
}

/**
 * Validates optional Peruvian cellphone number.
 */
export function validatePhoneField(phone: string): string | null {
  if (!phone || !phone.trim()) {
    return null; // Optional
  }
  const clean = normalizePhone(phone.trim());
  if (!isValidPeruPhone(clean)) {
    return 'Ingresa un celular peruano válido (9 dígitos comenzando con 9).';
  }
  return null;
}
