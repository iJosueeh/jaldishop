import { describe, expect, it } from 'vitest';
import {
  isValidEmail,
  isValidPeruPhone,
  normalizePhone,
  validateFirstName,
  validateLastName,
  validateEmailField,
  validateRegisterPassword,
  validateConfirmPassword,
  validatePhoneField,
} from './validation';

describe('Validation utilities', () => {
  describe('normalizePhone', () => {
    it('removes spaces, dashes, and parentheses', () => {
      expect(normalizePhone('987 654 321')).toBe('987654321');
      expect(normalizePhone('+51 (987) 654-321')).toBe('+51987654321');
    });
  });

  describe('isValidPeruPhone', () => {
    it('allows empty or null strings as phone is optional', () => {
      expect(isValidPeruPhone('')).toBe(true);
      expect(isValidPeruPhone('   ')).toBe(true);
      expect(isValidPeruPhone(null)).toBe(true);
      expect(isValidPeruPhone(undefined)).toBe(true);
    });

    it('validates 9-digit Peruvian cellphone starting with 9', () => {
      expect(isValidPeruPhone('987654321')).toBe(true);
      expect(isValidPeruPhone('912345678')).toBe(true);
      expect(isValidPeruPhone(' 987 654 321 ')).toBe(true);
    });

    it('validates Peruvian cellphone with +51 international prefix', () => {
      expect(isValidPeruPhone('+51987654321')).toBe(true);
      expect(isValidPeruPhone('+51 912 345 678')).toBe(true);
    });

    it('rejects numbers that do not start with 9 or have invalid length', () => {
      expect(isValidPeruPhone('887654321')).toBe(false); // starts with 8
      expect(isValidPeruPhone('98765432')).toBe(false); // 8 digits
      expect(isValidPeruPhone('9876543210')).toBe(false); // 10 digits without +51
      expect(isValidPeruPhone('123456789')).toBe(false);
      expect(isValidPeruPhone('invalid-phone')).toBe(false);
    });
  });

  describe('isValidEmail', () => {
    it('validates standard email addresses', () => {
      expect(isValidEmail('usuario@dominio.com')).toBe(true);
      expect(isValidEmail('cliente.pe@jaldishop.net')).toBe(true);
    });

    it('rejects malformed email addresses', () => {
      expect(isValidEmail('no-email')).toBe(false);
      expect(isValidEmail('usuario@')).toBe(false);
      expect(isValidEmail('@dominio.com')).toBe(false);
      expect(isValidEmail('usuario@dominio')).toBe(false);
    });
  });

  describe('Field validators', () => {
    it('validates first and last names', () => {
      expect(validateFirstName('')).toBe('Ingresa tus nombres.');
      expect(validateFirstName('A')).toBe('El nombre debe tener al menos 2 caracteres.');
      expect(validateFirstName('Carlos')).toBeNull();

      expect(validateLastName('')).toBe('Ingresa tus apellidos.');
      expect(validateLastName('B')).toBe('El apellido debe tener al menos 2 caracteres.');
      expect(validateLastName('Torres')).toBeNull();
    });

    it('validates email field', () => {
      expect(validateEmailField('')).toBe('Ingresa tu correo electrónico.');
      expect(validateEmailField('invalid')).toBe(
        'Ingresa un correo electrónico con formato válido (ej. nombre@correo.com).'
      );
      expect(validateEmailField('correo@valido.pe')).toBeNull();
    });

    it('validates password and password confirmation', () => {
      expect(validateRegisterPassword('')).toBe('Crea una contraseña.');
      expect(validateRegisterPassword('123')).toBe(
        'La contraseña debe tener al menos 6 caracteres.'
      );
      expect(validateRegisterPassword('segura123')).toBeNull();

      expect(validateConfirmPassword('segura123', '')).toBe('Confirma tu contraseña.');
      expect(validateConfirmPassword('segura123', 'otra')).toBe('Las contraseñas no coinciden.');
      expect(validateConfirmPassword('segura123', 'segura123')).toBeNull();
    });

    it('validates optional phone field', () => {
      expect(validatePhoneField('')).toBeNull();
      expect(validatePhoneField('12345678')).toBe(
        'Ingresa un celular peruano válido (9 dígitos comenzando con 9).'
      );
      expect(validatePhoneField('987654321')).toBeNull();
      expect(validatePhoneField('+51 987 654 321')).toBeNull();
    });
  });
});
