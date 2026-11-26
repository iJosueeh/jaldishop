import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { TaxIdType, TaxRegime } from '../models/store.models';

const RUC_WEIGHTS = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];

export interface TaxRegimeOption {
  value: TaxRegime;
  label: string;
  shortLabel: string;
  receiptSummary: string;
  igvSummary: string;
  example: string;
}

export const TAX_REGIME_OPTIONS: TaxRegimeOption[] = [
  {
    value: 'RUS',
    label: 'Nuevo RUS (NRUS)',
    shortLabel: 'Nuevo RUS',
    receiptSummary: 'Solo Boleta de Venta o Ticket · Prohibido emitir Facturas',
    igvSummary: 'IGV 0% (Exonerado / Cuota única)',
    example: 'Ej: Bodegas, juguerías, puestos y pequeños negocios personales con RUC 10 o DNI en formalización.',
  },
  {
    value: 'RER',
    label: 'Régimen Especial de Renta (RER)',
    shortLabel: 'RER',
    receiptSummary: 'Emite Boleta de Venta y Factura Electrónica',
    igvSummary: 'Afecto a IGV 18% (salvo exoneración)',
    example: 'Ej: Comercios minoristas o restaurantes con ingresos hasta S/ 525,000/año (RUC 10 o RUC 20).',
  },
  {
    value: 'MYPE',
    label: 'Régimen MYPE Tributario (RMT)',
    shortLabel: 'MYPE Tributario',
    receiptSummary: 'Emite Boleta de Venta y Factura Electrónica',
    igvSummary: 'Afecto a IGV 18% (salvo exoneración)',
    example: 'Ej: Micro y pequeñas empresas (E.I.R.L., S.A.C. o Persona Natural con RUC 10/20) que venden a clientes y empresas.',
  },
  {
    value: 'GENERAL',
    label: 'Régimen General (RG)',
    shortLabel: 'Régimen General',
    receiptSummary: 'Emite Boleta de Venta, Factura y todo comprobante SUNAT',
    igvSummary: 'Afecto a IGV 18% (salvo exoneración)',
    example: 'Ej: Medianas y grandes empresas sin límite de ingresos (RUC 10 o RUC 20).',
  },
];

export function isValidDni(dni: string | null | undefined): boolean {
  return !!dni && /^\d{8}$/.test(dni.trim());
}

export function hasValidModulo11CheckDigit(ruc: string | null | undefined): boolean {
  if (!ruc || !/^\d{11}$/.test(ruc)) {
    return false;
  }
  let sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += Number(ruc[i]) * RUC_WEIGHTS[i];
  }
  const remainder = 11 - (sum % 11);
  const expected = remainder === 10 ? 0 : remainder === 11 ? 1 : remainder;
  return Number(ruc[10]) === expected;
}

export function isValidSunatRuc(ruc: string | null | undefined, expectedPrefix?: '10' | '20'): boolean {
  if (!ruc) return false;
  const clean = ruc.trim();
  if (!/^\d{11}$/.test(clean)) return false;
  if (expectedPrefix && !clean.startsWith(expectedPrefix)) return false;
  if (!expectedPrefix && !clean.startsWith('10') && !clean.startsWith('20')) return false;
  return hasValidModulo11CheckDigit(clean);
}

export function getAllowedDocTypesForRegime(regime: TaxRegime): TaxIdType[] {
  switch (regime) {
    case 'RUS':
      return ['RUC_10', 'DNI'];
    case 'NONE':
      return ['DNI'];
    case 'RER':
    case 'MYPE':
    case 'GENERAL':
      return ['RUC_10', 'RUC_20'];
  }
}

export function getDefaultTaxRateForRegime(regime: TaxRegime, igvExempt = false): number {
  if (regime === 'RUS' || regime === 'NONE' || igvExempt) {
    return 0;
  }
  return 18;
}

export function sunatDocumentValidator(getDocType: () => TaxIdType): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = (control.value ?? '').toString().trim();
    if (!raw) {
      return null;
    }
    if (!/^\d+$/.test(raw)) {
      return { nonNumeric: true };
    }

    const docType = getDocType();
    if (docType === 'DNI') {
      return raw.length === 8 ? null : { invalidDniLength: true };
    }

    const expectedPrefix = docType === 'RUC_10' ? '10' : '20';
    if (raw.length !== 11) {
      return { invalidRucLength: true };
    }
    if (!raw.startsWith(expectedPrefix)) {
      return { invalidRucPrefix: { expectedPrefix } };
    }
    if (!hasValidModulo11CheckDigit(raw)) {
      return { invalidModulo11: true };
    }
    return null;
  };
}
