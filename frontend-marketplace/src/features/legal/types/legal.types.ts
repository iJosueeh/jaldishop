export type LegalCalloutVariant = 'info' | 'warning' | 'shield' | 'clock';

export interface LegalTldr {
  title: string;
  summary: string;
  keyPoints: string[];
}

export type LegalContentItem =
  | { type: 'paragraph'; text: string }
  | { type: 'list'; items: string[] }
  | { type: 'callout'; variant: LegalCalloutVariant; title: string; text: string }
  | { type: 'table'; headers: string[]; rows: string[][] };

export interface LegalClause {
  id: string;
  title: string;
  shortTitle?: string;
  badge?: string;
  tldr: LegalTldr;
  content: LegalContentItem[];
}

export interface LegalDocument {
  slug: 'terminos' | 'privacidad';
  title: string;
  subtitle: string;
  badgeText: string;
  lastUpdated: string;
  effectiveDate: string;
  version: string;
  jurisdiction: string;
  clauses: LegalClause[];
}

/**
 * Tipos para el Libro de Reclamaciones Virtual (D.S. 011-2011-PCM)
 */
export type ConsumerDocumentType = 'DNI' | 'CE' | 'PASAPORTE' | 'RUC';
export type ClaimType = 'RECLAMO' | 'QUEJA';
export type ContractedGoodType = 'PRODUCTO' | 'SERVICIO';

export interface ClaimFormData {
  // Identificación del Consumidor
  fullName: string;
  documentType: ConsumerDocumentType;
  documentNumber: string;
  email: string;
  phone: string;
  address: string;
  department: string;
  province: string;
  district: string;
  isMinor: boolean;
  parentName?: string;

  // Identificación del Bien Contratado
  goodType: ContractedGoodType;
  claimedAmount: number;
  orderNumber?: string;
  storeName?: string;
  goodDescription: string;

  // Detalle de la Reclamación
  claimType: ClaimType;
  claimDetail: string;
  consumerRequest: string;

  // Aceptación legal
  acceptTerms: boolean;
}

export interface ClaimSubmissionResult {
  claimCode: string; // ej. JALDI-REC-2026-00124
  submittedAt: string;
  maxResponseDate: string; // 15 días hábiles según Ley 29571
  data: ClaimFormData;
}
