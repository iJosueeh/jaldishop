import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { ProductDetailView } from '@/features/storefront/components/ProductDetailView';
import { storeService } from '@/features/storefront/services/storeService';

interface ProductDetailPageProps {
  params: Promise<{ slug: string; productSlug: string }>;
}

export async function generateMetadata({ params }: ProductDetailPageProps): Promise<Metadata> {
  const { slug, productSlug } = await params;
  const product = await storeService.getStoreProductBySlug(slug, productSlug).catch(() => null);
  return product
    ? { title: `${product.name} | JaldiShop`, description: product.description }
    : { title: 'Producto no encontrado | JaldiShop' };
}

export default async function ProductDetailPage({ params }: ProductDetailPageProps) {
  const { slug, productSlug } = await params;
  const [store, product] = await Promise.all([
    storeService.getStoreBySlug(slug),
    storeService.getStoreProductBySlug(slug, productSlug),
  ]);

  if (!store || !product) notFound();

  const [storeSettings, reviewsSummary] = await Promise.all([
    storeService.getPublicStoreSettings(store.id),
    storeService.getPublicProductReviews(product.id),
  ]);

  return (
    <ProductDetailView
      store={store}
      product={product}
      storeSettings={storeSettings}
      reviewsSummary={reviewsSummary}
    />
  );
}
