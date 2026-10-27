import { Metadata } from 'next';
import { FavoritesView } from '@/features/customer/components/FavoritesView';

export const metadata: Metadata = {
  title: 'Mis Favoritos | Mi JaldiShop',
  description: 'Tus productos y comercios guardados en JaldiShop.',
};

export default function FavoritesPage() {
  return <FavoritesView />;
}
