import { apiClient, ApiException } from '@/core/api/apiClient';
import { PublicStore } from '../types/storefront.types';

const DEMO_STORES: Record<string, PublicStore> = {
  'panaderia-don-pepe': {
    id: 'demo-store-001',
    name: 'Panadería & Pastelería Don Pepe',
    slug: 'panaderia-don-pepe',
    description:
      'Tradición artesanal desde 1998. Especialistas en masa madre, croissants de mantequilla francesa, empanadas recién horneadas y postres caseros.',
    phone: '+51987654321',
    address: 'Av. Primavera 450, Santiago de Surco, Lima',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
    deliveryFee: 5.0,
    minOrderAmount: 15.0,
    category: 'Panadería y Pastelería',
    rating: 4.9,
    reviewsCount: 128,
    preparationTimeMinutes: 25,
    openingHours: 'Lun - Sáb: 07:00 AM - 08:00 PM',
  },
  'dulce-amor': {
    id: 'demo-store-002',
    name: 'Dulce Amor Repostería Creativa',
    slug: 'dulce-amor',
    description:
      'Tortas artesanales personalizadas para cumpleaños y eventos especiales, cheesecakes clásicos, cupcakes gourmet y macarons variados.',
    phone: '+51987112233',
    address: 'Calle Los Cedros 123, Miraflores, Lima',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
    deliveryFee: 7.5,
    minOrderAmount: 25.0,
    category: 'Repostería Creativa',
    rating: 4.95,
    reviewsCount: 94,
    preparationTimeMinutes: 60,
    openingHours: 'Mar - Dom: 09:00 AM - 07:00 PM',
  },
  'tokyo-dark-kitchen': {
    id: 'demo-store-003',
    name: 'Tokyo Dark Kitchen',
    slug: 'tokyo-dark-kitchen',
    description:
      'Auténtico ramen japonés, sushi rolls tempura, gyoza al vapor y bowls teriyaki preparados en franjas horarias estrictas de alta precisión.',
    phone: '+51999887766',
    address: 'Av. Dos de Mayo 880, San Isidro, Lima',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: false,
    deliveryFee: 6.0,
    minOrderAmount: 30.0,
    category: 'Comida Japonesa & Nikkei',
    rating: 4.85,
    reviewsCount: 210,
    preparationTimeMinutes: 35,
    openingHours: 'Mar - Dom: 12:00 PM - 10:00 PM',
  },
};

export const storeService = {
  async getStoreBySlug(slug: string): Promise<PublicStore | null> {
    try {
      const store = await apiClient<PublicStore>(`/stores/slug/${slug}`, {
        timeoutMs: 4000,
      });
      return store;
    } catch (error: unknown) {
      if (error instanceof ApiException && error.status === 404) {
        if (DEMO_STORES[slug]) {
          return DEMO_STORES[slug];
        }
        return null;
      }
      if (DEMO_STORES[slug]) {
        return DEMO_STORES[slug];
      }
      return null;
    }
  },
};
