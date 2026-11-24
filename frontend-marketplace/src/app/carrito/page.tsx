import { Metadata } from 'next';
import { CartPageClient } from '@/features/storefront/cart/CartPageClient';

export const metadata: Metadata = {
  title: 'Tu Carrito | JaldiShop',
  description: 'Revisa los productos de tu tienda activa y continúa al checkout.',
};

export default function CarritoPage() {
  return <CartPageClient />;
}
