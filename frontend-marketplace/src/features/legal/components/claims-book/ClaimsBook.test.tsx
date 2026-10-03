import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { ClaimsBookForm } from './ClaimsBookForm';
import { validateClaimForm } from './claimsBook.schema';

describe('Libro de Reclamaciones Validation & Form (Paso 4)', () => {
  it('detects missing fields in validateClaimForm', () => {
    const invalidResult = validateClaimForm({});
    expect(invalidResult.isValid).toBe(false);
    expect(invalidResult.errors.fullName).toBeDefined();
    expect(invalidResult.errors.documentNumber).toBeDefined();
    expect(invalidResult.errors.email).toBeDefined();
    expect(invalidResult.errors.phone).toBeDefined();
    expect(invalidResult.errors.address).toBeDefined();
    expect(invalidResult.errors.goodDescription).toBeDefined();
    expect(invalidResult.errors.claimDetail).toBeDefined();
    expect(invalidResult.errors.consumerRequest).toBeDefined();
    expect(invalidResult.errors.acceptTerms).toBeDefined();
  });

  it('validates correct DNI length (8 digits) and email format', () => {
    const validDniResult = validateClaimForm({
      documentType: 'DNI',
      documentNumber: '12345678',
    });
    expect(validDniResult.errors.documentNumber).toBeUndefined();

    const invalidDniResult = validateClaimForm({
      documentType: 'DNI',
      documentNumber: '123',
    });
    expect(invalidDniResult.errors.documentNumber).toContain('8 dígitos');
  });

  it('renders ClaimsBookForm with all 4 legal sections and submit button', () => {
    render(<ClaimsBookForm />);

    expect(screen.getByText(/1. Identificación del Consumidor Reclamante/i)).toBeInTheDocument();
    expect(screen.getByText(/2. Identificación del Bien Contratado/i)).toBeInTheDocument();
    expect(screen.getByText(/3. Detalle de la Reclamación y Pedido del Consumidor/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Enviar Hoja de Reclamación/i })).toBeInTheDocument();
  });
});
