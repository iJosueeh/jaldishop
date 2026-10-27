import { describe, expect, it } from 'vitest';
import { getSafeRedirectUrl } from './safeRedirect';

describe('getSafeRedirectUrl', () => {
  it('returns target URL when it is a valid internal relative path', () => {
    expect(getSafeRedirectUrl('/tienda/panaderia-san-pedro')).toBe('/tienda/panaderia-san-pedro');
    expect(getSafeRedirectUrl('/tienda/panaderia-san-pedro#entrega')).toBe('/tienda/panaderia-san-pedro#entrega');
    expect(getSafeRedirectUrl('/tienda/artesanias?categoria=ceramica')).toBe('/tienda/artesanias?categoria=ceramica');
  });

  it('falls back to default "/" for undefined, null, or empty string', () => {
    expect(getSafeRedirectUrl(undefined)).toBe('/');
    expect(getSafeRedirectUrl(null)).toBe('/');
    expect(getSafeRedirectUrl('')).toBe('/');
    expect(getSafeRedirectUrl('   ')).toBe('/');
  });

  it('blocks protocol-relative URLs (open redirect bypass)', () => {
    expect(getSafeRedirectUrl('//evil.com')).toBe('/');
    expect(getSafeRedirectUrl('//google.com/phishing')).toBe('/');
  });

  it('blocks Windows path bypasses', () => {
    expect(getSafeRedirectUrl('/\\evil.com')).toBe('/');
    expect(getSafeRedirectUrl('\\evil.com')).toBe('/');
  });

  it('blocks external absolute URLs and protocol schemes', () => {
    expect(getSafeRedirectUrl('https://evil.com')).toBe('/');
    expect(getSafeRedirectUrl('http://evil.com')).toBe('/');
    expect(getSafeRedirectUrl('javascript:alert(1)')).toBe('/');
    expect(getSafeRedirectUrl('/javascript:alert(1)')).toBe('/');
    expect(getSafeRedirectUrl('data:text/html,evil')).toBe('/');
  });

  it('respects a custom fallback if provided', () => {
    expect(getSafeRedirectUrl('https://evil.com', '/fallback')).toBe('/fallback');
    expect(getSafeRedirectUrl(null, '/tienda')).toBe('/tienda');
  });
});
