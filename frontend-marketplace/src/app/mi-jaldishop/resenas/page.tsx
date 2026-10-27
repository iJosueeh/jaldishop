import { Metadata } from 'next';
import { ReviewsView } from '@/features/customer/components/ReviewsView';

export const metadata: Metadata = {
  title: 'Mis Reseñas | Mi JaldiShop',
  description: 'Tus opiniones y calificaciones de productos en JaldiShop.',
};

export default function ReviewsPage() {
  return <ReviewsView />;
}
