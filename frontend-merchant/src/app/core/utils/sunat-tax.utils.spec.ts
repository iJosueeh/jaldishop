import { describe, expect, it } from 'vitest';
import {
  getAllowedDocTypesForRegime,
  getDefaultTaxRateForRegime,
  hasValidModulo11CheckDigit,
  isValidDni,
  isValidSunatRuc,
} from './sunat-tax.utils';

describe('sunat-tax.utils', () => {
  it('valida DNI de 8 dígitos correctamente', () => {
    expect(isValidDni('12345678')).toBe(true);
    expect(isValidDni('1234567')).toBe(false);
    expect(isValidDni('1234567a')).toBe(false);
  });

  it('valida dígito verificador Módulo 11 de SUNAT para RUC 10 y RUC 20', () => {
    expect(hasValidModulo11CheckDigit('10456789019')).toBe(true);
    expect(hasValidModulo11CheckDigit('20100070970')).toBe(true);
    expect(hasValidModulo11CheckDigit('20131312955')).toBe(true);

    expect(hasValidModulo11CheckDigit('10456789012')).toBe(false);
    expect(hasValidModulo11CheckDigit('20100070979')).toBe(false);

    expect(isValidSunatRuc('10456789019', '10')).toBe(true);
    expect(isValidSunatRuc('20100070970', '20')).toBe(true);
    expect(isValidSunatRuc('20100070970', '10')).toBe(false);
  });

  it('filtra tipos de documento permitidos y tasa IGV por régimen', () => {
    expect(getAllowedDocTypesForRegime('RUS')).toEqual(['RUC_10', 'DNI']);
    expect(getAllowedDocTypesForRegime('MYPE')).toEqual(['RUC_10', 'RUC_20']);
    expect(getAllowedDocTypesForRegime('GENERAL')).toEqual(['RUC_10', 'RUC_20']);

    expect(getDefaultTaxRateForRegime('RUS')).toBe(0);
    expect(getDefaultTaxRateForRegime('MYPE')).toBe(18);
    expect(getDefaultTaxRateForRegime('MYPE', true)).toBe(0);
  });
});
