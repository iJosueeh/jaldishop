import { describe, it, expect } from 'vitest';
import { TERMS_DOCUMENT } from './termsContent';
import { PRIVACY_DOCUMENT } from './privacyContent';
import { LEGAL_META } from './legalMeta';

describe('Legal Documents & Constants Integrity (Paso 1)', () => {
  it('validates LEGAL_META metadata presence and compliance', () => {
    expect(LEGAL_META.platformName).toBe('JaldiShop');
    expect(LEGAL_META.country).toContain('Perú');
    expect(LEGAL_META.legalEmail).toBeDefined();
    expect(LEGAL_META.privacyEmail).toBeDefined();
    expect(LEGAL_META.claimsEmail).toBeDefined();
    expect(LEGAL_META.regulations.length).toBeGreaterThanOrEqual(3);

    // Verify key Peruvian regulations
    const codes = LEGAL_META.regulations.map((r) => r.code);
    expect(codes).toContain('Ley N° 29571');
    expect(codes).toContain('Ley N° 29733');
    expect(codes).toContain('D.S. 011-2011-PCM');
  });

  it('validates TERMS_DOCUMENT structure, clauses, and perishable exemption', () => {
    expect(TERMS_DOCUMENT.slug).toBe('terminos');
    expect(TERMS_DOCUMENT.clauses.length).toBeGreaterThanOrEqual(5);

    // Verify critical non-generic clauses
    const clauseIds = TERMS_DOCUMENT.clauses.map((c) => c.id);
    expect(clauseIds).toContain('naturaleza-del-servicio');
    expect(clauseIds).toContain('control-de-capacidad');
    expect(clauseIds).toContain('reserva-diez-minutos');
    expect(clauseIds).toContain('alimentos-perecibles-retracto');
    expect(clauseIds).toContain('seguridad-de-pagos');

    // Every clause must have a human-readable TLDR callout with key points
    TERMS_DOCUMENT.clauses.forEach((clause) => {
      expect(clause.id).toBeTruthy();
      expect(clause.title).toBeTruthy();
      expect(clause.tldr.title).toBeTruthy();
      expect(clause.tldr.summary).toBeTruthy();
      expect(clause.tldr.keyPoints.length).toBeGreaterThan(0);
      expect(clause.content.length).toBeGreaterThan(0);
    });
  });

  it('validates PRIVACY_DOCUMENT structure, ARCO rights, and data minimization', () => {
    expect(PRIVACY_DOCUMENT.slug).toBe('privacidad');
    expect(PRIVACY_DOCUMENT.clauses.length).toBeGreaterThanOrEqual(5);

    const clauseIds = PRIVACY_DOCUMENT.clauses.map((c) => c.id);
    expect(clauseIds).toContain('responsable-del-tratamiento');
    expect(clauseIds).toContain('datos-recopilados');
    expect(clauseIds).toContain('finalidad-del-tratamiento');
    expect(clauseIds).toContain('derechos-arco');
    expect(clauseIds).toContain('cookies-y-almacenamiento-local');

    // Verify ARCO clause contains tabular breakdown
    const arcoClause = PRIVACY_DOCUMENT.clauses.find((c) => c.id === 'derechos-arco');
    expect(arcoClause).toBeDefined();
    const hasTable = arcoClause?.content.some((item) => item.type === 'table');
    expect(hasTable).toBe(true);
  });
});
