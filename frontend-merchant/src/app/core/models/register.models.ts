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
  pickupEnabled: boolean;
  deliveryEnabled: boolean;
  address?: string;
}

export interface RegisterStep3Data {
  dailyOrderLimit: number | null;
  prepTime: string;
  openingTime: string;
  closingTime: string;
  operatingDays: string[];
  autoPauseOnLimit: boolean;
}

export interface DayOption {
  key: string;
  label: string;
}
