export type PromoScope = 'GLOBAL' | 'STORE';
export type PromoFundingSource = 'PLATFORM' | 'MERCHANT';
export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT';
export type PromoStatus = 'ACTIVE' | 'SCHEDULED' | 'EXPIRED' | 'EXHAUSTED' | 'INACTIVE';

export interface PromoCode {
  id: string;
  code: string;
  scope: PromoScope;
  fundingSource: PromoFundingSource;
  storeId: string | null;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number | null;
  maxDiscountAmount: number | null;
  usageLimit: number | null;
  usedCount: number;
  startsAt: string | null;
  endsAt: string | null;
  active: boolean;
  showInBanner: boolean;
  bannerText: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertPromoCodePayload {
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount?: number | null;
  maxDiscountAmount?: number | null;
  usageLimit?: number | null;
  startsAt?: string | null;
  endsAt?: string | null;
  active: boolean;
  showInBanner?: boolean;
  bannerText?: string | null;
}

export function resolvePromoStatus(promo: PromoCode, nowMs: number = Date.now()): PromoStatus {
  if (!promo.active) return 'INACTIVE';
  if (promo.usageLimit !== null && promo.usedCount >= promo.usageLimit) return 'EXHAUSTED';
  if (promo.startsAt && new Date(promo.startsAt).getTime() > nowMs) return 'SCHEDULED';
  if (promo.endsAt && new Date(promo.endsAt).getTime() < nowMs) return 'EXPIRED';
  return 'ACTIVE';
}
