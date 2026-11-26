import { TaxIdType, TaxRegime } from './store.models';

export interface RegisterStep1Data {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  passwordConfirm: string;
  terms: boolean;
}

export interface RegisterStep2Data {
  name: string;
  businessType: string;
  contactPhone: string;
  pickupEnabled?: boolean;
  deliveryEnabled?: boolean;
  address?: string;
  logoUrl?: string;
  bannerUrl?: string;
  categoryIds?: string[];
  taxRegime: TaxRegime;
  taxIdType: TaxIdType;
  taxIdNumber: string;
  legalBusinessName: string;
  canIssueInvoices: boolean;
  igvExempt: boolean;
  taxRate: number;
}

export interface RegisterStep3Data {
  pickupEnabled: boolean;
  deliveryEnabled: boolean;
  address?: string;
  addressReference?: string;
  latitude?: number | null;
  longitude?: number | null;
}

export interface RegisterStep4Data {
  dailyOrderLimit: number | null;
  prepTime: string;
  openingTime: string;
  closingTime: string;
  operatingDays: string[];
  autoPauseOnLimit: boolean;
  sunatTermsAccepted: boolean;
}

export interface DayOption {
  key: string;
  label: string;
}
