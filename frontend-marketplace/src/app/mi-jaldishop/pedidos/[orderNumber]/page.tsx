import { Metadata } from 'next';
import { OrderDetailView } from '@/features/customer/components/OrderDetailView';

export const metadata: Metadata = {
  title: 'Detalle del Pedido | Mi JaldiShop',
  description: 'Seguimiento en vivo y detalle del pedido en JaldiShop.',
};

interface OrderDetailPageProps {
  params: Promise<{
    orderNumber: string;
  }>;
}

export default async function OrderDetailPage({ params }: OrderDetailPageProps) {
  const { orderNumber } = await params;

  return <OrderDetailView orderNumber={orderNumber} />;
}
